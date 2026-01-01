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
package com.formkiq.stacks.dynamodb.folders;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.folders.GetAllFolderAndFilesQuery;
import com.formkiq.aws.dynamodb.folders.GetFolderFilesByNameQuery;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test {@link GetFolderFilesByNameQuery}.
 */
@ExtendWith(DynamoDbExtension.class)
public class GetAllFolderAndFilesQueryTest {

  /** {@link DynamoDbService}. */
  private static DynamoDbService db;
  /** {@link DocumentService}. */
  private static DocumentService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {
    DynamoDbConnectionBuilder dbc = DynamoDbTestServices.getDynamoDbConnection();
    db = new DynamoDbServiceImpl(dbc, DOCUMENTS_TABLE);
    service = new DocumentServiceImpl(dbc, DOCUMENTS_TABLE, DOCUMENT_SYNCS_TABLE,
        new DocumentVersionServiceNoVersioning());
  }

  private static String getPath(final Map<String, AttributeValue> item) {
    return DynamoDbTypes.toString(item.get("path"));
  }

  private static String getType(final Map<String, AttributeValue> item) {
    return DynamoDbTypes.toString(item.get("type"));
  }

  private void assertPath(final Map<String, AttributeValue> item, final String type,
      final String path) {
    assertEquals(path, getPath(item));
    assertEquals(type, getType(item));
  }

  private DocumentItem createDoc(final String path) {
    DocumentItem item = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
    item.setPath(path);
    return item;
  }

  /**
   * Test getting folder / file.
   */
  @Test
  void testGetFolderFilesByName() {
    // given
    final int limit = 3;
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      GetAllFolderAndFilesQuery q = new GetAllFolderAndFilesQuery();

      service.saveDocument(siteId, createDoc("test1.txt"), null);
      service.saveDocument(siteId, createDoc("test2.txt"), null);
      service.saveDocument(siteId, createDoc("a/test3.txt"), null);
      service.saveDocument(siteId, createDoc("b/test4.txt"), null);

      // when
      QueryResult result = q.query(db, DOCUMENTS_TABLE, siteId, null, limit);

      // then
      assertEquals(3, result.items().size());
      assertPath(result.items().get(0), "folder", "a");
      assertPath(result.items().get(1), "folder", "b");
      assertPath(result.items().get(2), "file", "test1.txt");

      // when
      result = q.query(db, DOCUMENTS_TABLE, siteId, result.toNextToken(), limit);

      // then
      assertEquals(1, result.items().size());
      assertPath(result.items().get(0), "file", "test2.txt");
      assertTrue(result.hasLastEvaluatedKey());

      // when
      result = q.query(db, DOCUMENTS_TABLE, siteId, result.toNextToken(), limit);

      // then
      assertEquals(1, result.items().size());
      assertPath(result.items().get(0), "file", "test3.txt");
      assertTrue(result.hasLastEvaluatedKey());

      // when
      result = q.query(db, DOCUMENTS_TABLE, siteId, result.toNextToken(), limit);

      // then
      assertEquals(1, result.items().size());
      assertPath(result.items().get(0), "file", "test4.txt");
      assertFalse(result.hasLastEvaluatedKey());
    }
  }
}
