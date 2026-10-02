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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies strict filter parsing and exact comparison of DynamoDB numeric values. */
class JsonAttributeSearchFilterDeserializerTest {

  private static Stream<Arguments> exactNumbers() {
    return Stream.of(Arguments.of("eq", "9007199254740993", "9007199254740992", false),
        Arguments.of("eq", "9007199254740993", "9007199254740993", true),
        Arguments.of("gt", "1.00000000000000000001", "1.00000000000000000000", false),
        Arguments.of("gte", "1.00000000000000000001", "1.00000000000000000001", true),
        Arguments.of("lt", "1.00000000000000000001", "1.00000000000000000000", true),
        Arguments.of("lte", "1.00000000000000000000", "1.00000000000000000001", false),
        Arguments.of("eq", "0", "-0", true));
  }

  private static Stream<String> invalidFilters() {
    return Stream.of("{\"path\":\"$.value\",\"eq\":1,\"range\":{\"start\":0,\"end\":2}}",
        "{\"path\":\"$.value\",\"unknown\":true}", "{\"path\":\"$.value\",\"eq\":null,\"gte\":1}",
        "{\"path\":false,\"eq\":1}", "{\"path\":\"$.value\",\"beginsWith\":2}",
        "{\"path\":\"$.value\",\"gt\":\"2\"}", "{\"path\":\"$.value\",\"gte\":true}",
        "{\"path\":\"$.value\",\"eqOr\":[1,null]}", "{\"path\":\"$.value\",\"eqOr\":\"paid\"}",
        "{\"path\":\"$.value\",\"eq\":{}}", "{\"path\":\"$.value\",\"eq\":[]}", "[]");
  }

  /** API filter adapter, isolated from other Gson configurations. */
  private final Gson gson = new GsonBuilder().registerTypeAdapter(JsonAttributeSearchFilter.class,
      new JsonAttributeSearchFilterDeserializer()).create();

  @ParameterizedTest
  @MethodSource("exactNumbers")
  void testExactNumbers(final String operator, final String comparison, final String stored,
      final boolean expected) {
    // given
    String json = "{\"path\":\"$.value\",\"" + operator + "\":" + comparison + "}";
    JsonAttributeSearchFilter filter = this.gson.fromJson(json, JsonAttributeSearchFilter.class);
    var search = new SearchAttributeCriteria("details", null, null, null, null, filter);
    JsonAttributeSearchPredicate predicate = new JsonAttributeSearchPredicate(search);
    Map<String, AttributeValue> item =
        Map.of("jsonValue", AttributeValue.fromM(Map.of("value", AttributeValue.fromN(stored))));

    // when
    boolean result = predicate.test(item);

    // then
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("invalidFilters")
  void testInvalidFilters(final String json) {
    // given
    Class<JsonAttributeSearchFilter> type = JsonAttributeSearchFilter.class;

    // when
    Executable executable = () -> this.gson.fromJson(json, type);

    // then
    assertThrows(JsonParseException.class, executable);
  }
}
