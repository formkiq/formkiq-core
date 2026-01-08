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
package com.formkiq.aws.dynamodb.useractivities;

import com.formkiq.aws.dynamodb.DynamoDbShardKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing a activity with its DynamoDB key structure and metadata.
 */
public record ActivityRecord(DynamoDbShardKey key, String resource, UserActivityType type,
    UserActivityStatus status, String sourceIpAddress, String source, String userId,
    String rulesetId, String ruleId, String entityTypeId, String entityId, String documentId,
    String workflowId, String attributeKey, String message, Date insertedDate, String versionPk,
    String versionSk, Map<String, Object> changes) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public ActivityRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(resource, "resource must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");
  }

  /**
   * Builds the DynamoDB item attribute map for this entity, starting from the key attributes and
   * adding metadata fields.
   * <p>
   * Only non-null values are included via {@link DynamoDbAttributeMapBuilder}.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("resource", resource)
        .withString("type", type.name()).withString("status", status.name())
        .withString("sourceIpAddress", sourceIpAddress).withString("source", source)
        .withString("userId", userId).withString("message", message)
        .withString("entityTypeId", entityTypeId).withString("entityId", entityId)
        .withString("rulesetId", rulesetId).withString("ruleId", ruleId)
        .withString("documentId", documentId).withString("workflowId", workflowId)
        .withString("attributeKey", attributeKey).withDate("inserteddate", insertedDate)
        .withMap("changes", changes).build();
  }
}
