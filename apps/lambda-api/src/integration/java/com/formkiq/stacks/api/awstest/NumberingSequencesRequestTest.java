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
package com.formkiq.stacks.api.awstest;

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.api.DocumentAttributesApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.GenerateDocumentAttributeValueResponse;
import com.formkiq.client.model.GetNumberingSequenceResponse;
import com.formkiq.client.model.NumberingSequence;
import com.formkiq.client.model.NumberingSequenceReset;
import com.formkiq.client.model.SetNumberingSequenceRequest;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Integration tests for site numbering sequences and attribute value generation. */
class NumberingSequencesRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit test timeout. */
  private static final int TEST_TIMEOUT = 60;

  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void shouldConfigureListAndGenerateNumberingSequence() throws Exception {
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String attributeKey = "agreementNumber" + System.currentTimeMillis();
    final String year = String.valueOf(Year.now(ZoneOffset.UTC).getValue());
    ApiClient client = getApiClients(siteId).getFirst();
    AttributesApi attributesApi = new AttributesApi(client);
    SystemManagementApi systemApi = new SystemManagementApi(client);
    final DocumentAttributesApi documentAttributesApi = new DocumentAttributesApi(client);

    attributesApi.addAttribute(new AddAttributeRequest().attribute(
        new AddAttribute().key(attributeKey).dataType(AttributeDataType.STRING)), siteId);

    SetNumberingSequenceRequest request =
        new SetNumberingSequenceRequest().pattern("CONTRACT-{YEAR}-{SEQUENCE}").startAt(75L)
            .padding(5).reset(NumberingSequenceReset.YEARLY).timezone("UTC");
    NumberingSequence saved =
        systemApi.setNumberingSequence(siteId, attributeKey, request).getNumberingSequence();
    assertEquals(attributeKey, saved.getAttributeKey());
    assertEquals(75L, saved.getStartAt());
    assertNull(saved.getCurrentSequence());

    GetNumberingSequenceResponse found = systemApi.getNumberingSequence(siteId, attributeKey);
    assertEquals("CONTRACT-{YEAR}-{SEQUENCE}", found.getNumberingSequence().getPattern());
    assertTrue(systemApi.getNumberingSequences(siteId, null, null).getNumberingSequences().stream()
        .anyMatch(sequence -> attributeKey.equals(sequence.getAttributeKey())));

    String firstDocumentId = addDocument(client, siteId, null, "first", "text/plain", null);
    String secondDocumentId = addDocument(client, siteId, null, "second", "text/plain", null);
    GenerateDocumentAttributeValueResponse first =
        documentAttributesApi.generateDocumentAttributeValue(firstDocumentId, attributeKey, siteId);
    GenerateDocumentAttributeValueResponse second = documentAttributesApi
        .generateDocumentAttributeValue(secondDocumentId, attributeKey, siteId);

    assertEquals("CONTRACT-" + year + "-00075", first.getAttribute().getStringValue());
    assertEquals(75L, first.getSequence());
    assertEquals("CONTRACT-" + year + "-00076", second.getAttribute().getStringValue());
    assertEquals(76L, second.getSequence());

    GenerateDocumentAttributeValueResponse repeated =
        documentAttributesApi.generateDocumentAttributeValue(firstDocumentId, attributeKey, siteId);
    assertEquals(first.getAttribute().getStringValue(), repeated.getAttribute().getStringValue());
    assertEquals(75L, repeated.getSequence());

    NumberingSequence current =
        systemApi.getNumberingSequence(siteId, attributeKey).getNumberingSequence();
    assertEquals(76L, current.getCurrentSequence());
    assertEquals("CONTRACT-" + year + "-00076", current.getLastValue());
  }
}
