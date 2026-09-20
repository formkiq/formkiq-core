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
package com.formkiq.stacks.dynamodb.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.formkiq.aws.dynamodb.attributes.AttributeAccessApproval;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeType;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.validation.ValidationError;

/** Tests per-key access approval in {@link AttributeValidatorImpl}. */
public class AttributeValidatorAccessApprovalTest {

  private DocumentAttributeRecord attribute(final String key, final String value) {
    return new DocumentAttributeRecord().setKey(key).setStringValue(value)
        .setValueType(DocumentAttributeValueType.STRING).setUserId("student");
  }

  private AttributeRecord attributeRecord(final String key, final AttributeType type) {
    return new AttributeRecord().key(key).dataType(AttributeDataType.STRING).type(type);
  }

  /** An approved key does not grant access to another protected key. */
  @Test
  public void testAccessApprovalScopedByKey() {
    // given
    String approvedKey = "MyRole";
    String unapprovedKey = "Department";
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE,
        Map.of(approvedKey, AttributeValidationAccess.ADMIN_CREATE));

    List<DocumentAttributeRecord> attributes =
        List.of(attribute(approvedKey, "student"), attribute(unapprovedKey, "science"));
    Map<String, AttributeRecord> attributeRecords =
        Map.of(approvedKey, attributeRecord(approvedKey, AttributeType.OPA), unapprovedKey,
            attributeRecord(unapprovedKey, AttributeType.OPA));

    // when
    var validator = new AttributeValidatorImpl(null);
    var errors = validator.validatePartialAttribute(Collections.emptyList(), null, attributes,
        attributeRecords, approval);

    // then
    assertEquals(1, errors.size());
    assertEquals(unapprovedKey, errors.iterator().next().key());
  }

  /** A key override applies to every record for that key. */
  @Test
  public void testAccessApprovalSupportsMultipleValuesForKey() {
    // given
    String key = "MyRole";
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE,
        Map.of(key, AttributeValidationAccess.ADMIN_CREATE));

    // when
    var validator = new AttributeValidatorImpl(null);
    var errors = validator.validatePartialAttribute(Collections.emptyList(), null,
        List.of(attribute(key, "student"), attribute(key, "assistant")),
        Map.of(key, attributeRecord(key, AttributeType.OPA)), approval);

    // then
    assertTrue(errors.isEmpty());
  }

  /** The default access remains effective when there are no key overrides. */
  @Test
  public void testDefaultAccessRejectsProtectedAttribute() {
    // given
    String key = "MyRole";
    var approval = new AttributeAccessApproval(AttributeValidationAccess.CREATE);

    // when
    var validator = new AttributeValidatorImpl(null);
    var errors = validator.validatePartialAttribute(Collections.emptyList(), null,
        List.of(attribute(key, "student")), Map.of(key, attributeRecord(key, AttributeType.OPA)),
        approval);

    // then
    assertEquals(1, errors.size());
    assertEquals(key, errors.iterator().next().key());
  }

  /** Delete validation resolves access using the deleted attribute key. */
  @Test
  public void testDeleteAccessApprovalScopedByKey() {
    // given
    String approvedKey = "MyRole";
    String unapprovedKey = "Department";
    var approval = new AttributeAccessApproval(AttributeValidationAccess.DELETE,
        Map.of(approvedKey, AttributeValidationAccess.ADMIN_DELETE));
    Map<String, AttributeRecord> attributeRecords =
        Map.of(approvedKey, attributeRecord(approvedKey, AttributeType.OPA), unapprovedKey,
            attributeRecord(unapprovedKey, AttributeType.OPA));

    // when
    var validator = new AttributeValidatorImpl(null);
    var errors = validator.validateDeleteAttributes(Collections.emptyList(),
        List.of(approvedKey, unapprovedKey), attributeRecords, approval);

    // then
    assertEquals(1, errors.size());
    ValidationError error = errors.iterator().next();
    assertEquals(unapprovedKey, error.key());
  }
}
