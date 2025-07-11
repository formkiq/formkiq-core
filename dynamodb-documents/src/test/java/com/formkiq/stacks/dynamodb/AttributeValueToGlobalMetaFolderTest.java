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
import com.formkiq.aws.dynamodb.base64.StringToBase66Decoder;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    Map<String, Object> results = avg.apply(map);

    // then
    assertEquals(0, results.size());
  }

  /**
   * Not Empty.
   */
  @Test
  void testApply02() {
    // given
    Map<String, AttributeValue> map = Map.of(DbKeys.PK, AttributeValue.fromS("alkdjsad"));

    // when
    Map<String, Object> results = avg.apply(map);

    // then
    final int expected = 6;
    assertEquals(expected, results.size());
    assertEquals("path,insertedDate,lastModifiedDate,indexKey,documentId,userId",
        String.join(",", results.keySet().stream().toList()));
    assertNull(results.get("path"));
    assertNull(results.get("insertedDate"));
    assertNull(results.get("lastModifiedDate"));

    String indexKey = results.get("indexKey").toString();
    assertEquals("YWxrZGpzYWQjbnVsbA", indexKey);
    assertEquals("alkdjsad#null", new StringToBase66Decoder().apply(indexKey));
    assertNull(results.get("documentId"));
    assertNull(results.get("userId"));
  }
}
