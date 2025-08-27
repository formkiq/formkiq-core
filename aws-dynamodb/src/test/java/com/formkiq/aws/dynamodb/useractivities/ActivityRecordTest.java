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
package com.formkiq.aws.dynamodb.useractivities;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test {@link ActivityRecord}.
 */
public class ActivityRecordTest {

  /** User Id. */
  private static final String USER_ID = "user1";
  /** Today. */
  private static final String TODAY = DateUtil.getYyyyMmDdFormatter().format(new Date());

  @Test
  void testBuildDocumentsKey() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = ID.uuid();
      ActivityRecord.Builder builder =
          ActivityRecord.builder().resource("documents").documentId(documentId).userId(USER_ID);

      // when
      DynamoDbKey key = builder.buildKey(siteId);

      // then
      assertKeyEquals(siteId, "doc#" + documentId, key.pk());
      assertTrue(key.sk().startsWith("activity#"));
      assertTrue(key.sk().contains("#" + documentId + "#"));

      assertKeyEquals(siteId, "activity#user#" + USER_ID, key.gsi1Pk());
      assertTrue(key.gsi1Sk().startsWith("activity#"));
      assertTrue(key.gsi1Sk().contains("#" + documentId + "#"));

      assertKeyEquals(siteId, "activity#" + TODAY, key.gsi2Pk());
      assertTrue(key.gsi2Sk().startsWith("activity#"));
      assertTrue(key.gsi2Sk().contains("#" + documentId + "#"));
    }
  }

  @Test
  void testBuildEntitiesKey() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String entityId = ID.uuid();
      String entityTypeId = ID.uuid();

      ActivityRecord.Builder builder = ActivityRecord.builder().resource("entities")
          .entityId(entityId).entityTypeId(entityTypeId).userId(USER_ID);

      // when
      DynamoDbKey key = builder.buildKey(siteId);

      // then
      assertKeyEquals(siteId, "entity#" + entityTypeId + "#" + entityId, key.pk());
      assertTrue(key.sk().startsWith("activity#"));
      assertTrue(key.sk().endsWith("#" + entityId));

      assertKeyEquals(siteId, "activity#user#" + USER_ID, key.gsi1Pk());
      assertTrue(key.gsi1Sk().startsWith("activity#"));
      assertTrue(key.gsi1Sk().endsWith("#" + entityId));

      assertKeyEquals(siteId, "activity#" + TODAY, key.gsi2Pk());
      assertTrue(key.gsi2Sk().startsWith("activity#"));
      assertTrue(key.gsi2Sk().endsWith("#" + entityId));

    }
  }

  @Test
  void testBuildEntityTypesKey() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String entityTypeId = ID.uuid();
      ActivityRecord.Builder builder = ActivityRecord.builder().resource("entityTypes")
          .entityTypeId(entityTypeId).userId(USER_ID);

      // when
      DynamoDbKey key = builder.buildKey(siteId);

      // then
      assertKeyEquals(siteId, "entityType#" + entityTypeId, key.pk());
      assertTrue(key.sk().startsWith("activity#"));
      assertTrue(key.sk().endsWith("#" + entityTypeId));

      assertKeyEquals(siteId, "activity#user#" + USER_ID, key.gsi1Pk());
      assertTrue(key.gsi1Sk().startsWith("activity#"));
      assertTrue(key.gsi1Sk().endsWith("#" + entityTypeId));

      assertKeyEquals(siteId, "activity#" + TODAY, key.gsi2Pk());
      assertTrue(key.gsi2Sk().startsWith("activity#"));
      assertTrue(key.gsi2Sk().endsWith("#" + entityTypeId));
    }
  }

  private void assertKeyEquals(final String siteId, final String expectedPk, final String gotPk) {
    assertEquals(siteId != null ? siteId + "/" + expectedPk : expectedPk, gotPk);
  }
}
