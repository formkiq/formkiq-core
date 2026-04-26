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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit Tests for {@link GetSoftDeletedDocumentsQuery}.
 */
public class GetSoftDeletedDocumentsQueryTest {

  @Test
  void testBuildAscWithRange() {
    String siteId = ID.uuid();
    Date start = DateUtil.toDateFromString("2025-04-02T00:00:00Z", ZoneOffset.UTC);
    Date end = DateUtil.toDateFromString("2025-04-03T00:00:00Z", ZoneOffset.UTC);

    QueryRequest request =
        new GetSoftDeletedDocumentsQuery(start, end, true).build("Documents", siteId, null, 10);

    assertEquals(Boolean.TRUE, request.scanIndexForward());
    assertEquals("#PK = :PK AND #SK BETWEEN :SK_low AND :SK_high",
        request.keyConditionExpression());
    assertEquals("date#2025-04-01T23:59:59.999Z",
        request.expressionAttributeValues().get(":SK_low").s());
    assertEquals("date#2025-04-03T00:00:00.000999999Z",
        request.expressionAttributeValues().get(":SK_high").s());
    assertEquals("GSI2SK", request.expressionAttributeNames().get("#SK"));
  }

  @Test
  void testBuildDefaultSortDesc() {
    String siteId = ID.uuid();

    QueryRequest request = new GetSoftDeletedDocumentsQuery().build("Documents", siteId, null, 10);

    assertEquals("GSI2", request.indexName());
    assertEquals(Boolean.FALSE, request.scanIndexForward());
    assertEquals("#PK = :PK", request.keyConditionExpression());
    assertEquals(siteId + "/softdelete#docs#", request.expressionAttributeValues().get(":PK").s());
    assertEquals("GSI2PK", request.expressionAttributeNames().get("#PK"));
  }

  @Test
  void testBuildEndOnly() {
    String siteId = ID.uuid();
    Date end = DateUtil.toDateFromString("2025-04-02T00:00:00Z", ZoneOffset.UTC);

    QueryRequest request =
        new GetSoftDeletedDocumentsQuery(null, end, false).build("Documents", siteId, null, 10);

    assertEquals("date#", request.expressionAttributeValues().get(":SK_low").s());
    assertEquals("date#2025-04-02T00:00:00.000999999Z",
        request.expressionAttributeValues().get(":SK_high").s());
  }

  @Test
  void testBuildPrimaryKey() {
    String siteId = ID.uuid();

    QueryRequest request =
        new GetSoftDeletedDocumentsQuery().buildPrimaryKey("Documents", siteId, null, 10);

    assertNull(request.indexName());
    assertEquals(Boolean.FALSE, request.scanIndexForward());
    assertEquals("#PK = :PK", request.keyConditionExpression());
    assertEquals(siteId + "/softdelete#docs#", request.expressionAttributeValues().get(":PK").s());
    assertEquals("PK", request.expressionAttributeNames().get("#PK"));
  }

  @Test
  void testBuildStartOnly() {
    String siteId = ID.uuid();
    Date start = DateUtil.toDateFromString("2025-04-02T00:00:00Z", ZoneOffset.UTC);

    QueryRequest request =
        new GetSoftDeletedDocumentsQuery(start, null, false).build("Documents", siteId, null, 10);

    assertEquals("date#2025-04-01T23:59:59.999Z",
        request.expressionAttributeValues().get(":SK_low").s());
    assertEquals("date#~", request.expressionAttributeValues().get(":SK_high").s());
  }
}
