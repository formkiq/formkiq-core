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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.model.DocumentSyncService;
import com.formkiq.client.model.DocumentSyncStatus;
import com.formkiq.client.model.DocumentSyncType;
import com.formkiq.client.model.GetDocumentSyncResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.DocumentSync;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * GET, OPTIONS /documents/{documentId}/syncs tests.
 *
 */
public class DocumentsDocumentIdSyncsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;

  private List<DocumentSync> find(final List<DocumentSync> list, final DocumentSyncService type) {
    return list.stream().filter(s -> type.equals(s.getService())).collect(Collectors.toList());
  }

  private DocumentSync find(final Collection<DocumentSync> list, final DocumentSyncType type) {
    return list.stream().filter(s -> type.equals(s.getType())).findFirst().orElse(null);
  }

  private boolean isComplete(final GetDocumentSyncResponse syncs) {
    int count = notNull(syncs.getSyncs()).size();
    return count == 4;
  }

  /**
   * Get Document Sync.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetSyncs01() throws Exception {

    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      for (ApiClient client : getApiClients(siteId)) {

        DocumentsApi api = new DocumentsApi(client);

        String path = UUID.randomUUID() + ".txt";
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        String documentId = addDocument(client, siteId, path, content, "text/plain", null);

        // when
        GetDocumentSyncResponse syncs = api.getDocumentSyncs(documentId, siteId, null, null);

        while (!isComplete(syncs)) {
          TimeUnit.SECONDS.sleep(1);
          syncs = api.getDocumentSyncs(documentId, siteId, null, null);
        }

        // then
        List<DocumentSync> list = notNull(syncs.getSyncs());
        assertFalse(list.isEmpty());

        for (DocumentSync sync : list) {
          assertNotNull(sync.getUserId());

          if (!DocumentSyncService.EVENTBRIDGE.equals(sync.getService())) {
            assertNotNull(sync.getSyncDate());
            assertEquals(DocumentSyncStatus.COMPLETE, sync.getStatus());
          }
        }

        List<DocumentSync> typesense = find(list, DocumentSyncService.TYPESENSE);
        if (!typesense.isEmpty()) {
          assertEquals(2, typesense.size());

          DocumentSync sync = find(typesense, DocumentSyncType.CONTENT);
          assertEquals(DocumentSyncService.TYPESENSE, sync.getService());
          assertEquals(DocumentSyncType.CONTENT, sync.getType());

          sync = find(typesense, DocumentSyncType.METADATA);
          assertEquals(DocumentSyncService.TYPESENSE, sync.getService());
          assertEquals(DocumentSyncType.METADATA, sync.getType());
        }

        List<DocumentSync> opensearch = find(list, DocumentSyncService.OPENSEARCH);
        if (!opensearch.isEmpty()) {
          assertEquals(2, opensearch.size());

          DocumentSync sync = find(opensearch, DocumentSyncType.CONTENT);
          assertEquals(DocumentSyncService.OPENSEARCH, sync.getService());
          assertEquals(DocumentSyncType.CONTENT, sync.getType());

          sync = find(opensearch, DocumentSyncType.METADATA);
          assertEquals(DocumentSyncService.OPENSEARCH, sync.getService());
          assertEquals(DocumentSyncType.METADATA, sync.getType());
        }
      }
    }
  }
}
