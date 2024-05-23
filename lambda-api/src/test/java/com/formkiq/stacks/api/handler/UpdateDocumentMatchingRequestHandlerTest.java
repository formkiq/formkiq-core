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

import static com.formkiq.stacks.api.handler.UpdateDocumentMatchingRequestHandler.FORMKIQ_DOC_EXT;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.MatchDocumentTag;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequest;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequestMatch;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequestUpdate;
import com.formkiq.client.model.UpdateMatchingDocumentTagsResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * 
 * Test Handlers for: PATCH /documents/tags.
 *
 */
public class UpdateDocumentMatchingRequestHandlerTest extends AbstractApiClientRequestTest {

  /** {@link S3Service}. */
  private S3Service s3 = null;

  /**
   * BeforeEach.
   */
  @BeforeEach
  public void beforeEach() {
    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(S3Service.class, new S3ServiceExtension());

    this.s3 = awsServices.getExtension(S3Service.class);
    this.s3.deleteAllFiles(STAGE_BUCKET_NAME);
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
          .tag(new MatchDocumentTag().key("category").eq("person")));
      request.update(new UpdateMatchingDocumentTagsRequestUpdate()
          .addTagsItem(new AddDocumentTag().key("user").value("111")));

      // when
      UpdateMatchingDocumentTagsResponse response =
          this.tagsApi.updateMatchingDocumentTags(request, siteId);

      // then
      assertEquals("received update tags request", response.getMessage());

      ListObjectsResponse s3Response = this.s3.listObjects(STAGE_BUCKET_NAME, siteId);
      List<S3Object> contents = s3Response.contents();
      assertEquals(1, contents.size());
      assertTrue(contents.get(0).key().contains("patch_documents_tags_"));
      assertTrue(contents.get(0).key().endsWith(FORMKIQ_DOC_EXT));

      GetObjectTaggingResponse tags =
          this.s3.getObjectTags(STAGE_BUCKET_NAME, contents.get(0).key());
      assertEquals(1, tags.tagSet().size());
      assertEquals("userId", tags.tagSet().get(0).key());
      assertEquals("joesmith", tags.tagSet().get(0).value());
    }
  }
}
