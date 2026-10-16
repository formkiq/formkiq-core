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
package com.formkiq.aws.dynamodb.documentattributes;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests automatic attribute pagination against stored DynamoDB records. */
@ExtendWith(DynamoDbExtension.class)
class QueryDocumentAttributesByKeyTest {

  /** Test site. */
  private static final String SITE = "site";
  /** Document artifact used by the query. */
  private static final DocumentArtifact DOCUMENT = DocumentArtifact.of("doc-123", "artifact-1");
  /** Attribute key with multiple stored values. */
  private static final String KEY = "Reviewer";
  /** Database used for fixture writes and queries. */
  private DynamoDbService db;

  @BeforeEach
  void beforeEach() throws URISyntaxException {
    this.db =
        new DynamoDbServiceImpl(DynamoDbTestServices.getDynamoDbConnection(), DOCUMENTS_TABLE);
  }

  private List<Map<String, AttributeValue>> records(final String siteId,
      final DocumentArtifact document, final String key, final int count) {
    return IntStream.range(0, count)
        .mapToObj(index -> new DocumentAttributeRecord().setDocument(document).setUserId("owner")
            .setKey(key).setStringValue(String.format(Locale.ROOT, "value-%03d", index))
            .updateValueType().getAttributes(siteId))
        .toList();
  }

  /**
   * Loads values beyond the page limit for both documents and artifacts in default and named sites.
   */
  @Test
  void testLoadsAllValuesFromDefaultTable() {
    // given
    final int count = 13;
    for (String siteId : Arrays.asList(null, SITE)) {
      for (DocumentArtifact document : List.of(DocumentArtifact.of(DOCUMENT.documentId(), null),
          DOCUMENT)) {
        List<Map<String, AttributeValue>> stored = records(siteId, document, KEY, count);
        this.db.putItems(stored);
        QueryDocumentAttributesByKey query = new QueryDocumentAttributesByKey(document, KEY);
        // when
        QueryResult result = query.query(this.db, siteId, null, 3);
        // then
        assertEquals(stored.stream().map(item -> item.get("stringValue").s()).toList(),
            result.items().stream().map(item -> item.get("stringValue").s()).toList());
        assertFalse(result.hasLastEvaluatedKey());
        assertNull(result.toNextToken());
      }
    }
  }

  /**
   * Uses the explicitly supplied table on every page rather than the database service's default
   * table.
   */
  @Test
  void testLoadsAllValuesFromExplicitTable() {
    // given
    this.db.putItems(records(SITE, DOCUMENT, KEY, 1));
    List<Map<String, AttributeValue>> stored = records(SITE, DOCUMENT, KEY, 7);
    this.db.putItems(CACHE_TABLE, stored);
    QueryDocumentAttributesByKey query = new QueryDocumentAttributesByKey(DOCUMENT, KEY);
    // when
    QueryResult result = query.query(this.db, CACHE_TABLE, SITE, null, 2);
    // then
    assertEquals(stored.stream().map(item -> item.get("stringValue").s()).toList(),
        result.items().stream().map(item -> item.get("stringValue").s()).toList());
    assertFalse(result.hasLastEvaluatedKey());
    assertNull(result.toNextToken());
  }

  /**
   * Completes pagination when the number of values is an exact multiple of the page size.
   */
  @Test
  void testLoadsExactMultipleOfPageSize() {
    // given
    List<Map<String, AttributeValue>> stored = records(SITE, DOCUMENT, KEY, 6);
    this.db.putItems(stored);
    QueryDocumentAttributesByKey query = new QueryDocumentAttributesByKey(DOCUMENT, KEY);
    // when
    QueryResult result = query.query(this.db, SITE, null, 2);
    // then
    assertEquals(stored.stream().map(item -> item.get("stringValue").s()).toList(),
        result.items().stream().map(item -> item.get("stringValue").s()).toList());
    assertFalse(result.hasLastEvaluatedKey());
    assertNull(result.toNextToken());
  }

  /**
   * Starts at the supplied continuation token and loads every remaining page without repeating
   * prior values.
   */
  @Test
  void testLoadsRemainingValuesFromContinuationToken() {
    // given
    final List<Map<String, AttributeValue>> stored = records(SITE, DOCUMENT, KEY, 7);
    this.db.putItems(stored);
    QueryDocumentAttributesByKey query = new QueryDocumentAttributesByKey(DOCUMENT, KEY);
    QueryResponse firstPage = this.db.query(query.build(DOCUMENTS_TABLE, SITE, null, 2));
    final String nextToken =
        new QueryResult(firstPage.items(), firstPage.lastEvaluatedKey()).toNextToken();
    // when
    QueryResult result = query.query(this.db, SITE, nextToken, 2);
    // then
    assertEquals(2, firstPage.items().size());
    assertNotNull(nextToken);
    assertEquals(
        stored.subList(2, stored.size()).stream().map(item -> item.get("stringValue").s()).toList(),
        result.items().stream().map(item -> item.get("stringValue").s()).toList());
    assertFalse(result.hasLastEvaluatedKey());
    assertNull(result.toNextToken());
  }

  /**
   * Returns an empty result without a continuation token when no records match the attribute key.
   */
  @Test
  void testMissingAttributeReturnsEmptyResult() {
    // given
    this.db.putItems(records(SITE, DOCUMENT, "Other", 3));
    QueryDocumentAttributesByKey query = new QueryDocumentAttributesByKey(DOCUMENT, KEY);
    // when
    QueryResult result = query.query(this.db, SITE, null, 2);
    // then
    assertEquals(List.of(), result.items());
    assertFalse(result.hasLastEvaluatedKey());
    assertNull(result.toNextToken());
  }
}
