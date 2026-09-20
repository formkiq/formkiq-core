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


import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.base64.StringToBase64Decoder;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Tests for {@link AttributeValueToGlobalMetaFolder}.
 */
public class AttributeValueToGlobalMetaFolderTest {

  /** {@link AttributeValueToGlobalMetaFolder}. */
  private final AttributeValueToGlobalMetaFolder avg = new AttributeValueToGlobalMetaFolder();

  /**
   * Empty Map.
   */
  @Test
  void testApply01() {
    // given
    Map<String, AttributeValue> map = Collections.emptyMap();

    // when
    DocumentSearchResult result = avg.apply(map);

    // then
    assertNull(result);
  }

  /**
   * Not Empty.
   */
  @Test
  void testApply02() {
    // given
    Map<String, AttributeValue> map = Map.of(DbKeys.PK, AttributeValue.fromS("alkdjsad"));

    // when
    DocumentSearchResult results = avg.apply(map);

    // then
    assertNull(results);
  }

  /**
   * Folder record.
   */
  @Test
  void testApply03() {
    // given
    Map<String, AttributeValue> map = Map.of(DbKeys.PK, AttributeValue.fromS("alkdjsad"), DbKeys.SK,
        AttributeValue.fromS("ff#folder"), "documentId", AttributeValue.fromS("document1"), "path",
        AttributeValue.fromS("folder"));

    // when
    DocumentSearchResult results = avg.apply(map);

    // then
    assertEquals("folder", results.documentRecord().path());

    String indexKey = results.folderIndex().indexKey();
    assertTrue(results.folderIndex().folder());
    assertEquals("alkdjsad#folder", new StringToBase64Decoder().apply(indexKey));
    assertEquals("document1", results.documentRecord().documentId());
    assertNull(results.documentRecord().userId());
  }

  /**
   * Global tag record without a document id.
   */
  @Test
  void testApply04() {
    // given
    Map<String, AttributeValue> map =
        Map.of(DbKeys.PK, AttributeValue.fromS(DbKeys.GLOBAL_FOLDER_TAGS), "tagKey",
            AttributeValue.fromS("category"));

    // when
    DocumentSearchResult result = avg.apply(map);

    // then
    assertEquals("category", result.value());
  }
}
