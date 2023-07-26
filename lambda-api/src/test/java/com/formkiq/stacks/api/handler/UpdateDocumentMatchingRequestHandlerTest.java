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
import static com.formkiq.stacks.api.handler.UpdateDocumentMatchingRequestHandler.FORMKIQ_DOC_EXT;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.invoker.Configuration;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.MatchDocumentTag;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequest;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequestMatch;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequestUpdate;
import com.formkiq.client.model.UpdateMatchingDocumentTagsResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.FormKiqApiExtension;
import com.formkiq.testutils.aws.JwtTokenEncoder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;

/**
 * 
 * Test Handlers for: PATCH /documents/tags.
 *
 */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class UpdateDocumentMatchingRequestHandlerTest {

  /** {@link S3Service}. */
  private static S3Service s3;
  /** FormKiQ Server. */
  @RegisterExtension
  static FormKiqApiExtension server =
      new FormKiqApiExtension().setCallback(new FormKiQResponseCallback());

  /**
   * BeforeAll.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    s3 = new S3Service(TestServices.getS3Connection(null));
  }

  /** {@link ApiClient}. */
  private ApiClient client =
      Configuration.getDefaultApiClient().setReadTimeout(0).setBasePath(server.getBasePath());
  /** {@link DocumentTagsApi}. */
  private DocumentTagsApi tagsApi = new DocumentTagsApi(this.client);

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   */
  private void setBearerToken(final String siteId) {
    String jwt = JwtTokenEncoder
        .encodeCognito(new String[] {siteId != null ? siteId : DEFAULT_SITE_ID}, "joesmith");
    this.client.setBearerToken(jwt);
  }

  /**
   * Test with missing data.
   * 
   * @throws Exception Exception
   */
  @Test
  void testUpdateMatchingDocumentTags01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      UpdateMatchingDocumentTagsRequest request = new UpdateMatchingDocumentTagsRequest();

      // when
      try {
        this.tagsApi.updateMatchingDocumentTags(request, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"match\",\"error\":\"'match' required\"},"
            + "{\"key\":\"update\",\"error\":\"'update' required\"}]}", e.getResponseBody());
      }

      // given
      request.setMatch(new UpdateMatchingDocumentTagsRequestMatch());
      request.setUpdate(new UpdateMatchingDocumentTagsRequestUpdate());

      // when
      try {
        this.tagsApi.updateMatchingDocumentTags(request, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"match.tag\",\"error\":\"'match.tag' required\"},"
            + "{\"key\":\"update\",\"error\":\"'update' required\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Test valid.
   * 
   * @throws Exception Exception
   */
  @Test
  void testUpdateMatchingDocumentTags02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      UpdateMatchingDocumentTagsRequest request = new UpdateMatchingDocumentTagsRequest();
      request.match(new UpdateMatchingDocumentTagsRequestMatch()
          .tag(new MatchDocumentTag().key("category").value("person")));
      request.update(new UpdateMatchingDocumentTagsRequestUpdate()
          .addTagsItem(new AddDocumentTag().key("user").value("111")));

      // when
      UpdateMatchingDocumentTagsResponse response =
          this.tagsApi.updateMatchingDocumentTags(request, siteId);

      // then
      assertEquals("received update tags request", response.getMessage());

      ListObjectsResponse s3Response = s3.listObjects(STAGE_BUCKET_NAME, siteId);
      assertEquals(1, s3Response.contents().size());
      assertTrue(s3Response.contents().get(0).key().contains("patch_documents_tags"));
      assertTrue(s3Response.contents().get(0).key().endsWith(FORMKIQ_DOC_EXT));
    }
  }
}
