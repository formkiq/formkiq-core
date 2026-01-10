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
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.builder.DynamoDbShardEntityBuilder;
import com.formkiq.aws.dynamodb.objects.DateUtil;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for {@link ActivityRecordBuilder}.
 */
public class ActivityRecordBuilder implements DynamoDbShardEntityBuilder<ActivityRecord> {

  /** Number of Shards - total of 4 shards, 0,1,2,3. */
  public static final int SHARD_COUNT = 7;

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
  /** Activity Record Schema. */
  private String schema;
  /** Activity Record ClassificationId. */
  private String classificationId;
  /** Activity Record MappingId. */
  private String mappingId;
  /** Activity Record ApiKey. */
  private String apiKey;
  /** Activity Record Entity Type Id. */
  private String entityTypeId;
  /** Activity Record Entity Id. */
  private String entityId;
  /** Activity Record Document Id. */
  private String documentId;
  /** Activity Record Workflow Id. */
  private String workflowId;
  /** Activity Record Ruleset Id. */
  private String rulesetId;
  /** Activity Record Attribute Key. */
  private String attributeKey;
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
  /** Pk GSI1 Shard. */
  private final String pkGsi1Shard;
  /** Pk GSI1 Shard. */
  private final String pkGsi2Shard;
  /** Rule Id. */
  private String ruleId;
  /** Control Policy. */
  private String controlPolicy;

  /**
   * constructor.
   *
   * @param gsi1Shard {@link String}
   * @param gsi2Shard {@link String}
   */
  public ActivityRecordBuilder(final String gsi1Shard, final String gsi2Shard) {
    pkGsi1Shard = gsi1Shard != null ? gsi1Shard : DynamoDbShardKey.getShardKey(SHARD_COUNT);
    pkGsi2Shard = gsi2Shard != null ? gsi2Shard : DynamoDbShardKey.getShardKey(SHARD_COUNT);
  }

  /**
   * Sets the Attribute Key.
   *
   * @param activityAttributeKey the attribute key
   * @return this Builder
   */
  public ActivityRecordBuilder attributeKey(final String activityAttributeKey) {
    this.attributeKey = activityAttributeKey;
    return this;
  }

  /**
   * Builds the {@link ActivityRecordBuilder}.
   *
   * @param siteId {@link String}
   * @return a fully-initialized {@code ActivityRecord}
   * @throws NullPointerException if any required field is missing
   */
  public ActivityRecord build(final String siteId) {

    Objects.requireNonNull(resource, "resource must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");

    DynamoDbShardKey key = buildKey(siteId);

    return new ActivityRecord(key, resource, type, status, sourceIpAddress, source, userId, schema,
        classificationId, mappingId, rulesetId, ruleId, entityTypeId, entityId, documentId,
        workflowId, attributeKey, apiKey, controlPolicy, message, insertedDate, versionPk,
        versionSk, changes);
  }

