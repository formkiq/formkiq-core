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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeResponse;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.GetAttributeResponse;
import com.formkiq.client.model.GetAttributesResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /attributes. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class AttributesRequestTest extends AbstractApiClientRequestTest {

  /**
   * POST /attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddAttributes01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));

      // when
      AddAttributeResponse response = this.attributesApi.addAttribute(req, siteId);

      // then
      assertEquals("Attribute 'security' created", response.getMessage());

      GetAttributesResponse attributes = this.attributesApi.getAttributes(siteId, null, null);
      assertEquals(1, attributes.getAttributes().size());
      Attribute attribute = attributes.getAttributes().get(0);
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());

      GetAttributeResponse attr = this.attributesApi.getAttribute(key, siteId);
      attribute = attr.getAttribute();
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
    }
  }

  /**
   * POST /documents/upload.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(new AddDocumentAttribute().key(key).stringValue("confidential"));

      // when
      String documentId =
          this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();

      // then
      DocumentSearchRequest s0 = new DocumentSearchRequest()
          .query(new DocumentSearch().attribute(new DocumentSearchAttribute().key(key)));

      DocumentSearchResponse response = this.searchApi.documentSearch(s0, siteId, null, null, null);

      assertEquals(1, response.getDocuments().size());
      assertEquals(documentId, response.getDocuments().get(0).getDocumentId());

      // todo match matching attribute
    }
  }
}
