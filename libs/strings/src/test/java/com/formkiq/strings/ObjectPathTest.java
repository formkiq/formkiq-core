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
package com.formkiq.strings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests typed context paths used by templates and rooted JSON searches. */
class ObjectPathTest {

  private Map<String, Object> context() {
    Map<String, Object> contract =
        Map.of("customer", Map.of("name", "Acme"), "items", List.of(Map.of("description", "First"),
            Map.of("description", "Second"), Map.of("description", "Third")));
    return Map.of("attributes",
        Map.of("CustomerName", List.of("Acme"), "Reviewer", List.of("Alice", "Bob"),
            "Invoice.Number", List.of("INV-123"), "Contract", List.of(contract)),
        "document", Map.of("documentId", "doc123", "path", "invoice.pdf"), "metadata",
        Map.of("Department", "Legal"), "auth", Map.of("username", "mike@example.com", "claims",
            Map.of("custom:department", "Sales", "roles", List.of("reviewer", "editor"))));
  }

  @Test
  void testAgreedWatermarkReferences() {
    // given
    Map<String, Object> root = context();
    Map<String, Object> references = Map.ofEntries(Map.entry("attributes.CustomerName[0]", "Acme"),
        Map.entry("attributes.Reviewer[1]", "Bob"),
        Map.entry("attributes[\"Invoice.Number\"][0]", "INV-123"),
        Map.entry("attributes.Contract[0].customer.name", "Acme"),
        Map.entry("attributes.Contract[0].items[2].description", "Third"),
        Map.entry("document.documentId", "doc123"), Map.entry("document.path", "invoice.pdf"),
        Map.entry("metadata.Department", "Legal"), Map.entry("auth.username", "mike@example.com"),
        Map.entry("auth.claims[\"custom:department\"]", "Sales"),
        Map.entry("auth.claims.roles[0]", "reviewer"));
    for (Map.Entry<String, Object> reference : references.entrySet()) {
      // when
      Object result = ObjectPath.parse(reference.getKey()).resolve(root);
      // then
      assertEquals(reference.getValue(), result, reference.getKey());
    }
  }

  @Test
  void testComposesWithTemplateSubstitution() {
    // given
    Map<String, Object> root = context();
    TemplateSubstitutor substitutor = new TemplateSubstitutor("${", "}");
    String template = "${attributes.Reviewer[1]} / ${auth.claims[\"custom:department\"]} / "
        + "${attributes.Contract[0].items[2].description} / ${document.missing}";
    // when
    String result =
        substitutor.substitute(template, expression -> ObjectPath.parse(expression).resolve(root));
    // then
    assertEquals("Bob / Sales / Third / ${document.missing}", result);
  }

  @Test
  void testMissingNullAndIncompatibleValues() {
    // given
    Map<String, Object> root = new HashMap<>();
    root.put("nullValue", null);
    root.put("list", Arrays.asList(null, "value"));
    root.put("scalar", "value");
    for (String expression : List.of("missing", "missing.child", "nullValue", "nullValue.child",
        "list[0].child", "list[2]", "list[999999999999999999999999]", "list.child", "scalar.child",
        "scalar[0]", "[0]")) {
      // when
      Object result = ObjectPath.parse(expression).resolve(root);
      // then
      assertNull(result, expression);
    }
  }

  @Test
  void testNullRoot() {
    // given
    ObjectPath root = ObjectPath.parse("$");
    ObjectPath nested = ObjectPath.parse("document.path");
    // when
    Object rootResult = root.resolve(null);
    Object nestedResult = nested.resolve(null);
    // then
    assertNull(rootResult);
    assertNull(nestedResult);
  }

  @Test
  void testNumericKeyDiffersFromListIndex() {
    // given
    Map<String, Object> root = Map.of("0", "zero");
    // when
    Object key = ObjectPath.parse("['0']").resolve(root);
    Object index = ObjectPath.parse("[0]").resolve(root);
    // then
    assertEquals("zero", key);
    assertNull(index);
  }

  @Test
  void testParsedSegmentsAreImmutableAndReusable() {
    // given
    ObjectPath path = ObjectPath.parse("attributes['Invoice.Number'][0]");
    Map<String, Object> first = context();
    Map<String, Object> second = Map.of("attributes", Map.of("Invoice.Number", List.of("INV-456")));
    // when
    Object firstResult = path.resolve(first);
    Object secondResult = path.resolve(second);
    final Executable mutation = () -> path.segments().add("other");
    // then
    assertEquals(List.of("attributes", "Invoice.Number", 0), path.segments());
    assertEquals("INV-123", firstResult);
    assertEquals("INV-456", secondResult);
    assertThrows(UnsupportedOperationException.class, mutation);
  }

  @Test
  void testQuotedKeysAndEscapedCharacters() {
    // given
    Map<String, Object> root = Map.of("customer.name", "literal", "customer",
        Map.of("name", "nested"), "it's", "apostrophe", "say\"hi", "quote", "back\\slash", "slash",
        "", "empty", "部門 name", "Unicode");
    Map<String, Object> references = Map.of("['customer.name']", "literal", "customer.name",
        "nested", "$['it\\'s']", "apostrophe", "[\"say\\\"hi\"]", "quote", "['back\\\\slash']",
        "slash", "['']", "empty", "[\"部門 name\"]", "Unicode");
    for (Map.Entry<String, Object> reference : references.entrySet()) {
      // when
      Object result = ObjectPath.parse(reference.getKey()).resolve(root);
      // then
      assertEquals(reference.getValue(), result, reference.getKey());
    }
  }

  @Test
  void testRejectsMalformedAndUnsupportedPaths() {
    // given
    List<String> invalid = Arrays.asList(null, "", " ", ".name", "$name", "$..name", "name.",
        "name..child", "name.*", "name[-1]", "name[01]", "name[+1]", "name[0:2]", "name[*]",
        "name[0]junk", "name[", "name['key'", "name['key]", "name[]", "name[key]", " name", "name ",
        "name()", "name + other");
    for (String expression : invalid) {
      // when
      Executable executable = () -> ObjectPath.parse(expression);
      // then
      assertEquals("invalid object path",
          assertThrows(IllegalArgumentException.class, executable, expression).getMessage());
    }
  }

  @Test
  void testRootAndRootListPaths() {
    // given
    List<Object> root = List.of(Map.of("name", "First"), List.of("Second"));
    // when
    Object sameRoot = ObjectPath.parse("$").resolve(root);
    Object first = ObjectPath.parse("[0].name").resolve(root);
    Object second = ObjectPath.parse("$[1][0]").resolve(root);
    // then
    assertSame(root, sameRoot);
    assertEquals("First", first);
    assertEquals("Second", second);
  }

  @Test
  void testRootedAndRelativePathsSelectTheSameValue() {
    // given
    Map<String, Object> root = context();
    // when
    Object relative = ObjectPath.parse("attributes.Contract[0].items[2].description").resolve(root);
    Object rooted = ObjectPath.parse("$.attributes.Contract[0].items[2].description").resolve(root);
    // then
    assertEquals("Third", relative);
    assertEquals(relative, rooted);
  }

  @Test
  void testValuesRetainTheirTypes() {
    // given
    Map<String, Object> root = Map.of("number", 42, "decimal", 1.5, "boolean", false, "object",
        Map.of("name", "Acme"), "list", List.of("one", "two"), "empty", "");
    for (Map.Entry<String, Object> entry : root.entrySet()) {
      // when
      Object result = ObjectPath.parse(entry.getKey()).resolve(root);
      // then
      assertSame(entry.getValue(), result);
    }
  }
}
