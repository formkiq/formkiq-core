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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.DocumentSync;
import com.formkiq.stacks.client.models.DocumentSyncs;
import com.formkiq.stacks.client.requests.GetDocumentSyncsRequest;

/**
 * GET, OPTIONS /documents/{documentId}/syncs tests.
 *
 */
public class DocumentsDocumentIdSyncsRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30000;

  /**
   * Get Document Sync.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGetSyncs01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        String path = UUID.randomUUID().toString();
        String documentId = addDocumentWithoutFile(client, siteId, path);

        GetDocumentSyncsRequest req =
            new GetDocumentSyncsRequest().siteId(siteId).documentId(documentId);

        // when
        DocumentSyncs syncs = client.getDocumentSyncs(req);

        while (syncs.syncs().size() < 2) {
          TimeUnit.SECONDS.sleep(1);
          syncs = client.getDocumentSyncs(req);
        }

        // then
        List<DocumentSync> list = syncs.syncs();
        assertEquals(2, list.size());

        assertEquals(documentId, list.get(0).documentId());
        assertEquals(DocumentSyncServiceType.TYPESENSE.name(), list.get(0).service());
        assertEquals(DocumentSyncStatus.COMPLETE.name(), list.get(0).status());
        assertEquals(DocumentSyncType.CONTENT.name(), list.get(0).type());
        assertNotNull(list.get(0).userId());
        assertNotNull(list.get(0).syncDate());

        assertEquals(documentId, list.get(1).documentId());
        assertEquals(DocumentSyncServiceType.TYPESENSE.name(), list.get(1).service());
        assertEquals(DocumentSyncStatus.COMPLETE.name(), list.get(1).status());
        assertEquals(DocumentSyncType.METADATA.name(), list.get(1).type());
        assertNotNull(list.get(1).userId());
        assertNotNull(list.get(1).syncDate());
      }
    }
  }
}