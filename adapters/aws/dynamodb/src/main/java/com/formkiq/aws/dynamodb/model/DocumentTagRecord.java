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
package com.formkiq.aws.dynamodb.model;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing a document tag item, with its DynamoDB key structure and metadata.
 */
public record DocumentTagRecord(DynamoDbKey key, String documentId, String artifactId,
    String tagKey, String tagValue, List<String> tagValues, Date insertedDate, String userId,
    DocumentTagType type) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copies.
   */
  public DocumentTagRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(tagKey, "tagKey must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");

    if (tagValues != null) {
      tagValues = List.copyOf(tagValues);
    }

    insertedDate = new Date(insertedDate.getTime());
    type = type != null ? type : DocumentTagType.USERDEFINED;
  }

  /**
   * Constructs a {@code DocumentTagRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code DocumentTagRecord} instance
   */
  public static DocumentTagRecord fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");

    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);
    String typeValue = DynamoDbTypes.toString(attributes.get("type"));

    return new DocumentTagRecord(key, DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toString(attributes.get("artifactId")),
        DynamoDbTypes.toString(attributes.get("tagKey")),
        DynamoDbTypes.toString(attributes.get("tagValue")),
        DynamoDbTypes.toStrings(attributes.get("tagValues")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")),
        DynamoDbTypes.toString(attributes.get("userId")),
        typeValue != null ? DocumentTagType.valueOf(typeValue) : DocumentTagType.USERDEFINED);
  }

  /**
   * Builds the DynamoDB item attribute map for this tag.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("documentId", documentId)
        .withString("artifactId", artifactId).withEnum("type", type).withString("tagKey", tagKey)
        .withString("tagValue", tagValue).withStrings("tagValues", tagValues)
        .withString("userId", userId).withDate("inserteddate", insertedDate).build();
  }

  /**
   * Creates a new {@link DocumentTagRecordBuilder} for {@link DocumentTagRecord}.
   *
   * @return a Builder instance
   */
  public static DocumentTagRecordBuilder builder() {
    return new DocumentTagRecordBuilder();
  }
}
