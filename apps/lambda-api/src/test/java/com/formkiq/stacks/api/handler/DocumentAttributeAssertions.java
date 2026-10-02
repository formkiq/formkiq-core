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
package com.formkiq.stacks.api.handler;

import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DocumentAttribute;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Assertions for document attribute responses. */
final class DocumentAttributeAssertions {
  static void assertBooleanAttribute(final DocumentAttribute attribute, final String key,
      final Boolean expected) {
    assertValues(attribute, key, new DocumentAttribute().booleanValue(expected));
  }

  static void assertJsonAttribute(final DocumentAttribute attribute, final String key,
      final Map<String, Object> expected) {
    assertValues(attribute, key, new DocumentAttribute().jsonValue(expected));
    assertEquals(AttributeValueType.JSON, attribute.getValueType());
  }

  static void assertKeyOnlyAttribute(final DocumentAttribute attribute, final String key) {
    assertValues(attribute, key, new DocumentAttribute());
  }

  static void assertNumberAttribute(final DocumentAttribute attribute, final String key,
      final String expected) {
    assertValues(attribute, key, new DocumentAttribute().numberValue(new BigDecimal(expected)));
  }

  static void assertNumbersAttribute(final DocumentAttribute attribute, final String key,
      final List<String> expected) {
    assertValues(attribute, key,
        new DocumentAttribute().numberValues(expected.stream().map(BigDecimal::new).toList()));
  }

  static void assertStringAttribute(final DocumentAttribute attribute, final String key,
      final String expected) {
    assertValues(attribute, key, new DocumentAttribute().stringValue(expected));
  }

  static void assertStringsAttribute(final DocumentAttribute attribute, final String key,
      final List<String> expected) {
    assertValues(attribute, key, new DocumentAttribute().stringValues(expected));
  }

  private static void assertValues(final DocumentAttribute attribute, final String key,
      final DocumentAttribute expected) {
    assertNotNull(attribute);
    assertEquals(key, attribute.getKey());
    assertEquals("joesmith", attribute.getUserId());
    assertNotNull(attribute.getInsertedDate());
    assertEquals(expected.getStringValue(), attribute.getStringValue());
    assertEquals(notNull(expected.getStringValues()), notNull(attribute.getStringValues()));
    if (expected.getNumberValue() != null) {
      assertNotNull(attribute.getNumberValue(), "Missing numeric value for attribute " + key);
      assertEquals(formatDouble(expected.getNumberValue().doubleValue()),
          formatDouble(attribute.getNumberValue().doubleValue()));
    } else {
      assertNull(attribute.getNumberValue());
    }
    assertEquals(
        notNull(expected.getNumberValues()).stream()
            .map(number -> formatDouble(number.doubleValue())).toList(),
        notNull(attribute.getNumberValues()).stream()
            .map(number -> formatDouble(number.doubleValue())).toList());
    assertEquals(expected.getBooleanValue(), attribute.getBooleanValue());
    assertEquals(expected.getJsonValue(), attribute.getJsonValue());
  }

  static DocumentAttribute findAttribute(final List<DocumentAttribute> attributes,
      final String key) {
    assertNotNull(attributes);
    return attributes.stream().filter(attribute -> key.equals(attribute.getKey())).findFirst()
        .orElseThrow(() -> new AssertionError("Missing attribute: " + key));
  }

  private DocumentAttributeAssertions() {}
}
