/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.GenerateDocumentAttributeValueResponse;
import com.formkiq.client.model.NumberingSequence;
import com.formkiq.client.model.NumberingSequenceReset;
import com.formkiq.client.model.SetNumberingSequenceRequest;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Request tests for numbering sequence endpoints. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class NumberingSequencesRequestTest extends AbstractApiClientRequestTest {

  /**
   * PUT/GET numbering sequences and POST generated document attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testNumberingSequences01() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {

      // given
      String attributeKey = "agreementNumber";
      final String year = String.valueOf(Year.now(ZoneOffset.UTC).getValue());
      setBearerToken(siteId);
      new AddAttributeRequestBuilder().keyAsString(attributeKey).submit(this.client, siteId)
          .throwIfError();
      SetNumberingSequenceRequest request =
          new SetNumberingSequenceRequest().pattern("CONTRACT-{YEAR}-{SEQUENCE}").startAt(5L)
              .padding(5).reset(NumberingSequenceReset.YEARLY).timezone("UTC");

      // when
      NumberingSequence saved =
          this.systemApi.setNumberingSequence(siteId, attributeKey, request).getNumberingSequence();

      // then
      assertEquals(5L, saved.getStartAt());
      assertNull(saved.getCurrentSequence());
      assertEquals(attributeKey, this.systemApi.getNumberingSequence(siteId, attributeKey)
          .getNumberingSequence().getAttributeKey());
      assertTrue(this.systemApi.getNumberingSequences(siteId, null, null).getNumberingSequences()
          .stream().anyMatch(sequence -> attributeKey.equals(sequence.getAttributeKey())));

      // given
      String firstDocumentId = this.documentsApi
          .addDocument(new AddDocumentRequest().content("first"), siteId, null).getDocumentId();
      String secondDocumentId = this.documentsApi
          .addDocument(new AddDocumentRequest().content("second"), siteId, null).getDocumentId();

      // when
      GenerateDocumentAttributeValueResponse first = this.documentAttributesApi
          .generateDocumentAttributeValue(firstDocumentId, attributeKey, siteId);
      GenerateDocumentAttributeValueResponse second = this.documentAttributesApi
          .generateDocumentAttributeValue(secondDocumentId, attributeKey, siteId);

      // then
      assertEquals("CONTRACT-" + year + "-00005", first.getAttribute().getStringValue());
      assertEquals(5L, first.getSequence());
      assertEquals("CONTRACT-" + year + "-00006", second.getAttribute().getStringValue());
      assertEquals(6L, second.getSequence());

      // when
      GenerateDocumentAttributeValueResponse repeated = this.documentAttributesApi
          .generateDocumentAttributeValue(firstDocumentId, attributeKey, siteId);

      // then
      assertEquals(5L, repeated.getSequence());
      assertEquals(first.getAttribute().getStringValue(), repeated.getAttribute().getStringValue());
    }
  }
}
