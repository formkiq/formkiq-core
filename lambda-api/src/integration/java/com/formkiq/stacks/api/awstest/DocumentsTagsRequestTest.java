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

import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentTag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.GetDocumentTagResponse;
import com.formkiq.client.model.MatchDocumentTag;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequest;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequestMatch;
import com.formkiq.client.model.UpdateMatchingDocumentTagsRequestUpdate;
import com.formkiq.client.model.UpdateMatchingDocumentTagsResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * Process Urls.
 * <p>
 * PATCH /documents/tags integration tests
 * </p>
 *
 */
public class DocumentsTagsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;

  /**
   * Test Patch multiple document tags.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testPatchDocumentsTags01() throws Exception {
    // given
    final String tagKey = "category";
    final String tagValue = "person";

    int count = 0;
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      List<ApiClient> clients = getApiClients(siteId);

      DocumentsApi documentsApi = new DocumentsApi(clients.get(0));
      AddDocumentRequest addReq = new AddDocumentRequest().content("somecontent")
          .tags(Arrays.asList(new AddDocumentTag().key(tagKey).value(tagValue)));
      String documentId = documentsApi.addDocument(addReq, siteId, null).getDocumentId();

      for (ApiClient apiClient : clients) {

        waitForDocumentContent(apiClient, siteId, documentId);

        DocumentTagsApi tagsApi = new DocumentTagsApi(apiClient);

        UpdateMatchingDocumentTagsRequestMatch matchReq =
            new UpdateMatchingDocumentTagsRequestMatch()
                .tag(new MatchDocumentTag().key(tagKey).eq(tagValue));

        final String newTagKey = "caseNo_" + count;
        final String newTagValue = "123_" + count;

        UpdateMatchingDocumentTagsRequestUpdate updateReq =
            new UpdateMatchingDocumentTagsRequestUpdate()
                .addTagsItem(new AddDocumentTag().key(newTagKey).value(newTagValue));

        UpdateMatchingDocumentTagsRequest req =
            new UpdateMatchingDocumentTagsRequest().match(matchReq).update(updateReq);

        // when
        UpdateMatchingDocumentTagsResponse response =
            tagsApi.updateMatchingDocumentTags(req, siteId);

        // then
        assertEquals("received update tags request", response.getMessage());

        GetDocumentTagResponse tagResponse =
            waitForDocumentTag(apiClient, siteId, documentId, newTagKey);
        assertEquals(newTagValue, tagResponse.getValue());

        count++;
      }
    }
  }
}
