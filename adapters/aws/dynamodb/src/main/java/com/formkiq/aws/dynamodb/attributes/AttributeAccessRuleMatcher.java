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
package com.formkiq.aws.dynamodb.attributes;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;

/** Matches conditional attribute access rules against document attributes. */
public class AttributeAccessRuleMatcher {

  /**
   * Tests whether a rule matches the effective attributes.
   *
   * @param rule {@link AttributeAccessRule}
   * @param effectiveAttributes resulting document attribute state
   * @return whether the rule matches
   */
  public boolean matches(final AttributeAccessRule rule,
      final Collection<DocumentAttributeRecord> effectiveAttributes) {

    Collection<DocumentAttributeRecord> effective = notNull(effectiveAttributes);
    return rule.criteria().stream().allMatch(criterion -> matchesCriterion(criterion, effective));
  }

  private boolean matchesCriterion(final AttributeAccessCriterion criterion,
      final Collection<DocumentAttributeRecord> attributes) {
    List<Object> values =
        attributes.stream().filter(attribute -> criterion.attributeKey().equals(attribute.getKey()))
            .map(this::getValue).toList();

    return switch (criterion.operator()) {
      case EQ ->
        !values.isEmpty() && values.stream().allMatch(value -> equal(value, criterion.value()));
      case GT -> !values.isEmpty() && values.stream()
          .allMatch(value -> compare(value, criterion.value(), result -> result > 0));
      case GTE -> !values.isEmpty() && values.stream()
          .allMatch(value -> compare(value, criterion.value(), result -> result >= 0));
      case IN -> !values.isEmpty()
          && values.stream().allMatch(value -> containedIn(value, criterion.value()));
      case LT -> !values.isEmpty() && values.stream()
          .allMatch(value -> compare(value, criterion.value(), result -> result < 0));
      case LTE -> !values.isEmpty() && values.stream()
          .allMatch(value -> compare(value, criterion.value(), result -> result <= 0));
      case NEQ ->
        !values.isEmpty() && values.stream().noneMatch(value -> equal(value, criterion.value()));
      case NOT_IN -> !values.isEmpty()
          && values.stream().noneMatch(value -> containedIn(value, criterion.value()));
    };
  }

  private boolean containedIn(final Object value, final Object candidates) {
    return candidates instanceof Collection<?> collection
        && collection.stream().anyMatch(candidate -> equal(value, candidate));
  }

  private boolean equal(final Object left, final Object right) {
    if (left instanceof Number || right instanceof Number) {
      Double leftNumber = toNumber(left);
      Double rightNumber = toNumber(right);
      return leftNumber != null && rightNumber != null && leftNumber.compareTo(rightNumber) == 0;
    }
    return Objects.equals(left, right);
  }

  private boolean compare(final Object left, final Object right, final IntPredicate predicate) {
    Double leftNumber = toNumber(left);
    Double rightNumber = toNumber(right);
    return leftNumber != null && rightNumber != null
        && predicate.test(leftNumber.compareTo(rightNumber));
  }

  private Object getValue(final DocumentAttributeRecord attribute) {
    DocumentAttributeValueType type = attribute.getValueType();
    if (DocumentAttributeValueType.BOOLEAN.equals(type)) {
      return attribute.getBooleanValue();
    } else if (DocumentAttributeValueType.NUMBER.equals(type)) {
      return attribute.getNumberValue();
    } else if (DocumentAttributeValueType.DATE.equals(type)) {
      return attribute.getDateValueAsString();
    }
    return attribute.getStringValue();
  }

  private Collection<DocumentAttributeRecord> notNull(
      final Collection<DocumentAttributeRecord> attributes) {
    return attributes != null ? attributes : List.of();
  }

  private Double toNumber(final Object value) {
    try {
      return value instanceof Number || value instanceof String ? Double.valueOf(value.toString())
          : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
