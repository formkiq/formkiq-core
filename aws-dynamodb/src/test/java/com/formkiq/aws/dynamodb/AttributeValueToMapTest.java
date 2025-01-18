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
package com.formkiq.aws.dynamodb;

import com.formkiq.aws.dynamodb.objects.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit Test for {@link AttributeValueToMap}.
 */
public class AttributeValueToMapTest {

  /** {@link AttributeValueToMap}. */
  private final AttributeValueToMap av = new AttributeValueToMap();

  @Test
  void testApply01() {
    // given
    Map<String, AttributeValue> map = Map.of("contentType", AttributeValue.fromS("text/plain"),
        "contentLength", AttributeValue.fromN("38"), "ids",
        AttributeValue.fromL(List.of(AttributeValue.fromS("123"), AttributeValue.fromS("444"))));

    // when
    Map<String, Object> result = av.apply(map);

    // then
    final int expected = 3;
    assertEquals(expected, result.size());
    assertEquals("text/plain", result.get("contentType"));
    assertEquals("38", Objects.formatDouble((Double) result.get("contentLength")));
    assertEquals("[123, 444]", result.get("ids").toString());
  }
}
