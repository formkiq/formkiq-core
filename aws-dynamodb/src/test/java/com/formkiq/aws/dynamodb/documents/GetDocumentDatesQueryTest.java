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
package com.formkiq.aws.dynamodb.documents;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.base64.MapAttributeValueToString;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit Tests for {@link GetDocumentDatesQuery}.
 */
@ExtendWith(DynamoDbExtension.class)
public class GetDocumentDatesQueryTest {
  /** {@link DynamoDbService}. */
  private static DynamoDbService db;
  /** {@link DocumentTimestampDbOperation}. */
  private static final DocumentTimestampDbOperation DB_TIMESTAMP =
      new DocumentTimestampDbOperation();
  /** {@link DynamoDbClient}. */
  private static DynamoDbClient dbClient;
  /** {@link DateTimeFormatter}. */
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {
    DynamoDbConnectionBuilder dbc = DynamoDbTestServices.getDynamoDbConnection();
    dbClient = dbc.build();
    db = new DynamoDbServiceImpl(dbc, DOCUMENTS_TABLE);
  }

  /**
   * Get Document Dates after multiple timestamp writes.
   */
  @Test
  void testQueryDocumentDates() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (int i = 0; i < 10; i++) {
        new DocumentTimestampDbOperation().execute(dbClient, DOCUMENTS_TABLE, null);
      }

      // when
      QueryResult results =
          new GetDocumentDatesQuery().query(db, DOCUMENTS_TABLE, siteId, null, 10);

      // then
      assertEquals(1, results.items().size());
      assertEquals("docdate", DynamoDbTypes.toString(results.items().get(0).get(PK)));
      assertEquals(DATE_FMT.format(new Date().toInstant()),
          DynamoDbTypes.toString(results.items().get(0).get(SK)));
    }
  }

  /**
   * Get Document Dates less than date.
   */
  @Test
  void testQueryDocumentLteDates() throws ValidationException {
    // given
    List<String> dates = List.of("2025-01-05", "2025-01-12", "2025-01-19", "2025-01-26",
        "2025-02-02", "2025-02-09", "2025-02-16", "2025-02-23");

    dates.forEach(date -> DB_TIMESTAMP.execute(dbClient, DOCUMENTS_TABLE, null,
        DateUtil.toDateFromString(date, ZoneOffset.UTC)));

    Date date = DateUtil.toDateFromString("2025-02-02", ZoneOffset.UTC);

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      QueryResult result =
          new GetDocumentDatesQuery(date).query(db, DOCUMENTS_TABLE, siteId, null, 10);

      // then
      assertEquals(5, result.items().size());
      String got = result.items().stream().map(a -> a.get(SK).s()).collect(Collectors.joining(","));
      String expected = "2025-02-02,2025-01-26,2025-01-19,2025-01-12,2025-01-05";
      assertEquals(expected, got);
    }
  }

  /**
   * Get Document Dates multiple days.
   */
  @Test
  void testQueryDocumentMultipleDates() throws ValidationException {
    // given
    List<String> dates = List.of("2025-01-05", "2025-01-12", "2025-01-19", "2025-01-26",
        "2025-02-02", "2025-02-09", "2025-02-16", "2025-02-23", "2025-03-02", "2025-03-09",
        "2025-03-16", "2025-03-23", "2025-03-30", "2025-04-06", "2025-04-13", "2025-04-20",
        "2025-04-27", "2025-05-04", "2025-05-11", "2025-05-18");

    dates.forEach(date -> DB_TIMESTAMP.execute(dbClient, DOCUMENTS_TABLE, null,
        DateUtil.toDateFromString(date, ZoneOffset.UTC)));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      QueryResult result = new GetDocumentDatesQuery().query(db, DOCUMENTS_TABLE, siteId, null, 10);

      // then
      assertEquals(10, result.items().size());
      String got = result.items().stream().map(a -> a.get(SK).s()).collect(Collectors.joining(","));
      String expected =
          "2025-05-18,2025-05-11,2025-05-04,2025-04-27,2025-04-20,2025-04-13,2025-04-06,"
              + "2025-03-30,2025-03-23,2025-03-16";
      assertEquals(expected, got);

      // given
      String nextToken = new MapAttributeValueToString().apply(result.lastEvaluatedKey());

      // when
      result = new GetDocumentDatesQuery().query(db, DOCUMENTS_TABLE, siteId, nextToken, 10);

      // then
      assertEquals(10, result.items().size());
      got = result.items().stream().map(a -> a.get(SK).s()).collect(Collectors.joining(","));
      expected =
          "2025-03-09,2025-03-02,2025-02-23,2025-02-16,2025-02-09,2025-02-02,2025-01-26,2025-01-19,"
              + "2025-01-12,2025-01-05";
      assertEquals(expected, got);
    }
  }
}
