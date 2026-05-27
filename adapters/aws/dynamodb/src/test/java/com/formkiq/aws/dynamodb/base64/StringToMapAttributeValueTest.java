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
package com.formkiq.aws.dynamodb.base64;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit Test for {@link StringToMapAttributeValue}.
 */
public class StringToMapAttributeValueTest {
  /** {@link StringToMapAttributeValue}. */
  private final StringToMapAttributeValue converter = new StringToMapAttributeValue();

  /**
   * Empty String.
   */
  @Test
  void testApply01() {
    assertNull(converter.apply(null), "Expected null AttributeValue map for empty string");
    assertNull(converter.apply(""), "Expected null AttributeValue map for empty string");
  }

  /**
   * Convert Base64 {@link String} to {@link Map}.
   */
  @Test
  void testApply02() {
    // given
    Map<String, AttributeValue> original =
        Map.of("alpha", AttributeValue.builder().s("one").build(), "beta",
            AttributeValue.builder().s("two").build());
    String encoded = new MapAttributeValueToString().apply(original);

    // when
    Map<String, AttributeValue> result = converter.apply(encoded);

    // then
    assertEquals(original.size(), result.size());
    original.forEach((k, v) -> assertEquals(v.s(), result.get(k).s()));
  }
}

