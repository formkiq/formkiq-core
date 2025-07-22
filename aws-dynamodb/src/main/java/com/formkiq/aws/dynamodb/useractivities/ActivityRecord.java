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

import com.formkiq.aws.dynamodb.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.eventsourcing.entity.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing a activity with its DynamoDB key structure and metadata.
 */
public record ActivityRecord(DynamoDbKey key, String resource, UserActivityType type,
    UserActivityStatus status, String sourceIpAddress, String source, String userId,
    String entityTypeId, String entityId, String documentId, String message, Date insertedDate,
    String versionPk, String versionSk, Map<String, Object> changes) {

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
        .withString("documentId", documentId).withDate("inserteddate", insertedDate)
        .withMap("changes", changes).build();
  }

  /**
   * Creates a new {@link Builder} for {@link ActivityRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link ActivityRecord}.
   */
  public static class Builder implements DynamoDbEntityBuilder<ActivityRecord> {
    /** Activity Record Type. */
    private UserActivityType type;
    /** Activity Record Resource. */
    private String resource;
    /** Activity Record Status. */
    private UserActivityStatus status;
    /** Activity Record Source IP address. */
    private String sourceIpAddress;
    /** Activity Record Source. */
    private String source;
    /** Activity Record UserId. */
    private String userId;
    /** Activity Record Entity Type Id. */
    private String entityTypeId;
    /** Activity Record Entity Id. */
    private String entityId;
    /** Activity Record Document Id. */
    private String documentId;
    /** Activity Message. */
    private String message;
    /** Activity Inserted Date. */
    private Date insertedDate = new Date();
    /** Activity Changes. */
    private Map<String, Object> changes;
    /** Document Version Pk. */
    private String versionPk;
    /** Document Version Pk. */
    private String versionSk;

    /**
     * Sets the activityResource associated with the activity.
     *
     * @param activityResource the activityResource name
     * @return this Builder
     */
    public Builder resource(final String activityResource) {
      this.resource = activityResource;
      return this;
    }

    /**
     * Sets the activityType associated with the activity.
     *
     * @param activityType the resource name
     * @return this Builder
     */
    public Builder type(final UserActivityType activityType) {
      this.type = activityType;
      return this;
    }

    /**
     * Sets the Version PK associated with the activity.
     *
     * @param activityVersionPk Version PK
     * @return this Builder
     */
    public Builder versionPk(final String activityVersionPk) {
      this.versionPk = activityVersionPk;
      return this;
    }

    /**
     * Sets the Version SK associated with the activity.
     *
     * @param activityVersionSk Version PK
     * @return this Builder
     */
    public Builder versionSk(final String activityVersionSk) {
      this.versionSk = activityVersionSk;
      return this;
    }

    /**
     * Sets the activityStatus of the user activity.
     *
     * @param activityStatus the {@link UserActivityStatus}
     * @return this Builder
     */
    public Builder status(final UserActivityStatus activityStatus) {
      this.status = activityStatus;
      return this;
    }

    /**
     * Sets the source IP address.
     *
     * @param activitySourceIpAddress the IP address
     * @return this Builder
     */
    public Builder sourceIpAddress(final String activitySourceIpAddress) {
      this.sourceIpAddress = activitySourceIpAddress;
      return this;
    }

    /**
     * Sets the Activity Change Set.
     *
     * @param activityChanges {@link Map}
     * @return this Builder
     */
    public Builder changes(final Map<String, Object> activityChanges) {
      this.changes = activityChanges;
      return this;
    }

    /**
     * Sets the Activity activityMessage.
     *
     * @param activityMessage S3 Key
     * @return this Builder
     */
    public Builder message(final String activityMessage) {
      this.message = activityMessage;
      return this;
    }

    /**
     * Sets the activitySource system or application.
     *
     * @param activitySource the activitySource identifier
     * @return this Builder
     */
    public Builder source(final String activitySource) {
      this.source = activitySource;
      return this;
    }

    /**
     * Sets the user ID.
     *
     * @param activityUserId the user identifier
     * @return this Builder
     */
    public Builder userId(final String activityUserId) {
      this.userId = activityUserId;
      return this;
    }

    /**
     * Sets the entity type ID.
     *
     * @param activityEntityTypeId the entity type identifier
     * @return this Builder
     */
    public Builder entityTypeId(final String activityEntityTypeId) {
      this.entityTypeId = activityEntityTypeId;
      return this;
    }

    /**
     * Sets the entity ID.
     *
     * @param activityEntityId the unique identifier of the entity
     * @return this Builder
     */
    public Builder entityId(final String activityEntityId) {
      this.entityId = activityEntityId;
      return this;
    }

    /**
     * Sets the document ID.
     *
     * @param activityDocumentId the document identifier
     * @return this Builder
     */
    public Builder documentId(final String activityDocumentId) {
      this.documentId = activityDocumentId;
      return this;
    }

    /**
     * Sets the insertion date of the activity.
     *
     * @param activityInsertedDate the insertion date
     * @return this Builder
     */
    public Builder insertedDate(final Date activityInsertedDate) {
      this.insertedDate = new Date(activityInsertedDate.getTime());
      return this;
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {
      return switch (resource) {
        case "documents", "users" -> buildDocumentsKey(siteId);
        case "entities" -> buildEntitiesKey(siteId);
        case "entityTypes" -> buildEntityTypesKey(siteId);
        default -> throw new IllegalArgumentException("Invalid resource " + resource);
      };
    }

    private String getGsi2Pk() {
      return "activity#" + DateUtil.getYyyyMmDdFormatter().format(new Date());
    }

    private DynamoDbKey buildDocumentsKey(final String siteId) {

      Objects.requireNonNull(documentId, "documentId must not be null");
      Objects.requireNonNull(userId, "userId must not be null");

      String pk = "doc#" + documentId;
      String sk = "activity#" + DateUtil.getNowInIso8601Format() + "#" + documentId;
      String gsi1Pk = "activity#user#" + userId;

      String gsi2Pk = getGsi2Pk();

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(sk)
          .gsi2Pk(siteId, gsi2Pk).gsi2Sk(sk).build();
    }

    private DynamoDbKey buildEntitiesKey(final String siteId) {

      Objects.requireNonNull(entityTypeId, "entityTypeId must not be null");
      Objects.requireNonNull(entityId, "entityId must not be null");
      Objects.requireNonNull(userId, "userId must not be null");

      String pk = "entity#" + entityTypeId + "#" + entityId;
      String sk = "activity#" + DateUtil.getNowInIso8601Format() + "#" + entityId;
      String gsi1Pk = "activity#user#" + userId;

      String gsi2Pk = getGsi2Pk();

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(sk)
          .gsi2Pk(siteId, gsi2Pk).gsi2Sk(sk).build();
    }

    private DynamoDbKey buildEntityTypesKey(final String siteId) {

      Objects.requireNonNull(entityTypeId, "entityTypeId must not be null");
      Objects.requireNonNull(userId, "userId must not be null");

      String pk = "entityType#" + entityTypeId;
      String sk = "activity#" + DateUtil.getNowInIso8601Format() + "#" + entityTypeId;
      String gsi1Pk = "activity#user#" + userId;

      String gsi2Pk = getGsi2Pk();

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(sk)
          .gsi2Pk(siteId, gsi2Pk).gsi2Sk(sk).build();
    }

    /**
     * Builds the {@link ActivityRecord}.
     *
     * @param siteId {@link String}
     * @return a fully-initialized {@code ActivityRecord}
     * @throws NullPointerException if any required field is missing
     */
    public ActivityRecord build(final String siteId) {

      Objects.requireNonNull(resource, "resource must not be null");
      Objects.requireNonNull(status, "status must not be null");
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");

      DynamoDbKey key = buildKey(siteId);

      return new ActivityRecord(key, resource, type, status, sourceIpAddress, source, userId,
          entityTypeId, entityId, documentId, message, insertedDate, versionPk, versionSk, changes);
    }
  }
}
