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
package com.formkiq.stacks.dynamodb.numbering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Unit tests for {@link NumberingSequenceServiceDynamoDb}. */
@ExtendWith(DynamoDbExtension.class)
class NumberingSequenceServiceDynamoDbTest {

  /** Service under test. */
  private NumberingSequenceService service;

  @BeforeEach
  void before() throws URISyntaxException {
    DynamoDbConnectionBuilder connection = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService db = new DynamoDbServiceImpl(connection, "Documents");
    Clock clock = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC);
    this.service = new NumberingSequenceServiceDynamoDb(db, clock);
  }

  @Test
  void shouldAllocateYearlySequenceFromConfiguredInitialNumber() {
    String siteId = ID.uuid();
    NumberingSequenceRecord definition = NumberingSequenceRecord.builder()
        .attributeKey("agreementNumber").pattern("CONTRACT-{YEAR}-{SEQUENCE}").startAt(75L)
        .padding(5).reset(NumberingSequenceReset.YEARLY).timezone("UTC").build(siteId);

    NumberingSequence saved = this.service.save(siteId, definition);
    assertNull(saved.currentSequence());
    assertNull(saved.currentPeriod());
    assertNull(saved.lastValue());

    NumberingSequenceValue first = this.service.next(siteId, "agreementNumber");
    final NumberingSequenceValue second = this.service.next(siteId, "agreementNumber");

    assertEquals("CONTRACT-2026-00075", first.value());
    assertEquals(75L, first.sequence());
    assertEquals("2026", first.period());
    assertEquals("CONTRACT-2026-00076", second.value());

    NumberingSequence current = this.service.find(siteId, "agreementNumber");
    assertEquals(76L, current.currentSequence());
    assertEquals("2026", current.currentPeriod());
    assertEquals("CONTRACT-2026-00076", current.lastValue());
  }

  @Test
  void shouldListDefinitionsUsingPagination() {
    String siteId = ID.uuid();
    this.service.save(siteId,
        NumberingSequenceRecord.builder().attributeKey("agreementNumber")
            .pattern("CONTRACT-{SEQUENCE}").startAt(1L).padding(5)
            .reset(NumberingSequenceReset.NONE).timezone("UTC").build(siteId));
    this.service.save(siteId,
        NumberingSequenceRecord.builder().attributeKey("invoiceNumber")
            .pattern("INVOICE-{SEQUENCE}").startAt(100L).padding(6)
            .reset(NumberingSequenceReset.NONE).timezone("UTC").build(siteId));
    this.service.next(siteId, "agreementNumber");

    Pagination<NumberingSequence> first = this.service.findAll(siteId, null, 1);
    assertEquals(1, first.getResults().size());
    assertNotNull(first.getNextToken());

    Pagination<NumberingSequence> second = this.service.findAll(siteId, first.getNextToken(), 1);
    assertEquals(1, second.getResults().size());
    assertEquals("invoiceNumber", second.getResults().getFirst().attributeKey());
    assertNull(second.getResults().getFirst().currentSequence());
  }

  @Test
  void shouldParsePreviouslyGeneratedValue() {
    String siteId = ID.uuid();
    this.service.save(siteId,
        NumberingSequenceRecord.builder().attributeKey("agreementNumber")
            .pattern("CONTRACT-{YEAR}-{SEQUENCE}").startAt(1L).padding(5)
            .reset(NumberingSequenceReset.YEARLY).timezone("UTC").build(siteId));

    NumberingSequenceValue parsed =
        this.service.parse(siteId, "agreementNumber", "CONTRACT-2026-00123");

    assertEquals(123L, parsed.sequence());
    assertEquals("2026", parsed.period());
  }
}
