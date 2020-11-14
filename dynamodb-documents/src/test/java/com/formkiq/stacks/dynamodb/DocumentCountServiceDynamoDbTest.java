/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
