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

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/** Unit Tests for {@link DocumentCountServiceDynamoDb}. */
@ExtendWith(DynamoDbExtension.class)
public class DocumentCountServiceDynamoDbTest {

  /** {@link DynamoDbConnectionBuilder}. */
  private DynamoDbConnectionBuilder db;
  /** Document Count Service. */
  private DocumentCountService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.db = DynamoDbTestServices.getDynamoDbConnection();
    this.service = new DocumentCountServiceDynamoDb(this.db, DOCUMENTS_TABLE);
  }

  /**
   * Increment Document Count with SiteId.
   */
  @Test
  public void testIncrementDocumentCount01() {
    // given
    String siteId = UUID.randomUUID().toString();

    try {
      // when
      this.service.incrementDocumentCount(siteId);

      // then
      assertEquals(1, this.service.getDocumentCount(siteId));

    } finally {
      this.service.removeDocumentCount(siteId);
    }
  }

  /**
   * Increment Document Count without SiteId.
   */
  @Test
  public void testIncrementDocumentCount02() {
    // given
    String siteId = null;

    try {
      // when
      this.service.incrementDocumentCount(siteId);

      // then
      assertTrue(this.service.getDocumentCount(siteId) > 0);
    } finally {
      this.service.removeDocumentCount(siteId);
    }
  }
}
