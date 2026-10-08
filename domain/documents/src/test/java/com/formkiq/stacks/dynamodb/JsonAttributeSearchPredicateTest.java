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

import com.formkiq.aws.dynamodb.builder.ObjectToAttributeValue;
import com.formkiq.aws.dynamodb.model.JsonAttributeSearchFilter;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.validation.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.function.Executable;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests nested path resolution, comparison types, operator boundaries, and invalid filters. */
class JsonAttributeSearchPredicateTest {

  private static Stream<Arguments> comparisons() {
    return Stream.concat(Stream.concat(scalarComparisons(), boundComparisons()), pathComparisons());
  }

  private static Stream<Arguments> scalarComparisons() {
    return Stream.of(
        Arguments.of("{\"path\":\"$.name\",\"eq\":\"Acme\"}", "{\"name\":\"Acme\"}", true),
        Arguments.of("{\"path\":\"$.name\",\"eq\":\"Acme\"}", "{\"name\":\"Beta\"}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"2\"}", "{\"value\":\"2\"}", true),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"2\"}", "{\"value\":2}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"false\"}", "{\"value\":false}", true),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"false\"}", "{\"value\":\"false\"}", true),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"true\"}", "{\"value\":true}", true),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"true\"}", "{\"value\":false}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"TRUE\"}", "{\"value\":true}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"False\"}", "{\"value\":false}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"0\"}", "{\"value\":false}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"\"}", "{\"value\":\"\"}", true),
        Arguments.of("{\"path\":\"$.value\",\"eqOr\":[\"false\",\"2\",\"paid\"]}", "{\"value\":2}",
            false),
        Arguments.of("{\"path\":\"$.value\",\"eqOr\":[\"false\",\"2\",\"paid\"]}",
            "{\"value\":\"2\"}", true),
        Arguments.of("{\"path\":\"$.value\",\"eqOr\":[\"false\",\"2\",\"paid\"]}",
            "{\"value\":false}", true),
        Arguments.of("{\"path\":\"$.value\",\"eqOr\":[\"true\",\"paid\"]}", "{\"value\":true}",
            true),
        Arguments.of("{\"path\":\"$.value\",\"eqOr\":[\"true\",\"paid\"]}", "{\"value\":false}",
            false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"true\",\"eqOr\":[\"false\"]}",
            "{\"value\":true}", false),
        Arguments.of("{\"path\":\"$.value\",\"beginsWith\":\"Ac\"}", "{\"value\":\"Acme\"}", true),
        Arguments.of("{\"path\":\"$.value\",\"beginsWith\":\"2\"}", "{\"value\":2}", false));
  }

  private static Stream<Arguments> boundComparisons() {
    return Stream.of(Arguments.of("{\"path\":\"$.value\",\"gt\":2}", "{\"value\":2}", false),
        Arguments.of("{\"path\":\"$.value\",\"gte\":2}", "{\"value\":2}", true),
        Arguments.of("{\"path\":\"$.value\",\"lt\":2}", "{\"value\":2}", false),
        Arguments.of("{\"path\":\"$.value\",\"lte\":2}", "{\"value\":2}", true),
        Arguments.of("{\"path\":\"$.value\",\"gte\":1,\"lte\":3}", "{\"value\":2}", true),
        Arguments.of("{\"path\":\"$.value\",\"gte\":1,\"lte\":3}", "{\"value\":\"2\"}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"2\",\"gt\":2}", "{\"value\":2}", false),
        Arguments.of("{\"path\":\"$.value\",\"gte\":2,\"lte\":2}", "{\"value\":2}", true));
  }

  private static Stream<Arguments> pathComparisons() {
    return Stream.of(
        Arguments.of("{\"path\":\"$.customer.name\",\"eq\":\"Acme\"}",
            "{\"customer\":{\"name\":\"Acme\"}}", true),
        Arguments.of("{\"path\":\"$.customer.name\",\"eq\":\"Acme\"}", "{\"name\":\"Acme\"}",
            false),
        Arguments.of("{\"path\":\"$.items[0].quantity\",\"gte\":2}",
            "{\"items\":[{\"quantity\":2}]}", true),
        Arguments.of("{\"path\":\"$.items[1].quantity\",\"gte\":2}",
            "{\"items\":[{\"quantity\":2}]}", false),
        Arguments.of("{\"path\":\"$['customer.name']\",\"eq\":\"Acme\"}",
            "{\"customer.name\":\"Acme\"}", true),
        Arguments.of("{\"path\":\"$[\\\"customer.name\\\"]\",\"eq\":\"Acme\"}",
            "{\"customer.name\":\"Acme\"}", true),
        Arguments.of("{\"path\":\"$['it\\\\'s']\",\"eq\":\"Acme\"}", "{\"it's\":\"Acme\"}", true),
        Arguments.of("{\"path\":\"$['back\\\\\\\\slash']\",\"eq\":\"Acme\"}",
            "{\"back\\\\slash\":\"Acme\"}", true),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"Acme\"}", "{\"value\":null}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"Acme\"}", "{\"value\":{}}", false),
        Arguments.of("{\"path\":\"$.value\",\"eq\":\"Acme\"}", "{\"value\":[]}", false),
        Arguments.of("{\"path\":\"$.value[0]\",\"eq\":\"Acme\"}", "{\"value\":\"Acme\"}", false),
        Arguments.of("{\"path\":\"$.value.name\",\"eq\":\"Acme\"}", "{\"value\":\"Acme\"}", false));
  }

  private static Stream<String> invalidFilters() {
    return Stream.of("{}", "{\"path\":\"$.value\"}", "{\"path\":\"$.value\",\"eqOr\":[]}",
        "{\"path\":\"$.value\",\"eqOr\":[null]}", "{\"path\":\"$.value\",\"eq\":null}",
        "{\"path\":null,\"eq\":\"Acme\"}", "{\"path\":\"$\",\"eq\":\"Acme\"}",
        "{\"path\":\"value\",\"eq\":\"Acme\"}", "{\"path\":\"$..value\",\"eq\":\"Acme\"}",
        "{\"path\":\"$.*\",\"eq\":\"Acme\"}", "{\"path\":\"$.value[-1]\",\"eq\":\"Acme\"}",
        "{\"path\":\"$.value[01]\",\"eq\":\"Acme\"}", "{\"path\":\"$.value[0:2]\",\"eq\":\"Acme\"}",
        "{\"path\":\"$['']\",\"eq\":\"Acme\"}", "{\"path\":\"$.value[0]junk\",\"eq\":\"Acme\"}");
  }

  @ParameterizedTest
  @MethodSource("comparisons")
  void testComparisons(final String filterJson, final String objectJson, final boolean expected) {
    // given
    JsonAttributeSearchFilter filter =
        GsonUtil.getInstance().fromJson(filterJson, JsonAttributeSearchFilter.class);
    var search = new SearchAttributeCriteria("details", null, null, null, null, filter);
    Map<String, AttributeValue> item = Map.of("jsonValue",
        new ObjectToAttributeValue().apply(GsonUtil.getInstance().fromJson(objectJson, Map.class)));
    JsonAttributeSearchPredicate predicate = new JsonAttributeSearchPredicate(search);

    // when
    boolean result = predicate.test(item);

    // then
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("invalidFilters")
  void testInvalidFilter(final String filterJson) {
    // given
    JsonAttributeSearchFilter filter =
        GsonUtil.getInstance().fromJson(filterJson, JsonAttributeSearchFilter.class);
    var search = new SearchAttributeCriteria("details", null, null, null, null, filter);

    // when
    Executable executable = () -> new JsonAttributeSearchPredicate(search);

    // then
    assertThrows(ValidationException.class, executable);
  }

  @ParameterizedTest
  @ValueSource(strings = {"eq", "eqOr", "beginsWith", "range"})
  void testMixedScalarOperators(final String operator) {
    // given
    String json = "{\"key\":\"details\",\"json\":{\"path\":\"$.name\",\"eq\":\"Acme\"},\""
        + operator + "\":" + switch (operator) {
          case "eqOr" -> "[\"Acme\"]";
          case "range" -> "{\"start\":\"A\",\"end\":\"Z\"}";
          default -> "\"Acme\"";
        } + "}";
    SearchAttributeCriteria search =
        GsonUtil.getInstance().fromJson(json, SearchAttributeCriteria.class);

    // when
    Executable executable = () -> new JsonAttributeSearchPredicate(search);

    // then
    assertThrows(ValidationException.class, executable);
  }
}
