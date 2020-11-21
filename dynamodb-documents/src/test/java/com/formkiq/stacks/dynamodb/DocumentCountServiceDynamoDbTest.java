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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Unit Tests for {@link DocumentCountServiceDynamoDb}. */
public class DocumentCountServiceDynamoDbTest {

  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder adb;

  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbhelper;

  /**
   * Generate Test Data.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException {

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    adb = new DynamoDbConnectionBuilder().setCredentials(cred).setRegion(Region.US_EAST_1)
        .setEndpointOverride("http://localhost:8000");

    dbhelper = new DynamoDbHelper(adb);

    if (!dbhelper.isDocumentsTableExists()) {
      dbhelper.createDocumentsTable();
      dbhelper.createCacheTable();
    }
  }

  /** Document Count Service. */
  private DocumentCountService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @Before
  public void before() throws Exception {
    this.service = new DocumentCountServiceDynamoDb(adb, "Documents");
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
