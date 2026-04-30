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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test {@link ActivityRecordBuilder}.
 */
public class ActivityRecordBuilderTest {

  /** User Id. */
  private static final String USER_ID = "user1";
  /** Site Id. */
  private static final String SITE_ID = "site1";
  /** Inserted Date. */
  private static final Instant INSERTED_DATE = Instant.parse("2024-01-02T03:04:05Z");
  /** Inserted Date String. */
  private static final String INSERTED_DATE_STRING = "2024-01-02T03:04:05Z";
  /** Inserted Date Partition. */
  private static final String INSERTED_DATE_PARTITION = "2024-01-02";

  private void assertBuildKey(final String resource, final Map<String, Object> resourceIds,
      final String expectedPk, final String expectedSkIdPart) {

    for (String siteId : Arrays.asList(null, SITE_ID)) {
      ActivityRecordBuilder builder = new ActivityRecordBuilder("", "").resource(resource)
          .resourceIds(resourceIds).userId(USER_ID).insertedDate(INSERTED_DATE);

      DynamoDbShardKey shardKey = builder.buildKey(siteId);
      DynamoDbKey key = shardKey.key();

      assertKeyEquals(siteId, expectedPk, key.pk());
      assertTrue(key.sk().startsWith("activity#" + INSERTED_DATE_STRING + "#"));
      assertTrue(key.sk().contains("#" + expectedSkIdPart + "#"));

      assertKeyEquals(siteId, "activity#user#" + USER_ID, key.gsi1Pk());
      assertEquals(key.sk(), key.gsi1Sk());

      assertKeyEquals(siteId, "activity#" + INSERTED_DATE_PARTITION, key.gsi2Pk());
      assertEquals(key.sk(), key.gsi2Sk());
    }
  }

  private void assertKeyEquals(final String siteId, final String expectedKey, final String gotKey) {
    assertEquals(siteId != null ? siteId + "/" + expectedKey : expectedKey, gotKey);
  }

  @Test
  void testBuildKeyActivityTypes() {
    String type = UserActivityType.SSO_TOKEN_GRANT.name();
    assertBuildKey("activityTypes", Map.of("type", type), "activityTypes#" + type, type);
  }

  @Test
  void testBuildKeyApiKeys() {
    String apiKey = "api-key-1";
    assertBuildKey("apikeys", Map.of("apiKey", apiKey), "apikeys#" + apiKey, apiKey);
  }

  @Test
  void testBuildKeyAttributes() {
    String attributeKey = "status";
    assertBuildKey("attributes", Map.of("attributeKey", attributeKey), "attributes#" + attributeKey,
        attributeKey);
  }

  @Test
  void testBuildKeyClassifications() {
    String classificationId = "classification-1";
    assertBuildKey("classifications", Map.of("classificationId", classificationId),
        "classifications#" + classificationId, classificationId);
  }

  @Test
  void testBuildKeyControlPolicyOpa() {
    String controlPolicy = "opa-1";
    assertBuildKey("controlpolicy#opa", Map.of("controlPolicy", controlPolicy),
        "controlpolicy#" + controlPolicy, controlPolicy);
  }

  @Test
  void testBuildKeyDocumentsAliases() {
    String documentId = "doc-1";
    Map<String, Object> resourceIds = Map.of("documentId", documentId);

    assertBuildKey("documents", resourceIds, "doc#" + documentId, documentId);
    assertBuildKey("users", resourceIds, "doc#" + documentId, documentId);
    assertBuildKey("documentAttributes", resourceIds, "doc#" + documentId, documentId);
  }

  @Test
  void testBuildKeyDocumentsWithArtifactId() {
    String documentId = "doc-1";
    String artifactId = "artifact-1";

    assertBuildKey("documents", Map.of("documentId", documentId, "artifactId", artifactId),
        "doc#" + documentId + "#art#" + artifactId, documentId);
  }

  @Test
  void testBuildKeyEntities() {
    String entityId = "entity-1";
    String entityTypeId = "entity-type-1";
    assertBuildKey("entities", Map.of("entityId", entityId, "entityTypeId", entityTypeId),
        "entity#" + entityTypeId + "#" + entityId, entityId);
  }

  @Test
  void testBuildKeyEntityTypes() {
    String entityTypeId = "entity-type-1";
    assertBuildKey("entityTypes", Map.of("entityTypeId", entityTypeId),
        "entityType#" + entityTypeId, entityTypeId);
  }

  @Test
  void testBuildKeyInvalidResource() {
    ActivityRecordBuilder builder = new ActivityRecordBuilder("", "").resource("invalid")
        .resourceIds(Map.of()).userId(USER_ID).insertedDate(INSERTED_DATE);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> builder.buildKey(null));

    assertEquals("Invalid resource invalid", e.getMessage());
  }

  @Test
  void testBuildKeyMappings() {
    String mappingId = "ssoTokenGrant";
    assertBuildKey("mappings", Map.of("mappingId", mappingId), "mappings#" + mappingId, mappingId);
  }

  @Test
  void testBuildKeyRulesetRules() {
    String rulesetId = "ruleset-1";
    String ruleId = "rule-1";
    assertBuildKey("rulesetRules", Map.of("rulesetId", rulesetId, "ruleId", ruleId),
        "rulesetRules#" + rulesetId + "#" + ruleId, ruleId);
  }

  @Test
  void testBuildKeyRulesets() {
    String rulesetId = "ruleset-1";
    assertBuildKey("rulesets", Map.of("rulesetId", rulesetId), "rulesets#" + rulesetId, rulesetId);
  }

  @Test
  void testBuildKeySchemas() {
    String schema = "invoice";
    assertBuildKey("schemas", Map.of("schema", schema), "schemas#" + schema, schema);
  }

  @Test
  void testBuildKeyWorkflows() {
    String workflowId = "workflow-1";
    assertBuildKey("workflows", Map.of("workflowId", workflowId), "workflows#" + workflowId,
        workflowId);
  }

  @Test
  void testBuildSsoTokenGrant() {
    String mappingId = "mapping-1";
    assertBuildKey("ssoTokenGrant", Map.of(), "globalActivity#ssoTokenGrant", "ssoTokenGrant");
  }
}