  private DynamoDbShardKey buildActivityKey(final String siteId, final String pk,
      final String skIdPart) {
    Objects.requireNonNull(pk, "pk must not be null");
    Objects.requireNonNull(skIdPart, "skIdPart must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    String sk = "activity#" + getSkDate() + "#" + skIdPart + "#" + ID.ulid();
    String gsi1Pk = "activity#user#" + userId;
    String gsi2Pk = getGsi2Pk();

    DynamoDbKey key = DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(sk)
        .gsi2Pk(siteId, gsi2Pk).gsi2Sk(sk).build();

    return DynamoDbShardKey.builder().key(key).pkGsi1Shard(pkGsi1Shard).pkGsi2Shard(pkGsi2Shard)
        .build();
  }

  private DynamoDbShardKey buildApiKeys(final String siteId) {

    Objects.requireNonNull(apiKey, "apikeys must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "apikeys#" + apiKey, apiKey);
  }

  private DynamoDbShardKey buildAttributes(final String siteId) {

    Objects.requireNonNull(attributeKey, "attributeKey must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "attributes#" + attributeKey, attributeKey);
  }

  private DynamoDbShardKey buildClassifications(final String siteId) {

    Objects.requireNonNull(classificationId, "classificationId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "classifications#" + classificationId, classificationId);
  }

  private DynamoDbShardKey buildControlPolicyOpa(final String siteId) {

    Objects.requireNonNull(controlPolicy, "controlPolicy must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "controlpolicy#" + controlPolicy, controlPolicy);
  }

  private DynamoDbShardKey buildDocumentsKey(final String siteId) {

    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    return buildActivityKey(siteId, "doc#" + documentId, documentId);
  }

  private DynamoDbShardKey buildEntitiesKey(final String siteId) {

    Objects.requireNonNull(entityTypeId, "entityTypeId must not be null");
    Objects.requireNonNull(entityId, "entityId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "entity#" + entityTypeId + "#" + entityId, entityId);
  }

  private DynamoDbShardKey buildEntityTypesKey(final String siteId) {

    Objects.requireNonNull(entityTypeId, "entityTypeId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    return buildActivityKey(siteId, "entityType#" + entityTypeId, entityTypeId);
  }

  @Override
  @SuppressWarnings("CyclomaticComplexity")
  public DynamoDbShardKey buildKey(final String siteId) {
    return switch (resource) {
      case "documents", "users", "documentAttributes" -> buildDocumentsKey(siteId);
      case "rulesets" -> buildRulesetsKey(siteId);
      case "rulesetRules" -> buildRulesKey(siteId);
      case "workflows" -> buildWorkflowsKey(siteId);
      case "entities" -> buildEntitiesKey(siteId);
      case "entityTypes" -> buildEntityTypesKey(siteId);
      case "attributes" -> buildAttributes(siteId);
      case "schemas" -> buildSchemas(siteId);
      case "classifications" -> buildClassifications(siteId);
      case "mappings" -> buildMappings(siteId);
      case "apikeys" -> buildApiKeys(siteId);
      case "controlpolicy#opa" -> buildControlPolicyOpa(siteId);
      default -> throw new IllegalArgumentException("Invalid resource " + resource);
    };
  }

  private DynamoDbShardKey buildMappings(final String siteId) {

    Objects.requireNonNull(mappingId, "mappingId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "mappings#" + mappingId, mappingId);
  }

  private DynamoDbShardKey buildRulesKey(final String siteId) {

    Objects.requireNonNull(rulesetId, "rulesetId must not be null");
    Objects.requireNonNull(ruleId, "ruleId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "rulesetRules#" + rulesetId + "#" + ruleId, ruleId);
  }

  private DynamoDbShardKey buildRulesetsKey(final String siteId) {

    Objects.requireNonNull(rulesetId, "rulesetId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "rulesets#" + rulesetId, rulesetId);
  }

  private DynamoDbShardKey buildSchemas(final String siteId) {

    Objects.requireNonNull(schema, "schema must not be null");
    Objects.requireNonNull(userId, "userId must not be null");

    return buildActivityKey(siteId, "schemas#" + schema, schema);
  }

  private DynamoDbShardKey buildWorkflowsKey(final String siteId) {

    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    return buildActivityKey(siteId, "workflows#" + workflowId, workflowId);
  }

  /**
   * Sets the Activity Change Set.
   *
   * @param activityChanges {@link Map}
   * @return this Builder
   */
  public ActivityRecordBuilder changes(final Map<String, Object> activityChanges) {
    this.changes = activityChanges;
    return this;
  }

  private String getGsi2Pk() {
    return "activity#" + DateUtil.getYyyyMmDdFormatter().format(insertedDate);
  }

  private String getSkDate() {
    return DateTimeFormatter.ISO_INSTANT
        .format(insertedDate.toInstant().truncatedTo(ChronoUnit.SECONDS));
  }

  /**
   * Sets the insertion date of the activity.
   *
   * @param activityInsertedDate the insertion date
   * @return this Builder
   */
  public ActivityRecordBuilder insertedDate(final Date activityInsertedDate) {
    this.insertedDate = new Date(activityInsertedDate.getTime());
    return this;
  }

  /**
   * Sets the insertion date of the activity.
   *
   * @param activityInsertedDate the insertion date
   * @return this Builder
   */
  public ActivityRecordBuilder insertedDate(final Instant activityInsertedDate) {
    this.insertedDate = Date.from(activityInsertedDate);
    return this;
  }

  /**
   * Sets the Activity activityMessage.
   *
   * @param activityMessage S3 Key
   * @return this Builder
   */
  public ActivityRecordBuilder message(final String activityMessage) {
    this.message = activityMessage;
    return this;
  }

  /**
   * Sets the activityResource associated with the activity.
   *
   * @param activityResource the activityResource name
   * @return this Builder
   */
  public ActivityRecordBuilder resource(final String activityResource) {
    this.resource = activityResource;
    return this;
  }

  /**
   * Set Resource Ids.
   *
   * @param resourceIds {@link Map}
   * @return this Builder
   */
  public ActivityRecordBuilder resourceIds(final Map<String, Object> resourceIds) {

    workflowId = (String) resourceIds.get("workflowId");
    documentId = (String) resourceIds.get("documentId");
    entityId = (String) resourceIds.get("entityId");
    mappingId = (String) resourceIds.get("mappingId");
    schema = (String) resourceIds.get("schema");
    classificationId = (String) resourceIds.get("classificationId");
    rulesetId = (String) resourceIds.get("rulesetId");
    ruleId = (String) resourceIds.get("ruleId");
    entityTypeId = (String) resourceIds.get("entityTypeId");
    attributeKey = (String) resourceIds.get("attributeKey");
    apiKey = (String) resourceIds.get("apiKey");
    controlPolicy = (String) resourceIds.get("controlPolicy");

    return this;
  }

  /**
   * Sets the activitySource system or application.
   *
   * @param activitySource the activitySource identifier
   * @return this Builder
   */
  public ActivityRecordBuilder source(final String activitySource) {
    this.source = activitySource;
    return this;
  }

  /**
   * Sets the source IP address.
   *
   * @param activitySourceIpAddress the IP address
   * @return this Builder
   */
  public ActivityRecordBuilder sourceIpAddress(final String activitySourceIpAddress) {
    this.sourceIpAddress = activitySourceIpAddress;
    return this;
  }

  /**
   * Sets the activityStatus of the user activity.
   *
   * @param activityStatus the {@link UserActivityStatus}
   * @return this Builder
   */
  public ActivityRecordBuilder status(final UserActivityStatus activityStatus) {
    this.status = activityStatus;
    return this;
  }

  /**
   * Sets the activityType associated with the activity.
   *
   * @param activityType the resource name
   * @return this Builder
   */
  public ActivityRecordBuilder type(final UserActivityType activityType) {
    this.type = activityType;
    return this;
  }

  /**
   * Sets the user ID.
   *
   * @param activityUserId the user identifier
   * @return this Builder
   */
  public ActivityRecordBuilder userId(final String activityUserId) {
    this.userId = activityUserId;
    return this;
  }

  /**
   * Sets the Version PK associated with the activity.
   *
   * @param activityVersionPk Version PK
   * @return this Builder
   */
  public ActivityRecordBuilder versionPk(final String activityVersionPk) {
    this.versionPk = activityVersionPk;
    return this;
  }

  /**
   * Sets the Version SK associated with the activity.
   *
   * @param activityVersionSk Version PK
   * @return this Builder
   */
  public ActivityRecordBuilder versionSk(final String activityVersionSk) {
    this.versionSk = activityVersionSk;
    return this;
  }
}
