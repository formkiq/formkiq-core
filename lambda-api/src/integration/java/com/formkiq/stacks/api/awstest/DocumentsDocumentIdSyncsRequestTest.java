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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Test;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.DocumentSync;
import com.formkiq.stacks.client.models.DocumentSyncs;
import com.formkiq.stacks.client.models.Version;
import com.formkiq.stacks.client.requests.GetDocumentSyncsRequest;

/**
 * GET, OPTIONS /documents/{documentId}/syncs tests.
 *
 */
public class DocumentsDocumentIdSyncsRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30000;

  private List<DocumentSync> find(final Collection<DocumentSync> list,
      final DocumentSyncServiceType type) {
    return list.stream().filter(s -> s.service().equals(type.name())).collect(Collectors.toList());
  }

  private Optional<DocumentSync> find(final Collection<DocumentSync> list,
      final DocumentSyncType type) {
    return list.stream().filter(s -> s.type().equals(type.name())).findFirst();
  }

  private boolean isComplete(final String formkiqType, final DocumentSyncs syncs) {
    final int four = 4;
    int count = syncs.syncs().size();
    return ("enterprise".equals(formkiqType) && count == four)
        || (!"enterprise".equals(formkiqType) && count == 2);
  }

  /**
   * Get Document Sync.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGetSyncs01() throws Exception {

    String formkiqType = null;

    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      for (FormKiqClientV1 client : getFormKiqClients(siteId)) {

        Version version = client.getVersion();
        formkiqType = version.type();

        String path = UUID.randomUUID().toString();
        String documentId = addDocumentWithoutFile(client, siteId, path);

        GetDocumentSyncsRequest req =
            new GetDocumentSyncsRequest().siteId(siteId).documentId(documentId);

        // when
        DocumentSyncs syncs = client.getDocumentSyncs(req);

        while (!isComplete(formkiqType, syncs)) {
          TimeUnit.SECONDS.sleep(1);
          syncs = client.getDocumentSyncs(req);
        }

        // then
        List<DocumentSync> list = syncs.syncs();
        assertFalse(list.isEmpty());

        for (DocumentSync sync : list) {
          assertEquals(documentId, sync.documentId());
          assertNotNull(sync.userId());
          assertNotNull(sync.syncDate());
          assertEquals(DocumentSyncStatus.COMPLETE.name(), sync.status());
        }

        List<DocumentSync> typesense = find(list, DocumentSyncServiceType.TYPESENSE);
        assertEquals(2, typesense.size());

        DocumentSync sync = find(typesense, DocumentSyncType.CONTENT).get();
        assertEquals(DocumentSyncServiceType.TYPESENSE.name(), sync.service());
        assertEquals(DocumentSyncType.CONTENT.name(), sync.type());

        sync = find(typesense, DocumentSyncType.METADATA).get();
        assertEquals(DocumentSyncServiceType.TYPESENSE.name(), sync.service());
        assertEquals(DocumentSyncType.METADATA.name(), sync.type());

        List<DocumentSync> opensearch = find(list, DocumentSyncServiceType.OPENSEARCH);
        if (!opensearch.isEmpty()) {
          assertEquals(2, opensearch.size());

          sync = find(opensearch, DocumentSyncType.TAG).get();
          assertEquals(DocumentSyncServiceType.OPENSEARCH.name(), sync.service());
          assertEquals(DocumentSyncType.TAG.name(), sync.type());

          sync = find(opensearch, DocumentSyncType.METADATA).get();
          assertEquals(DocumentSyncServiceType.OPENSEARCH.name(), sync.service());
          assertEquals(DocumentSyncType.METADATA.name(), sync.type());
        }
      }
    }
  }
}
