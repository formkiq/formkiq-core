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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link MapAttributeValueToString}.
 */
public class MapAttributeValueToStringTest {
  /** {@link MapAttributeValueToString}. */
  private final MapAttributeValueToString converter = new MapAttributeValueToString();

  @Test
  void testApply01() {
    assertEquals("", converter.apply(Map.of()),
        "Expected empty string for empty AttributeValue map");
  }

  @Test
  void testApply02() {
    // given
    Map<String, AttributeValue> input = Map.of("k1", AttributeValue.builder().s("v1").build(), "k2",
        AttributeValue.builder().s("v2").build());
    String result = converter.apply(input);

    // when
    String decoded = new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8);

    // then
    assertTrue(decoded.contains("k1=v1"));
    assertTrue(decoded.contains("k2=v2"));
  }
}
