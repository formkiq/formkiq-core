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
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentMetadata;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.DeleteFulltextResponse;
import com.formkiq.client.model.GetDocumentFulltextResponse;
import com.formkiq.client.model.SetDocumentFulltextRequest;
import com.formkiq.client.model.SetDocumentFulltextResponse;
import com.formkiq.client.model.UpdateDocumentFulltextRequest;

/** Unit Tests for request /documents/{documentId}/fulltext. */
public class ApiDocumentsFulltextRequestTest extends AbstractApiClientRequestTest {

  /**
   * GET /documents/{documentId}/fulltext request. Document NOT found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentFulltext01() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = UUID.randomUUID().toString();

      // when
      try {
        this.advancedSearchApi.getDocumentFulltext(documentId, siteId, null);
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * GET /documents/{documentId}/fulltext and PUT /documents/{documentId}/fulltext request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentFulltext02() throws Exception {
    String content = "some content";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      setBearerToken(siteId);

      SetDocumentFulltextRequest req =
          new SetDocumentFulltextRequest()
              .metadata(Arrays.asList(new AddDocumentMetadata().key("mykey").value("myvalue"),
                  new AddDocumentMetadata().key("mykey2")))
              .content(content).contentType("text/plain");

      // when
      SetDocumentFulltextResponse putResponse =
          this.advancedSearchApi.setDocumentFulltext(documentId, siteId, req);

      // then
      assertEquals("Add document to Typesense", putResponse.getMessage());

      GetDocumentFulltextResponse response =
          this.advancedSearchApi.getDocumentFulltext(documentId, siteId, null);
      assertEquals("text/plain", response.getContentType());
      assertEquals(content, response.getContent());
      Map<String, Object> metadata = response.getMetadata();
      assertEquals(2, metadata.size());
      assertEquals("myvalue", metadata.get("mykey"));
      assertEquals("", metadata.get("mykey2"));
    }
  }

  /**
   * PUT /documents/{documentId}/fulltext with tags request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutDocumentFulltext01() throws Exception {
    String content = "some content";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      setBearerToken(siteId);

      SetDocumentFulltextRequest req = new SetDocumentFulltextRequest().content(content)
          .contentType("text/plain").addTagsItem(new AddDocumentTag().key("category").value("123"));

      try {
        // when
        this.advancedSearchApi.setDocumentFulltext(documentId, siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"'tags' are not supported with Typesense\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/fulltext with 'contentUrls' request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutDocumentFulltext02() throws Exception {
    String content = "some content";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      setBearerToken(siteId);

      SetDocumentFulltextRequest req = new SetDocumentFulltextRequest().content(content)
          .contentType("text/plain").addContentUrlsItem("http://localhost");

      try {
        // when
        this.advancedSearchApi.setDocumentFulltext(documentId, siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"'contentUrls' are not supported by Typesense\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/fulltext.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteDocumentFulltext01() throws Exception {
    String content = "some content";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      setBearerToken(siteId);

      SetDocumentFulltextRequest req =
          new SetDocumentFulltextRequest().content(content).contentType("text/plain");
      this.advancedSearchApi.setDocumentFulltext(documentId, siteId, req);

      // when
      DeleteFulltextResponse response =
          this.advancedSearchApi.deleteDocumentFulltext(documentId, siteId);

      // then
      assertEquals("Deleted document '" + documentId + "'", response.getMessage());
    }
  }

  /**
   * PATCH /documents/{documentId}/fulltext request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchDocumentFulltext02() throws Exception {
    String content = "some content";
    String content2 = "new content";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      setBearerToken(siteId);

      SetDocumentFulltextRequest req =
          new SetDocumentFulltextRequest().content(content).contentType("text/plain");

      SetDocumentFulltextResponse putResponse =
          this.advancedSearchApi.setDocumentFulltext(documentId, siteId, req);

      UpdateDocumentFulltextRequest updateReq =
          new UpdateDocumentFulltextRequest().content(content2).contentType("text/plain");

      // when
      this.advancedSearchApi.updateDocumentFulltext(documentId, siteId, updateReq);

      // then
      GetDocumentFulltextResponse documentFulltext =
          this.advancedSearchApi.getDocumentFulltext(documentId, siteId, null);
      assertEquals(content2, documentFulltext.getContent());
      assertEquals("Add document to Typesense", putResponse.getMessage());
    }
  }
}
