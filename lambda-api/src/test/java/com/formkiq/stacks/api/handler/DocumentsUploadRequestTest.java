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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentTagsResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /documents/upload. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsUploadRequestTest extends AbstractApiClientRequestTest {

  /**
   * Get Request Upload Document Url, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGet01() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken("Admins");

      UpdateConfigurationRequest config = new UpdateConfigurationRequest().maxDocuments("1");
      this.systemApi.updateConfiguration(siteId, config);

      setBearerToken(siteId);
      this.documentsApi.getDocumentUpload(null, siteId, Integer.valueOf(1), null, null);

      // when
      try {
        this.documentsApi.getDocumentUpload(null, siteId, Integer.valueOf(1), null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Max Number of Documents reached\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGet02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.getDocumentUpload(null, siteId, Integer.valueOf(1), null, null);

      // then
      String documentId = response.getDocumentId();
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());
    }
  }

  /**
   * POST Request Upload Document Url, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testPost01() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken("Admins");

      UpdateConfigurationRequest config = new UpdateConfigurationRequest().maxDocuments("1");
      this.systemApi.updateConfiguration(siteId, config);

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest();
      this.documentsApi.addDocumentUpload(req, siteId, Integer.valueOf(1), null, null);

      // when
      try {
        this.documentsApi.addDocumentUpload(req, siteId, Integer.valueOf(1), null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Max Number of Documents reached\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testPost02() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest();
      this.documentsApi.addDocumentUpload(req, siteId, Integer.valueOf(1), null, null);

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, Integer.valueOf(1), null, null);

      // then
      String documentId = response.getDocumentId();
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());
    }
  }

  /**
   * POST Request Upload Document Url with tags.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testPost03() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest()
          .addTagsItem(new AddDocumentTag().key("category").value("person"));
      this.documentsApi.addDocumentUpload(req, siteId, Integer.valueOf(1), null, null);

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, Integer.valueOf(1), null, null);

      // then
      String documentId = response.getDocumentId();
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());

      GetDocumentTagsResponse tags =
          this.tagsApi.getDocumentTags(documentId, siteId, null, null, null, null);
      assertEquals(1, tags.getTags().size());
      assertEquals("category", tags.getTags().get(0).getKey());
      assertEquals("person", tags.getTags().get(0).getValue());
      assertEquals("userdefined", tags.getTags().get(0).getType());
    }
  }
}
