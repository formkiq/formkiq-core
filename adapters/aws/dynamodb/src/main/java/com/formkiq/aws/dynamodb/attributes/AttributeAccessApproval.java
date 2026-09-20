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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;

/**
 * Attribute access approval containing a default access and optional access overrides by attribute
 * key.
 *
 * @param defaultAccess {@link AttributeValidationAccess} used when an attribute key has no override
 * @param accessByKey {@link AttributeValidationAccess} overrides by attribute key
 * @param conditionalRules conditional access rules; alternatives are evaluated in list order
 * @param deniedMessage message returned when no conditional rule matches
 */
public record AttributeAccessApproval(AttributeValidationAccess defaultAccess,
    Map<String, AttributeValidationAccess> accessByKey, List<AttributeAccessRule> conditionalRules,
    String deniedMessage) {

  /** Default conditional access denial message. */
  public static final String DEFAULT_DENIED_MESSAGE = "attribute access criteria mismatch";

  /**
   * Constructor with no attribute key overrides.
   *
   * @param access {@link AttributeValidationAccess}
   */
  public AttributeAccessApproval(final AttributeValidationAccess access) {
    this(access, Map.of(), List.of(), DEFAULT_DENIED_MESSAGE);
  }

  /**
   * Constructor with attribute key overrides and no conditional rules.
   *
   * @param access {@link AttributeValidationAccess}
   * @param overrides access overrides by attribute key
   */
  public AttributeAccessApproval(final AttributeValidationAccess access,
      final Map<String, AttributeValidationAccess> overrides) {
    this(access, overrides, List.of(), DEFAULT_DENIED_MESSAGE);
  }

  /** Canonical constructor. */
  public AttributeAccessApproval {
    Objects.requireNonNull(defaultAccess, "defaultAccess is required");
    accessByKey = accessByKey != null ? Map.copyOf(accessByKey) : Map.of();
    conditionalRules = conditionalRules != null ? List.copyOf(conditionalRules) : List.of();
    deniedMessage = deniedMessage != null ? deniedMessage : DEFAULT_DENIED_MESSAGE;
  }

  /**
   * Gets the approved access for an attribute key.
   *
   * @param attributeKey {@link String}
   * @return {@link AttributeValidationAccess}
   */
  public AttributeValidationAccess accessFor(final String attributeKey) {
    return accessByKey.getOrDefault(attributeKey, defaultAccess);
  }

  /**
   * Resolves conditional rules against an effective document state.
   *
   * @param effectiveAttributes resulting document attribute state
   * @return resolved approval, or empty when conditional access is denied
   */
  public Optional<AttributeAccessApproval> resolve(
      final Collection<DocumentAttributeRecord> effectiveAttributes) {

    if (conditionalRules.isEmpty()) {
      return Optional.of(this);
    }

    AttributeAccessRuleMatcher matcher = new AttributeAccessRuleMatcher();
    return conditionalRules.stream().filter(rule -> matcher.matches(rule, effectiveAttributes))
        .findFirst().map(rule -> {
          Map<String, AttributeValidationAccess> resolved = new HashMap<>(accessByKey);
          resolved.putAll(rule.accessByKey());
          return new AttributeAccessApproval(defaultAccess, resolved, List.of(), deniedMessage);
        });
  }

  /**
   * Returns this approval with a new default access.
   *
   * @param access {@link AttributeValidationAccess}
   * @return {@link AttributeAccessApproval}
   */
  public AttributeAccessApproval withDefaultAccess(final AttributeValidationAccess access) {
    return new AttributeAccessApproval(access, accessByKey, conditionalRules, deniedMessage);
  }
}
