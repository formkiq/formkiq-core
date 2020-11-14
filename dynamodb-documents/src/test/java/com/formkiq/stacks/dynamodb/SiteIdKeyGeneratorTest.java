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

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.getDocumentId;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.UUID;
import org.junit.Test;

/**
 * 
 * Unit Tests for {@link SiteIdKeyGenerator}.
 *
 */
public class SiteIdKeyGeneratorTest {

  /**
   * Test Create Database Key.
   */
  @Test
  public void testCreateDatabaseKey01() {
    String siteId = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    assertNull(createDatabaseKey(null, null));
    assertEquals(id, createDatabaseKey(DEFAULT_SITE_ID, id));
    assertEquals(siteId + "/" + id, createDatabaseKey(siteId, id));
  }

  /**
   * Test create S3 Key.
   */
  @Test
  public void testCreateS3Key01() {
    String siteId = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    assertEquals(id, createS3Key(null, id));
    assertEquals(id, createS3Key(DEFAULT_SITE_ID, id));
    assertEquals(siteId + "/" + id, createS3Key(siteId, id));
  }

  /**
   * Test create S3 Key with Content-Type.
   */
  @Test
  public void testCreateS3Key02() {
    String contentType = "application/pdf";
    String siteId = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    assertEquals(id + "/application/pdf", createS3Key(null, id, contentType));
    assertEquals(id + "/application/pdf", createS3Key(DEFAULT_SITE_ID, id, contentType));
    assertEquals(siteId + "/" + id + "/application/pdf", createS3Key(siteId, id, contentType));
  }

  /**
   * Test Get DocumentId.
   */
  @Test
  public void testGetDocumentId() {
    String siteId = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    assertNull(getDocumentId(null));
    assertEquals(id, getDocumentId(id));
    assertEquals(id, getDocumentId(siteId + "/" + id));
    assertEquals(id, getDocumentId(DEFAULT_SITE_ID + "/" + id));
  }

  /**
   * Test Get SiteId.
   */
  @Test
  public void testGetSiteId() {
    String siteId = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    assertNull(getSiteId(null));
    assertNull(getSiteId(id));
    assertEquals(siteId, getSiteId(siteId + "/" + id));
    assertNull(getSiteId(DEFAULT_SITE_ID + "/" + id));
  }

  /**
   * Test Reset Database Key.
   */
  @Test
  public void testResetDatabaseKey01() {
    String siteId = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();
    assertEquals(id, resetDatabaseKey(siteId, siteId + "/" + id));
    assertEquals("asdas/" + id, resetDatabaseKey(siteId, "asdas/" + id));
    assertEquals(id, resetDatabaseKey(siteId, id));
    assertNull(resetDatabaseKey(null, null));
    assertNull(resetDatabaseKey(siteId, null));
    assertEquals(id, resetDatabaseKey(null, id));
  }
}
