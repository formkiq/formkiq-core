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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the compatibility boundary between JSON search and the shared path parser. */
class JsonAttributeSearchPathTest {

  @Test
  void testLegacyValidationAndMessages() {
    // given
    List<String> invalid = Arrays.asList(null, "", "$", "value", "['name']", "$..name", "$.*",
        "$.items[-1]", "$.items[01]", "$.items[0:2]", "$['']", "$[\"\"]", "$.items[0]junk");
    for (String path : invalid) {
      // when
      Executable executable = () -> JsonAttributeSearchPath.parse(path);
      // then
      assertEquals("invalid JSON search path",
          assertThrows(IllegalArgumentException.class, executable).getMessage());
    }
  }

  @Test
  void testNestedDynamoDbPathAndQuotedKey() {
    // given
    AttributeValue root = AttributeValue.fromM(Map.of("items", AttributeValue.fromL(
        List.of(AttributeValue.fromM(Map.of("custom:name", AttributeValue.fromS("Acme")))))));
    // when
    List<Object> segments = JsonAttributeSearchPath.parse("$.items[0]['custom:name']");
    AttributeValue result = JsonAttributeSearchPath.resolve(root, segments);
    // then
    assertEquals(List.of("items", 0, "custom:name"), segments);
    assertEquals("Acme", result.s());
  }

  @Test
  void testOversizedIndexStillResolvesAsMissing() {
    // given
    AttributeValue root = AttributeValue
        .fromM(Map.of("items", AttributeValue.fromL(List.of(AttributeValue.fromS("Acme")))));
    // when
    List<Object> segments = JsonAttributeSearchPath.parse("$.items[999999999999999999999999]");
    AttributeValue result = JsonAttributeSearchPath.resolve(root, segments);
    // then
    assertNull(result);
  }
}
