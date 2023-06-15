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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getDocumentId;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;

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
    assertNull(getSiteId("formkiq:://sample/test.txt.fkb64"));
    assertEquals("bleh", getSiteId("bleh/formkiq:://sample/test.txt.fkb64"));
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
