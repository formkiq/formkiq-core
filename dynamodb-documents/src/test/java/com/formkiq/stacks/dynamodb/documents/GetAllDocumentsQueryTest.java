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
package com.formkiq.stacks.dynamodb.documents;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.base64.MapAttributeValueToString;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documents.GetAllDocumentsQuery;
import com.formkiq.aws.dynamodb.documents.GetDocumentDatesQuery;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.strings.Strings.isEmpty;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit Tests for {@link GetDocumentDatesQuery}.
 */
@ExtendWith(DynamoDbExtension.class)
public class GetAllDocumentsQueryTest {
  /** {@link DynamoDbService}. */
  private static DynamoDbService db;
  /** {@link DocumentService}. */
  private static DocumentService service;

  private static void assertQueryResults(final QueryResult results, final String siteId,
      final List<String> documentIds) {
    List<Map<String, AttributeValue>> items = results.items();
    assertEquals(documentIds.size(), items.size());

    for (int i = 0; i < documentIds.size(); i++) {
      String documentId = documentIds.get(i);
      Map<String, AttributeValue> item = items.get(i);
      assertEquals("PK, SK, documentId",
          item.keySet().stream().sorted().collect(Collectors.joining(", ")));
      assertEquals(createDatabaseKey(siteId, "docs#" + documentId),
          DynamoDbTypes.toString(item.get(PK)));
      assertEquals("document", DynamoDbTypes.toString(item.get(SK)));
      assertEquals(documentId, DynamoDbTypes.toString(item.get("documentId")));
    }
  }

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

  private @NotNull List<String> addDocuments(final String siteId,
      final List<String> insertedDates) {
    List<String> documentIds = new ArrayList<>();

    for (String date : insertedDates) {
      DocumentItem doc = createDocument(date);
      documentIds.add(doc.getDocumentId());
      service.saveDocument(siteId, doc, null);
    }
    return documentIds;
  }

  private DocumentItem createDocument(final String insertedDate) {
    Date date = !isEmpty(insertedDate) ? DateUtil.toDateFromString(insertedDate, ZoneOffset.UTC)
        : new Date();
    return new DocumentItemDynamoDb(ID.uuid(), date, "joe");
  }

  /**
   * Get All Document Dates.
   */
  @Test
  void testQueryAllDocument() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<String> documentIds = addDocuments(siteId, Collections.singletonList(null));

      // when
      QueryResult results = new GetAllDocumentsQuery().query(db, DOCUMENTS_TABLE, siteId, null, 10);

      // then
      assertQueryResults(results, siteId, documentIds);
    }
  }

  /**
   * Get Document across Dates simple with next token.
   */
  @Test
  void testQueryAllDocumentAcrossDates() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<String> documentIds = addDocuments(siteId,
          Arrays.asList("2025-11-29T16:30", "2025-11-29T16:00", "2025-11-29T15:30",
              "2025-10-01T12:30", "2025-10-01T10:30", "2025-01-01T08:30", "2025-01-01T06:30",
              "2025-01-01T04:30"));

      // when
      QueryResult result = new GetAllDocumentsQuery().query(db, DOCUMENTS_TABLE, siteId, null, 5);

      // then
      assertQueryResults(result, siteId, documentIds.subList(0, 5));

      // given
      String nextToken = new MapAttributeValueToString().apply(result.lastEvaluatedKey());

      // when
      result = new GetAllDocumentsQuery().query(db, DOCUMENTS_TABLE, siteId, nextToken, 5);

      // then
      assertQueryResults(result, siteId, documentIds.subList(5, documentIds.size()));
    }
  }

  /**
   * Get Document across Dates simple.
   */
  @Test
  void testQueryAllDocumentAcrossDatesSimple() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<String> documentIds = addDocuments(siteId, Arrays.asList(null, "2025-01-01"));

      // when
      QueryResult results = new GetAllDocumentsQuery().query(db, DOCUMENTS_TABLE, siteId, null, 10);

      // then
      assertQueryResults(results, siteId, documentIds);
    }
  }
}
