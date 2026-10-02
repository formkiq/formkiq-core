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

/** Validates and evaluates typed JSON comparisons without coercing strings or booleans. */
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

  private BigDecimal decimal(final Object number) {
    return new BigDecimal(number.toString());
  }

  private boolean equalsValue(final AttributeValue value, final Object expected) {
    return switch (expected) {
      case String string -> string.equals(value.s());
      case Boolean bool -> bool.equals(value.bool());
      case Number number ->
        value.n() != null && new BigDecimal(value.n()).compareTo(decimal(number)) == 0;
      default -> false;
    };
  }

  private boolean finite(final Number number) {
    return number instanceof BigDecimal || Double.isFinite(number.doubleValue());
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
    return (filter.gt() == null || value.compareTo(decimal(filter.gt())) > 0)
        && (filter.gte() == null || value.compareTo(decimal(filter.gte())) >= 0);
  }

  private boolean matchesPrefix(final AttributeValue value) {
    return this.filter.beginsWith() == null
        || value.s() != null && value.s().startsWith((String) this.filter.beginsWith());
  }

  private boolean matchesUpper(final BigDecimal value) {
    return (filter.lt() == null || value.compareTo(decimal(filter.lt())) < 0)
        && (filter.lte() == null || value.compareTo(decimal(filter.lte())) <= 0);
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
    if (filter.eq() != null) {
      validateScalar(filter.eq());
    }
    if (filter.eqOr() != null) {
      if (filter.eqOr().isEmpty()) {
        throw new IllegalArgumentException("json eqOr requires at least one value");
      }
      filter.eqOr().forEach(this::validateScalar);
    }
    Stream.of(filter.gt(), filter.gte(), filter.lt(), filter.lte()).filter(Objects::nonNull)
        .forEach(this::validateNumber);
    if (filter.beginsWith() != null && !(filter.beginsWith() instanceof String)) {
      throw new IllegalArgumentException("json beginsWith requires a string");
    }
  }

  private void validateNumber(final Object value) {
    if (!(value instanceof Number number) || !finite(number)) {
      throw new IllegalArgumentException("JSON bounds require a number");
    }
  }

  private void validateScalar(final Object value) {
    boolean scalar = value instanceof String || value instanceof Boolean;
    boolean number = value instanceof Number n && finite(n);
    if (!scalar && !number) {
      throw new IllegalArgumentException("JSON comparisons require a string, number, or boolean");
    }
  }

  private void validateScalarOperators(final SearchAttributeCriteria search) {
    if (Stream.of(search.eq(), search.eqOr(), search.beginsWith(), search.range())
        .anyMatch(Objects::nonNull)) {
      throw new IllegalArgumentException("json cannot be combined with scalar search operators");
    }
  }
}
