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
package com.formkiq.stacks.dynamodb.numbering;

import java.util.Map;
import java.util.Objects;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** DynamoDB record containing a numbering sequence definition. */
@Reflectable
public record NumberingSequenceRecord(DynamoDbKey key, String attributeKey, String pattern,
    Long startAt, Integer padding, NumberingSequenceReset reset, String timezone) {

  /** Numbering sequence partition key suffix. */
  public static final String PK_PREFIX = "numberingSequences";
  /** Numbering sequence definition sort key prefix. */
  public static final String SK_PREFIX = "sequence#";
  /** Numbering sequence counter sort key prefix. */
  public static final String COUNTER_PREFIX = "counter#";

  /** Canonical constructor enforcing the sequence identity fields. */
  public NumberingSequenceRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(attributeKey, "attributeKey must not be null");
  }

  /**
   * Creates a builder for a numbering sequence definition.
   *
   * @return builder
   */
  public static NumberingSequenceRecordBuilder builder() {
    return new NumberingSequenceRecordBuilder();
  }

  /**
   * Build the key for a sequence counter.
   *
   * @param siteId Site identifier
   * @param attributeKey Attribute key
   * @param period Sequence period
   * @return DynamoDB key
   */
  public static DynamoDbKey counterKey(final String siteId, final String attributeKey,
      final String period) {
    return DynamoDbKey.builder().pk(siteId, PK_PREFIX)
        .sk(COUNTER_PREFIX + attributeKey + "#" + period).build();
  }

  /**
   * Constructs a numbering sequence definition from DynamoDB attributes.
   *
   * @param attributes DynamoDB attributes
   * @return numbering sequence definition, or null for an empty map
   */
  public static NumberingSequenceRecord fromAttributeMap(
      final Map<String, AttributeValue> attributes) {
    NumberingSequenceRecord record = null;
    if (attributes != null && !attributes.isEmpty()) {
      DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);
      String resetValue = DynamoDbTypes.toString(attributes.get("reset"));
      record =
          new NumberingSequenceRecord(key, DynamoDbTypes.toString(attributes.get("attributeKey")),
              DynamoDbTypes.toString(attributes.get("pattern")),
              DynamoDbTypes.toLong(attributes.get("startAt")),
              DynamoDbTypes.toInteger(attributes.get("padding")),
              resetValue != null ? NumberingSequenceReset.valueOf(resetValue) : null,
              DynamoDbTypes.toString(attributes.get("timezone")));
    }
    return record;
  }

  /**
   * Builds the DynamoDB attribute map for this definition.
   *
   * @return DynamoDB attributes
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("attributeKey", attributeKey)
        .withString("pattern", pattern).withLong("startAt", startAt).withInteger("padding", padding)
        .withEnum("reset", reset).withString("timezone", timezone).build();
  }
}
