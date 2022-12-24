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
package com.formkiq.module.lambda.typesense;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TypeSenseExtension.API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentSync;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.formkiq.testutils.aws.TypeSenseExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;

/**
 * 
 * Unit Tests {@link TypesenseProcessor}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(TypeSenseExtension.class)
class TypesenseProcessorTest {

  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().create();
  /** Max results. */
  private static final int MAX = 10;
  /** {@link TypesenseProcessor}. */
  private static TypesenseProcessor processor;
  /** {@link TypeSenseService}. */
  private static TypeSenseService service;
  /** {@link DocumentSyncService}. */
  private static DocumentSyncService syncService;

  @BeforeAll
  public static void beforeAll() throws Exception {
    AwsBasicCredentials cred = AwsBasicCredentials.create("asd", "asd");
    DynamoDbConnectionBuilder db = DynamoDbTestServices.getDynamoDbConnection(null);

    processor = new TypesenseProcessor(Map.of("AWS_REGION", "us-east-1", "DOCUMENT_SYNC_TABLE",
        DOCUMENT_SYNCS_TABLE, "TYPESENSE_HOST",
        "http://localhost:" + TypeSenseExtension.getMappedPort(), "TYPESENSE_API_KEY", API_KEY), db,
        cred);

    service = new TypeSenseServiceImpl("http://localhost:" + TypeSenseExtension.getMappedPort(),
        API_KEY, Region.US_EAST_1, cred);

    syncService = new DocumentSyncServiceDynamoDb(db, DOCUMENT_SYNCS_TABLE);
  }

  /** {@link Context}. */
  private Context context = new LambdaContextRecorder();

  /**
   * Load Request File.
   * 
   * @param name {@link String}
   * @return {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> loadRequest(final String name) throws IOException {

    try (InputStream is = getClass().getResourceAsStream(name)) {
      String s = IoUtils.toUtf8String(is);
      return GSON.fromJson(s, Map.class);
    }
  }

  /**
   * Insert 2 records.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest01() throws Exception {
    // given
    String siteId = null;

    for (int i = 0; i < 2; i++) {

      Map<String, Object> map = loadRequest("/insert.json");

      // when
      processor.handleRequest(map, this.context);

      // then
      String documentId = "acd4be1b-9466-4dcd-b8b8-e5b19135b460";

      //
      List<String> documents = service.searchFulltext(siteId, "karate", MAX);
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0));

      documents = service.searchFulltext(siteId, "test.pdf", MAX);
      assertEquals(1, documents.size());

      documents = service.searchFulltext(siteId, "bleh.pdf", MAX);
      assertEquals(0, documents.size());

      PaginationResults<DocumentSync> syncs = syncService.getSyncs(siteId, documentId, null, MAX);
      assertEquals(1, syncs.getResults().size());

      assertEquals(documentId, syncs.getResults().get(0).getDocumentId());
      assertEquals(DocumentSyncServiceType.TYPESENSE, syncs.getResults().get(0).getService());
      assertEquals(DocumentSyncStatus.COMPLETE, syncs.getResults().get(0).getStatus());
      assertEquals(DocumentSyncType.METADATA, syncs.getResults().get(0).getType());
      assertNotNull(syncs.getResults().get(0).getSyncdDate());
    }
  }

  /**
   * Modify records.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest02() throws Exception {
    // given
    String siteId = null;
    for (int i = 0; i < 2; i++) {
      String documentId = "717a3cee-888d-47e0-83a3-a7487a588954";
      Map<String, Object> map = loadRequest("/modify.json");

      // when
      processor.handleRequest(map, this.context);

      // then
      List<String> documents = service.searchFulltext(siteId, "some.pdf", MAX);
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0));

      if (i > 0) {
        PaginationResults<DocumentSync> syncs = syncService.getSyncs(siteId, documentId, null, MAX);
        assertEquals(1, syncs.getResults().size());

        assertEquals(documentId, syncs.getResults().get(0).getDocumentId());
        assertEquals(DocumentSyncServiceType.TYPESENSE, syncs.getResults().get(0).getService());
        assertEquals(DocumentSyncStatus.COMPLETE, syncs.getResults().get(0).getStatus());
        assertEquals(DocumentSyncType.METADATA, syncs.getResults().get(0).getType());
        assertEquals("arn:aws:iam::111111111:user/mike", syncs.getResults().get(0).getUserId());
        assertNotNull(syncs.getResults().get(0).getSyncdDate());
      }
    }
  }

  /**
   * Test Delete records.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest03() throws Exception {
    // given
    String siteId = null;
    for (int i = 0; i < 2; i++) {
      String documentId = "717a3cee-888d-47e0-83a3-a7487a588954";
      Map<String, Object> map = loadRequest("/modify.json");

      // when
      processor.handleRequest(map, this.context);

      // then
      List<String> documents = service.searchFulltext(siteId, "some.pdf", MAX);
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0));

      // given
      map = loadRequest("/remove.json");

      // when
      processor.handleRequest(map, this.context);

      // then
      documents = service.searchFulltext(siteId, "some.pdf", MAX);
      assertEquals(0, documents.size());

      PaginationResults<DocumentSync> syncs = syncService.getSyncs(siteId, documentId, null, MAX);
      assertEquals(0, syncs.getResults().size());
    }
  }
}
