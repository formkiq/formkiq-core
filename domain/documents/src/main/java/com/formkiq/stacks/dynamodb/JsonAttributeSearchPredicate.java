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
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Validates and evaluates JSON string equality, boolean strings, and numeric bounds. */
final class JsonAttributeSearchPredicate implements Predicate<Map<String, AttributeValue>> {

  /** Comparisons to apply to the resolved field. */
  private final JsonAttributeSearchFilter filter;
  /** Parsed property names and array positions. */
  private final List<Object> segments;

  JsonAttributeSearchPredicate(final SearchAttributeCriteria search) {
    this.filter = search.json();
    try {
      this.segments = JsonAttributeSearchPath.parse(this.filter.path());
      validate(search);
    } catch (IllegalArgumentException e) {
      throw ValidationException.builder().error(search.key(), e.getMessage()).build();
    }
  }

  private boolean equalsValue(final AttributeValue value, final String expected) {
    return expected.equals(value.s())
        || value.bool() != null && expected.equals(value.bool().toString());
  }

  private boolean matchesBounds(final AttributeValue value) {
    boolean bounded =
        Stream.of(filter.gt(), filter.gte(), filter.lt(), filter.lte()).anyMatch(Objects::nonNull);
    if (!bounded) {
      return true;
    }
    if (value.n() == null) {
      return false;
    }
    BigDecimal number = new BigDecimal(value.n());
    return matchesLower(number) && matchesUpper(number);
  }

  private boolean matchesEquality(final AttributeValue value) {
    boolean equal = this.filter.eq() == null || equalsValue(value, this.filter.eq());
    boolean any = this.filter.eqOr() == null
        || this.filter.eqOr().stream().anyMatch(expected -> equalsValue(value, expected));
    return equal && any;
  }

  private boolean matchesLower(final BigDecimal value) {
    return (filter.gt() == null || value.compareTo(filter.gt()) > 0)
        && (filter.gte() == null || value.compareTo(filter.gte()) >= 0);
  }

  private boolean matchesPrefix(final AttributeValue value) {
    return this.filter.beginsWith() == null
        || value.s() != null && value.s().startsWith(this.filter.beginsWith());
  }

  private boolean matchesUpper(final BigDecimal value) {
    return (filter.lt() == null || value.compareTo(filter.lt()) < 0)
        && (filter.lte() == null || value.compareTo(filter.lte()) <= 0);
  }

  @Override
  public boolean test(final Map<String, AttributeValue> item) {
    AttributeValue root = item.get("jsonValue");
    AttributeValue value = JsonAttributeSearchPath.resolve(root, this.segments);
    return value != null && matchesEquality(value) && matchesPrefix(value) && matchesBounds(value);
  }

  private void validate(final SearchAttributeCriteria search) {
    validateScalarOperators(search);
    boolean hasComparison = Stream.of(filter.eq(), filter.eqOr(), filter.beginsWith(), filter.gt(),
        filter.gte(), filter.lt(), filter.lte()).anyMatch(Objects::nonNull);
    if (!hasComparison) {
      throw new IllegalArgumentException("json requires at least one comparison");
    }
    if (filter.eqOr() != null) {
      if (filter.eqOr().isEmpty()) {
        throw new IllegalArgumentException("json eqOr requires at least one value");
      }
      filter.eqOr().forEach(this::validateString);
    }
  }

  private void validateScalarOperators(final SearchAttributeCriteria search) {
    if (Stream.of(search.eq(), search.eqOr(), search.beginsWith(), search.range())
        .anyMatch(Objects::nonNull)) {
      throw new IllegalArgumentException("json cannot be combined with scalar search operators");
    }
  }

  private void validateString(final String value) {
    if (value == null) {
      throw new IllegalArgumentException("json eqOr requires string values");
    }
  }
}
