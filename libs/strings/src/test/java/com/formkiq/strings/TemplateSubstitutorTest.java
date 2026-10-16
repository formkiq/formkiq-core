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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for {@link TemplateSubstitutor}. */
class TemplateSubstitutorTest {

  @Test
  void testCalculatedValuesAreResolvedForEveryOccurrence() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{", "}");
    AtomicInteger sequence = new AtomicInteger();
    // when
    String result =
        substitutor.substitute("{SEQUENCE}/{SEQUENCE}", _ -> sequence.incrementAndGet());
    // then
    assertEquals("1/2", result);
    assertEquals(2, sequence.get());
  }

  @Test
  void testCognitoMarkers() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("${", "}");
    Map<String, String> values = Map.of("email", "mike@example.com", "emailLocal", "mike", "link",
        "https://example.com/confirm");
    // when
    String result = substitutor.substitute("${emailLocal}: ${email}\n${link}", values);
    // then
    assertEquals("mike: mike@example.com\nhttps://example.com/confirm", result);
  }

  @Test
  void testEmptyAndUnclosedPlaceholdersArePreserved() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    Map<String, String> values = Map.of("", "empty", "name", "Mike");
    // when
    String result = substitutor.substitute("{{}} {{name}} {{unclosed", values);
    // then
    assertEquals("{{}} Mike {{unclosed", result);
  }

  @Test
  void testIdenticalMarkers() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("%", "%");
    // when
    String result = substitutor.substitute("%name%%name%", Map.of("name", "Mike"));
    // then
    assertEquals("MikeMike", result);
  }

  @Test
  void testInvalidMarkers() {
    // given
    Executable emptyOpening = () -> new TemplateSubstitutor("", "}");
    Executable emptyClosing = () -> new TemplateSubstitutor("{", "");
    Executable nullOpening = () -> new TemplateSubstitutor(null, "}");
    Executable nullClosing = () -> new TemplateSubstitutor("{", null);
    // when
    Executable[] emptyMarkers = {emptyOpening, emptyClosing};
    Executable[] nullMarkers = {nullOpening, nullClosing};
    // then
    for (Executable executable : emptyMarkers) {
      assertThrows(IllegalArgumentException.class, executable);
    }
    for (Executable executable : nullMarkers) {
      assertThrows(NullPointerException.class, executable);
    }
  }

  @Test
  void testLiteralCustomMarkersAndValues() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("$[.*", ".*]$");
    Map<String, String> values = Map.of("name", "$5\\folder\nRésumé");
    // when
    String result = substitutor.substitute("$[.*name.*]$", values);
    // then
    assertEquals("$5\\folder\nRésumé", result);
  }

  @Test
  void testLookupCanReturnMissingOrLiteralValues() throws IOException {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("${", "}");
    TemplateValueResolver<IOException> resolver =
        expression -> "service:text".equals(expression) ? "$5\\folder ${missing}" : null;
    // when
    String result = substitutor.substituteWithResolver("${service:text}/${missing}", resolver);
    // then
    assertEquals("$5\\folder ${missing}/${missing}", result);
  }

  @Test
  void testLookupFailurePropagatesAndStopsResolution() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    IOException failure = new IOException("Remote service unavailable");
    List<String> lookups = new ArrayList<>();
    TemplateValueResolver<IOException> resolver = expression -> {
      lookups.add(expression);
      if ("service:status".equals(expression)) {
        throw failure;
      }
      return "Acme";
    };
    // when
    Executable executable = () -> substitutor.substituteWithResolver(
        "{{database:customerName}} {{service:status}} {{unused}}", resolver);
    // then
    assertSame(failure, assertThrows(IOException.class, executable));
    assertEquals(List.of("database:customerName", "service:status"), lookups);
  }

  @Test
  void testLookupIsNotInvokedForNullEmptyOrUnclosedPlaceholders() throws IOException {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    TemplateValueResolver<IOException> resolver = _ -> {
      throw new IOException("Lookup should not be invoked");
    };
    // when
    String nullResult = substitutor.substituteWithResolver(null, resolver);
    String result = substitutor.substituteWithResolver("{{}} {{unclosed", resolver);
    // then
    assertNull(nullResult);
    assertEquals("{{}} {{unclosed", result);
  }

  @Test
  void testLookupsAreLazyAndCanMixValueSources() throws IOException {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    Map<String, String> values = Map.of("documentId", "doc123");
    List<String> lookups = new ArrayList<>();
    TemplateValueResolver<IOException> resolver = expression -> {
      lookups.add(expression);
      return switch (expression) {
        case "database:customerName" -> "Acme";
        case "service:status" -> "Approved";
        default -> values.get(expression);
      };
    };
    // when
    String staticResult = substitutor.substituteWithResolver("Static text", resolver);
    List<String> staticLookups = List.copyOf(lookups);
    String result = substitutor.substituteWithResolver(
        "{{documentId}} / {{database:customerName}} / {{service:status}}", resolver);
    // then
    assertEquals("Static text", staticResult);
    assertEquals(List.of(), staticLookups);
    assertEquals("doc123 / Acme / Approved", result);
    assertEquals(List.of("documentId", "database:customerName", "service:status"), lookups);
  }

  @Test
  void testMissingNullAndEmptyValues() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    Map<String, String> values = new HashMap<>();
    values.put("null", null);
    values.put("empty", "");
    // when
    String result = substitutor.substitute("{{missing}}/{{null}}/{{empty}}", values);
    // then
    assertEquals("{{missing}}/{{null}}/", result);
  }

  @Test
  void testNamesAreExact() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("[[", "]]");
    Map<String, String> values = Map.of("name", "Mike", " name ", "spaced");
    // when
    String result = substitutor.substitute("[[name]]/[[ name ]]/[[NAME]]", values);
    // then
    assertEquals("Mike/spaced/[[NAME]]", result);
  }

  @Test
  void testNotificationMarkers() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    Map<String, String> values = Map.of("documentId", "doc123", "AgreementName", "Contract");
    // when
    String result = substitutor.substitute("{{AgreementName}}: {{documentId}}", values);
    // then
    assertEquals("Contract: doc123", result);
  }

  @Test
  void testNullEmptyAndStaticTemplates() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    Map<String, String> values = Map.of();
    // when
    String nullResult = substitutor.substitute(null, values);
    String emptyResult = substitutor.substitute("", values);
    String staticResult = substitutor.substitute("Static text }}", values);
    // then
    assertNull(nullResult);
    assertEquals("", emptyResult);
    assertEquals("Static text }}", staticResult);
  }

  @Test
  void testNullLookupResolver() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{", "}");
    // when
    Executable executable = () -> substitutor.substituteWithResolver("{name}", null);
    // then
    assertThrows(NullPointerException.class, executable);
  }

  @Test
  void testNullValueSources() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{", "}");

    // when
    Executable nullMap = () -> substitutor.substitute("{name}", (Map<String, String>) null);
    Executable nullResolver = () -> substitutor.substitute("{name}", (Function<String, String>) null);

    // then
    assertThrows(NullPointerException.class, nullMap);
    assertThrows(NullPointerException.class, nullResolver);
  }

  @Test
  void testNumberingMarkers() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{", "}");
    Map<String, Object> values = Map.of("YEAR", 2026, "SEQUENCE", "00042");
    // when
    String result = substitutor.substitute("INV-{YEAR}-{SEQUENCE}", values);
    // then
    assertEquals("INV-2026-00042", result);
  }

  @Test
  void testResolverCanTrimAndProvideDefaults() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{{", "}}");
    Map<String, String> values = Map.of("name", "Mike");
    // when
    String result = substitutor.substitute("{{ name }}/{{missing}}",
        name -> values.getOrDefault(name.trim(), "default"));
    // then
    assertEquals("Mike/default", result);
  }

  @Test
  void testResolverExceptionsPropagate() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{", "}");
    Function<String, String> resolver = name -> {
      throw new IllegalArgumentException("Missing " + name);
    };
    // when
    Executable executable = () -> substitutor.substitute("{missing}", resolver);
    // then
    assertEquals("Missing missing",
        assertThrows(IllegalArgumentException.class, executable).getMessage());
  }

  @Test
  void testSinglePassWithRepeatedVariables() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("${", "}");
    Map<String, String> values = Map.of("first", "${second}", "second", "expanded");
    // when
    String result = substitutor.substitute("${first}${second}/${first}", values);
    // then
    assertEquals("${second}expanded/${second}", result);
  }

  @Test
  void testValuesAreSuppliedPerCall() {
    // given
    TemplateSubstitutor substitutor = new TemplateSubstitutor("{", "}");
    // when
    String first = substitutor.substitute("{name}", Map.of("name", "Mike"));
    String second = substitutor.substitute("{name}", Map.of("name", "Jane"));
    // then
    assertEquals("Mike", first);
    assertEquals("Jane", second);
  }
}
