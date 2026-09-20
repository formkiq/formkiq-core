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
package com.formkiq.aws.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.formkiq.aws.dynamodb.attributes.AttributeAccessApproval;
import com.formkiq.aws.dynamodb.attributes.AttributeAccessCriterion;
import com.formkiq.aws.dynamodb.attributes.AttributeAccessOperator;
import com.formkiq.aws.dynamodb.attributes.AttributeAccessRule;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;

/** Tests for {@link ApiAuthorization}. */
class ApiAuthorizationTest {

  /** Test request approval uses the handler's default and preserves key overrides. */
  @Test
  void testGetAttributeAccessApproval() {
    // given
    ApiAuthorization authorization = new ApiAuthorization()
        .attributeAccessApproval(new AttributeAccessApproval(AttributeValidationAccess.CREATE,
            Map.of("MyRole", AttributeValidationAccess.ADMIN_CREATE)));

    // when
    AttributeAccessApproval approval =
        authorization.getAttributeAccessApproval(AttributeValidationAccess.UPDATE);

    // then
    assertEquals(AttributeValidationAccess.UPDATE, approval.defaultAccess());
    assertEquals(AttributeValidationAccess.ADMIN_CREATE, approval.accessFor("MyRole"));
    assertEquals(AttributeValidationAccess.UPDATE, approval.accessFor("regularAttribute"));
  }

  /** Test authorization with no request approval uses the supplied default. */
  @Test
  void testGetAttributeAccessApprovalWithoutOverride() {
    // given
    ApiAuthorization authorization = new ApiAuthorization();

    // when
    AttributeAccessApproval approval =
        authorization.getAttributeAccessApproval(AttributeValidationAccess.CREATE);

    // then
    assertEquals(AttributeValidationAccess.CREATE, approval.defaultAccess());
    assertEquals(AttributeValidationAccess.CREATE, approval.accessFor("MyRole"));
  }

  /** Test request approval preserves conditional rules and its denial message. */
  @Test
  void testGetConditionalAttributeAccessApproval() {
    // given
    AttributeAccessRule rule = new AttributeAccessRule(
        List.of(new AttributeAccessCriterion("MyRole", "student", AttributeAccessOperator.EQ)),
        Map.of("MyRole", AttributeValidationAccess.ADMIN_CREATE));
    ApiAuthorization authorization =
        new ApiAuthorization().attributeAccessApproval(new AttributeAccessApproval(
            AttributeValidationAccess.CREATE, Map.of(), List.of(rule), "opa: denied"));

    // when
    AttributeAccessApproval approval =
        authorization.getAttributeAccessApproval(AttributeValidationAccess.UPDATE);

    // then
    assertEquals(AttributeValidationAccess.UPDATE, approval.defaultAccess());
    assertEquals(List.of(rule), approval.conditionalRules());
    assertEquals("opa: denied", approval.deniedMessage());
  }
}
