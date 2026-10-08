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

import com.formkiq.aws.dynamodb.model.JsonAttributeSearchFilter;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies ordinary JSON mapping of equality strings and precise numeric bounds. */
class JsonAttributeSearchFilterTest {

  private static Stream<Arguments> equalityValues() {
    return Stream.of(Arguments.of("\"true\"", "true"), Arguments.of("true", "true"),
        Arguments.of("\"false\"", "false"), Arguments.of("false", "false"),
        Arguments.of("\"2\"", "2"), Arguments.of("2", "2"));
  }

  private static Stream<Arguments> exactNumbers() {
    return Stream.of(Arguments.of("gte", "9007199254740993", "9007199254740992", false),
        Arguments.of("gte", "9007199254740993", "9007199254740993", true),
        Arguments.of("gt", "1.00000000000000000001", "1.00000000000000000000", false),
        Arguments.of("gte", "1.00000000000000000001", "1.00000000000000000001", true),
        Arguments.of("lt", "1.00000000000000000001", "1.00000000000000000000", true),
        Arguments.of("lte", "1.00000000000000000000", "1.00000000000000000001", false),
        Arguments.of("gte", "0", "-0", true));
  }

  @ParameterizedTest
  @MethodSource("equalityValues")
  void testEqualityMapping(final String literal, final String expected) {
    // given
    String json = "{\"path\":\"$.value\",\"eq\":" + literal + ",\"eqOr\":[" + literal + "]}";

    // when
    JsonAttributeSearchFilter filter =
        GsonUtil.getInstance().fromJson(json, JsonAttributeSearchFilter.class);

    // then
    assertEquals(expected, filter.eq());
    assertEquals(List.of(expected), filter.eqOr());
  }

  @ParameterizedTest
  @MethodSource("exactNumbers")
  void testExactNumbers(final String operator, final String comparison, final String stored,
      final boolean expected) {
    // given
    String json = "{\"path\":\"$.value\",\"" + operator + "\":" + comparison + "}";
    JsonAttributeSearchFilter filter =
        GsonUtil.getInstance().fromJson(json, JsonAttributeSearchFilter.class);
    var search = new SearchAttributeCriteria("details", null, null, null, null, filter);
    JsonAttributeSearchPredicate predicate = new JsonAttributeSearchPredicate(search);
    Map<String, AttributeValue> item =
        Map.of("jsonValue", AttributeValue.fromM(Map.of("value", AttributeValue.fromN(stored))));

    // when
    boolean result = predicate.test(item);

    // then
    assertEquals(expected, result);
  }
}
