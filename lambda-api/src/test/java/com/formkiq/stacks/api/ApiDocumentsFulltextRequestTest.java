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
package com.formkiq.stacks.api;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.api.CustomIndexApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.invoker.Configuration;
import com.formkiq.client.model.AddDocumentMetadata;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.DeleteFulltextResponse;
import com.formkiq.client.model.GetDocumentFulltextResponse;
import com.formkiq.client.model.SetDocumentFulltextRequest;
import com.formkiq.client.model.SetDocumentFulltextResponse;
import com.formkiq.client.model.UpdateDocumentFulltextRequest;
import com.formkiq.stacks.api.handler.FormKiQResponseCallback;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.FormKiqApiExtension;
import com.formkiq.testutils.aws.JwtTokenEncoder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TypesenseExtension;

/** Unit Tests for request /documents/{documentId}/fulltext. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(TypesenseExtension.class)
public class ApiDocumentsFulltextRequestTest {
  /** {@link FormKiQResponseCallback}. */
  private static final FormKiQResponseCallback CALLBACK = new FormKiQResponseCallback();
  /** FormKiQ Server. */
  @RegisterExtension
  static FormKiqApiExtension server = new FormKiqApiExtension(CALLBACK);
  /** {@link ApiClient}. */
  private ApiClient client =
      Configuration.getDefaultApiClient().setReadTimeout(0).setBasePath(server.getBasePath());
  /** {@link CustomIndexApi}. */
  private AdvancedDocumentSearchApi searchApi = new AdvancedDocumentSearchApi(this.client);

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   */
  private void setBearerToken(final String siteId) {
    String jwt = JwtTokenEncoder.encodeCognito(new String[] {siteId != null ? siteId : "default"},
        "joesmith");
    this.client.addDefaultHeader("Authorization", jwt);
  }

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
        this.searchApi.getDocumentFulltext(documentId, siteId, null);
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
  @SuppressWarnings("unchecked")
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
          this.searchApi.setDocumentFulltext(documentId, siteId, req);

      // then
      assertEquals("Add document to Typesense", putResponse.getMessage());

      GetDocumentFulltextResponse response =
          this.searchApi.getDocumentFulltext(documentId, siteId, null);
      assertEquals("text/plain", response.getContentType());
      assertEquals(content, response.getContent());
      Map<String, Object> metadata = (Map<String, Object>) response.getMetadata();
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
        this.searchApi.setDocumentFulltext(documentId, siteId, req);
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
        this.searchApi.setDocumentFulltext(documentId, siteId, req);
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
      this.searchApi.setDocumentFulltext(documentId, siteId, req);

      // when
      DeleteFulltextResponse response = this.searchApi.deleteDocumentFulltext(documentId, siteId);

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
          this.searchApi.setDocumentFulltext(documentId, siteId, req);

      UpdateDocumentFulltextRequest updateReq =
          new UpdateDocumentFulltextRequest().content(content2).contentType("text/plain");

      // when
      this.searchApi.updateDocumentFulltext(documentId, siteId, updateReq);

      // then
      GetDocumentFulltextResponse documentFulltext =
          this.searchApi.getDocumentFulltext(documentId, siteId, null);
      assertEquals(content2, documentFulltext.getContent());
      assertEquals("Add document to Typesense", putResponse.getMessage());
    }
  }
}
