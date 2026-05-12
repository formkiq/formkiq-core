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
package com.formkiq.aws.dynamodb.llm;

import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DocumentAiPromptValue}.
 */
public class DocumentAiPromptValueTest {

  @Test
  void testDocumentAiPromptResultTypeFromString01() {
    assertEquals(DocumentAiPromptResultType.KEY_VALUE,
        DocumentAiPromptResultType.fromString("key_value"));
    assertEquals(DocumentAiPromptResultType.ENTITY,
        DocumentAiPromptResultType.fromString("Entity"));
  }

  @Test
  void testDocumentAiPromptResultTypeFromString02() {
    assertThrows(IllegalArgumentException.class,
        () -> DocumentAiPromptResultType.fromString("missing"));
  }

  @Test
  void testEntity01() {
    DocumentAiPromptValue result =
        DocumentAiPromptValue.entity("Invoice", EntityTypeNamespace.CUSTOM,
            List.of(new DocumentAiPromptResultAttribute("invoice_no", List.of("123"))));

    assertEquals(
        Map.of("resultType", "ENTITY", "entityType", "Invoice", "entityNamespace", "CUSTOM",
            "attributes", List.of(Map.of("key", "invoice_no", "stringValues", List.of("123")))),
        result.toMap());
  }

  @Test
  void testEntity02() {
    assertThrows(NullPointerException.class,
        () -> new DocumentAiPromptValue(DocumentAiPromptResultType.ENTITY, null,
            EntityTypeNamespace.CUSTOM, List.of()));
  }

  @Test
  void testFromMap01() {
    DocumentAiPromptValue result = DocumentAiPromptValue.fromMap(
        Map.of("resultType", "entity", "entityType", "Invoice", "entityNamespace", "custom",
            "attributes", List.of(Map.of("key", "invoice_no", "stringValues", List.of("123")))));

    assertEquals(DocumentAiPromptResultType.ENTITY, result.resultType());
    assertEquals(EntityTypeNamespace.CUSTOM, result.entityNamespace());
  }

  @Test
  void testKeyValue01() {
    DocumentAiPromptValue result = DocumentAiPromptValue
        .keyValue(List.of(new DocumentAiPromptResultAttribute("invoice_no", List.of("123"))));

    assertEquals(Map.of("resultType", "KEY_VALUE", "attributes",
        List.of(Map.of("key", "invoice_no", "stringValues", List.of("123")))), result.toMap());
  }

  @Test
  void testKeyValue02() {
    assertThrows(IllegalArgumentException.class,
        () -> new DocumentAiPromptValue(DocumentAiPromptResultType.KEY_VALUE, "Invoice",
            EntityTypeNamespace.CUSTOM, List.of()));
  }
}
