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
package com.formkiq.stacks.dynamodb;

import static com.formkiq.aws.dynamodb.model.DocumentSyncServiceType.TYPESENSE;
import static com.formkiq.stacks.dynamodb.DocumentSyncService.MESSAGE_ADDED_METADATA;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentSync;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/** Unit Tests for {@link DocumentSearchServiceImpl}. */
@ExtendWith(DynamoDbExtension.class)
public class DocumentSyncServiceDynamoDbTest {

  /** {@link DocumentSyncService}. */
  private static DocumentSyncService syncService;

  /**
   * BeforeAll.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    syncService = new DocumentSyncServiceDynamoDb(dynamoDbConnection, DOCUMENT_SYNCS_TABLE);
  }

  /**
   * Get Document Syncs.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGetSyncs01() throws Exception {
    // given
    String userId = "joe";

    String documentId = UUID.randomUUID().toString();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // when
      syncService.saveSync(siteId, documentId, TYPESENSE, DocumentSyncStatus.FAILED,
          DocumentSyncType.METADATA, userId, MESSAGE_ADDED_METADATA);
      TimeUnit.SECONDS.sleep(1);
      syncService.saveSync(siteId, documentId, TYPESENSE, DocumentSyncStatus.COMPLETE,
          DocumentSyncType.METADATA, userId, MESSAGE_ADDED_METADATA);

      // then
      PaginationResults<DocumentSync> results = syncService.getSyncs(siteId, documentId, null, 1);
      assertEquals(1, results.getResults().size());

      assertEquals(documentId, results.getResults().get(0).getDocumentId());
      assertEquals(TYPESENSE, results.getResults().get(0).getService());
      assertEquals(DocumentSyncStatus.COMPLETE, results.getResults().get(0).getStatus());
      assertNotNull(results.getResults().get(0).getSyncDate());

      results = syncService.getSyncs(siteId, documentId, results.getToken(), 1);
      assertEquals(1, results.getResults().size());

      assertEquals(documentId, results.getResults().get(0).getDocumentId());
      assertEquals(TYPESENSE, results.getResults().get(0).getService());
      assertEquals(DocumentSyncStatus.FAILED, results.getResults().get(0).getStatus());
      assertNotNull(results.getResults().get(0).getSyncDate());
    }
  }
}
