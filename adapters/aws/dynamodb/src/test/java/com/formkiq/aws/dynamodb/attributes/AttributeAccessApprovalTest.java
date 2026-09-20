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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;

/** Unit tests for {@link AttributeAccessApproval}. */
public class AttributeAccessApprovalTest {

  private DocumentAttributeRecord attribute(final String key, final Object value) {
    DocumentAttributeRecord attribute = new DocumentAttributeRecord().setKey(key);
    if (value instanceof Number number) {
      attribute.setNumberValue(number.doubleValue());
    } else if (value instanceof Boolean booleanValue) {
      attribute.setBooleanValue(booleanValue);
    } else {
      attribute.setStringValue(value.toString());
    }
    attribute.updateValueType();
    return attribute;
  }

  private DocumentAttributeRecord attribute(final String key, final String value) {
    return attribute(key, (Object) value);
  }

  private AttributeAccessRule rule(final String key, final String value) {
    return new AttributeAccessRule(
        List.of(new AttributeAccessCriterion(key, value, AttributeAccessOperator.EQ)),
        Map.of(key, AttributeValidationAccess.ADMIN_CREATE));
  }

  /**
   * Every conditional attribute access operator matches its permitted example.
   *
   * @param operator {@link AttributeAccessOperator}
   */
  @ParameterizedTest
  @EnumSource(AttributeAccessOperator.class)
  public void testConditionalRuleOperators(final AttributeAccessOperator operator) {
    // given
    String key = "TestAttribute";
    Object attributeValue;
    Object criteriaValue;
    switch (operator) {
      case EQ -> {
        attributeValue = "student";
        criteriaValue = "student";
      }
      case GT -> {
        attributeValue = 101;
        criteriaValue = 100;
      }
      case GTE -> {
        attributeValue = 100;
        criteriaValue = 100;
      }
      case IN -> {
        attributeValue = "student";
        criteriaValue = List.of("student", "teacher");
      }
      case LT -> {
        attributeValue = 99;
        criteriaValue = 100;
      }
      case LTE -> {
        attributeValue = 100;
        criteriaValue = 100;
      }
      case NEQ -> {
        attributeValue = "student";
        criteriaValue = "teacher";
      }
      case NOT_IN -> {
        attributeValue = "student";
        criteriaValue = List.of("teacher", "administrator");
      }
      default -> throw new IllegalArgumentException("Unexpected operator: " + operator);
    }
    DocumentAttributeRecord attribute = attribute(key, attributeValue);
    AttributeAccessRule rule =
        new AttributeAccessRule(List.of(new AttributeAccessCriterion(key, criteriaValue, operator)),
            Map.of(key, AttributeValidationAccess.ADMIN_CREATE));
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE, Map.of(),
        List.of(rule), "denied");

    // when
    var resolved = approval.resolve(List.of(attribute));

