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

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbShardKey;
import com.formkiq.aws.dynamodb.DynamoDbShardKeyAttributeBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteIdName;

/**
 * Record representing an Document Activity Event, with its DynamoDB key structure and metadata.
 */
public record DocumentActivityEventRecord(DynamoDbKey key, String siteId, String documentId,
    Date insertedDate, Collection<DynamoDbShardKey> activityKeys) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public DocumentActivityEventRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(siteId, "siteId must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");
    Objects.requireNonNull(activityKeys, "activityKeys must not be null");
    insertedDate = new Date(insertedDate.getTime());
  }

  /**
   * Constructs a {@code EntityTypeRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code EntityTypeRecord} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static DocumentActivityEventRecord fromAttributeMap(
      final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);

    Collection<DynamoDbShardKey> activityKeys =
        DynamoDbTypes.toCustom("activityKeys", attributes, new DynamoDbShardKeyAttributeBuilder());
    return new DocumentActivityEventRecord(key, DynamoDbTypes.toString(attributes.get("siteId")),
        DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")), activityKeys);
  }

  /**
   * Builds the DynamoDB item attribute map for this entity, starting from the key attributes and
   * adding metadata fields.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("documentId", documentId)
        .withString("siteId", siteId).withTimeToLiveInDays(1).withDate("inserteddate", insertedDate)
        .withCustom("activityKeys", activityKeys, new DynamoDbShardKeyAttributeBuilder()).build();
  }

  /**
   * Creates a new {@link Builder} for {@link DocumentActivityEventRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link DocumentActivityEventRecord} that computes the DynamoDbKey.
   */
  public static class Builder implements DynamoDbEntityBuilder<DocumentActivityEventRecord> {
    /** Document Id. */
    private String documentId;
    /** Activity Keys. */
    private Collection<DynamoDbShardKey> activityKeys;

    /**
     * Sets the activity {@link DynamoDbKey}.
     *
     * @param documentActivityKeys {@link Collection} {@link DynamoDbShardKey}
     * @return this Builder
     */
    public Builder activityKeys(final Collection<DynamoDbShardKey> documentActivityKeys) {
      this.activityKeys = documentActivityKeys;
      return this;
    }

    @Override
    public DocumentActivityEventRecord build(final String siteId) {
      DynamoDbKey key = buildKey(siteId);
      return new DocumentActivityEventRecord(key, getSiteIdName(siteId), documentId, new Date(),
          activityKeys);
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {

      Objects.requireNonNull(documentId, "documentId must not be null");

      String pk = "documentEvent";
      String sk = "event#docs#activities#" + DateUtil.getInIso8601Format(new Date()) + "#"
          + documentId + "#" + ID.uuid();
      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).build();
    }

    /**
     * Sets the document identifier.
     *
     * @param entityTypeDocumentId the document ID
     * @return this Builder
     */
    public Builder documentId(final String entityTypeDocumentId) {
      this.documentId = entityTypeDocumentId;
      return this;
    }
  }
}