    // then
    assertTrue(resolved.isPresent(), () -> operator + " should match its permitted example");
    assertEquals(AttributeValidationAccess.ADMIN_CREATE, resolved.orElseThrow().accessFor(key));
  }

  /**
   * Every effective value for an elevated key must satisfy that key's criteria.
   *
   * @param operator {@link AttributeAccessOperator}
   */
  @ParameterizedTest
  @EnumSource(AttributeAccessOperator.class)
  public void testConditionalRuleRejectsForbiddenEffectiveValue(
      final AttributeAccessOperator operator) {
    // given
    List<?> operatorValues = switch (operator) {
      case EQ -> List.of("student", "teacher", "student");
      case GT -> List.of(101, 99, 100);
      case GTE -> List.of(100, 99, 100);
      case IN -> List.of("student", "administrator", List.of("student", "teacher"));
      case LT -> List.of(99, 101, 100);
      case LTE -> List.of(100, 101, 100);
      case NEQ -> List.of("student", "teacher", "teacher");
      case NOT_IN -> List.of("student", "teacher", List.of("teacher", "administrator"));
    };
    DocumentAttributeRecord permitted = attribute("MyRole", operatorValues.get(0));
    DocumentAttributeRecord forbidden = attribute("MyRole", operatorValues.get(1));
    AttributeAccessRule rule = new AttributeAccessRule(
        List.of(new AttributeAccessCriterion("MyRole", operatorValues.get(2), operator)),
        Map.of("MyRole", AttributeValidationAccess.ADMIN_UPDATE));
    var approval = new AttributeAccessApproval(AttributeValidationAccess.UPDATE, Map.of(),
        List.of(rule), "denied");
    // when
    var resolved = approval.resolve(List.of(permitted, forbidden));
    // then
    assertFalse(resolved.isPresent(), () -> operator + " should reject a forbidden value");
  }

  /** IN permits multiple effective values when every value belongs to the permitted set. */
  @Test
  public void testConditionalRuleInPermitsMultipleEffectiveValues() {
    // given
    DocumentAttributeRecord student = attribute("MyRole", "student");
    DocumentAttributeRecord teacher = attribute("MyRole", "teacher");
    AttributeAccessRule rule =
        new AttributeAccessRule(
            List.of(new AttributeAccessCriterion("MyRole", List.of("student", "teacher"),
                AttributeAccessOperator.IN)),
            Map.of("MyRole", AttributeValidationAccess.ADMIN_CREATE));
    var approval = new AttributeAccessApproval(AttributeValidationAccess.UPDATE, Map.of(),
        List.of(rule), "denied");

    // when
    var resolved = approval.resolve(List.of(student, teacher));

    // then
    assertTrue(resolved.isPresent());
  }

  /** A matching conditional rule grants its key access. */
  @Test
  public void testConditionalRuleMatch() {
    // given
    DocumentAttributeRecord attribute = attribute("MyRole", "student");
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE, Map.of(),
        List.of(rule("MyRole", "student")), "denied");

    // when
    var resolved = approval.resolve(List.of(attribute));

    // then
    assertTrue(resolved.isPresent());
    assertEquals(AttributeValidationAccess.ADMIN_CREATE,
        resolved.orElseThrow().accessFor("MyRole"));
    assertEquals(AttributeValidationAccess.CREATE,
        resolved.orElseThrow().accessFor("regularAttribute"));
  }

  /** A conditional rule may match an attribute generated after request interception. */
  @Test
  public void testConditionalRuleMatchesGeneratedAttribute() {
    // given
    DocumentAttributeRecord generatedDefault = attribute("MyRole", "student");
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE, Map.of(),
        List.of(rule("MyRole", "student")), "denied");

    // when
    var resolved = approval.resolve(List.of(generatedDefault));

    // then
    assertTrue(resolved.isPresent());
  }

  /** A conditional rule denies a forbidden attribute value. */
  @Test
  public void testConditionalRuleNoMatch() {
    // given
    DocumentAttributeRecord attribute = attribute("MyRole", "teacher");
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE, Map.of(),
        List.of(rule("MyRole", "student")), "opa: denied");

    // when
    var resolved = approval.resolve(List.of(attribute));

    // then
    assertFalse(resolved.isPresent());
    assertEquals("opa: denied", approval.deniedMessage());
  }

  /** Conditional rules use OR semantics. */
  @Test
  public void testConditionalRulesUseOrSemantics() {
    // given
    DocumentAttributeRecord attribute = attribute("MyRole", "teacher");
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE, Map.of(),
        List.of(rule("MyRole", "student"), rule("MyRole", "teacher")), "denied");

    // when
    var resolved = approval.resolve(List.of(attribute));

    // then
    assertTrue(resolved.isPresent());
  }

  /** Default access is returned when no key override exists. */
  @Test
  public void testDefaultAccess() {
    // given
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE);

    // when
    AttributeValidationAccess access = approval.accessFor("MyRole");

    // then
    assertEquals(AttributeValidationAccess.CREATE, access);
  }

  /** Default access is required. */
  @Test
  public void testDefaultAccessRequired() {
    // given
    AttributeValidationAccess access = null;

    // when
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new AttributeAccessApproval(access));

    // then
    assertEquals("defaultAccess is required", exception.getMessage());
  }

  /** Key overrides are immutable. */
  @Test
  public void testImmutableAccessByKey() {
    // given
    Map<String, AttributeValidationAccess> accessByKey = new HashMap<>();
    accessByKey.put("MyRole", AttributeValidationAccess.ADMIN_CREATE);

    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE, accessByKey);

    // when
    accessByKey.put("Title", AttributeValidationAccess.ADMIN_CREATE);

    // then
    assertEquals(AttributeValidationAccess.CREATE, approval.accessFor("Title"));
    assertThrows(UnsupportedOperationException.class,
        () -> approval.accessByKey().put("Title", AttributeValidationAccess.ADMIN_CREATE));
  }

  /** A key override takes precedence over the default access. */
  @Test
  public void testKeyAccess() {
    // given
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE,
        Map.of("MyRole", AttributeValidationAccess.ADMIN_CREATE));

    // when
    AttributeValidationAccess myRoleAccess = approval.accessFor("MyRole");
    AttributeValidationAccess titleAccess = approval.accessFor("Title");

    // then
    assertEquals(AttributeValidationAccess.ADMIN_CREATE, myRoleAccess);
    assertEquals(AttributeValidationAccess.CREATE, titleAccess);
  }
}
