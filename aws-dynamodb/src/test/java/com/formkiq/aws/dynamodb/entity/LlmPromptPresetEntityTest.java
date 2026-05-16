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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.validation.ValidationException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit Tests for {@link LlmPromptPresetEntity}. */
class LlmPromptPresetEntityTest {

  private DynamoDbService dbWithDocuments(
      final Map<DynamoDbKey, Map<String, AttributeValue>> docs) {
    return (DynamoDbService) Proxy.newProxyInstance(DynamoDbService.class.getClassLoader(),
        new Class<?>[] {DynamoDbService.class}, (proxy, method, args) -> {
          if ("get".equals(method.getName()) && args.length == 1
              && args[0] instanceof DynamoDbKey key) {
            return docs.getOrDefault(key, Map.of());
          }

          throw new UnsupportedOperationException(method.getName());
        });
  }

  private DynamoDbService dbWithDocuments(final String siteId, final String documentId,
      final String artifactId, final String contentType) {
    DocumentRecord record = DocumentRecord.builder().documentId(documentId).artifactId(artifactId)
        .contentType(contentType).build(siteId);
    return dbWithDocuments(Map.of(record.key(), record.getAttributes()));
  }

  /**
   * Get LLM Prompt attribute keys.
   */
  @Test
  void testGetAttributeKeys01() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();

    // when
    List<String> keys = entity.getAttributeKeys();

    // then
    assertEquals(List.of("UserPrompt", "LlmSystemPrompt", "LlmSystemPromptDocumentId",
        "LlmSystemPromptArtifactId", "LlmResponsePresetEntityTypes", "LlmResponseFieldKeys",
        "LlmAnalysisCategory"), keys);
  }

  /**
   * LLM Prompt attribute keys are reserved string attributes.
   */
  @Test
  void testReservedAttributeKeys01() {
    // given
    List<String> keys = new LlmPromptPresetEntity().getAttributeKeys().stream().skip(1).toList();

    // when / then
    for (String key : keys) {
      AttributeKeyReserved reserved = AttributeKeyReserved.find(key.toLowerCase());
      assertNotNull(reserved);
      assertEquals(key, reserved.getKey());
      assertEquals(AttributeDataType.STRING, reserved.getDataType());
    }
  }

  /**
   * LLM Prompt can use an inline system prompt.
   */
  @Test
  void testValidateAttributes01() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();
    List<EntityAttribute> attributes =
        List.of(EntityAttribute.builder().key("LlmSystemPrompt").addStringValue("system").build());

    // when / then
    entity.validateAttributes(null, null, attributes, null);
  }

  /**
   * LLM Prompt can use a document for the system prompt.
   */
  @Test
  void testValidateAttributes02() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();
    String siteId = "site1";
    String documentId = "doc123";
    List<EntityAttribute> attributes = List.of(EntityAttribute.builder()
        .key("LlmSystemPromptDocumentId").addStringValue(documentId).build());
    DynamoDbService db = dbWithDocuments(siteId, documentId, null, "text/plain");

    // when / then
    entity.validateAttributes(db, siteId, attributes, null);
  }

  /**
   * LLM Prompt document artifact requires a document id.
   */
  @Test
  void testValidateAttributes03() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();
    List<EntityAttribute> attributes = List.of(EntityAttribute.builder()
        .key("LlmSystemPromptArtifactId").addStringValue("artifact123").build());

    // when
    ValidationException exception = assertThrows(ValidationException.class,
        () -> entity.validateAttributes(null, null, attributes, null));

    // then
    assertEquals("'LlmSystemPrompt' or 'LlmSystemPromptDocumentId' is required",
        exception.getMessage());
  }

  /**
   * LLM Prompt can use existing system prompt document id during updates.
   */
  @Test
  void testValidateAttributes04() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();
    String siteId = "site1";
    String documentId = "doc123";
    String artifactId = "artifact123";
    List<EntityAttribute> attributes = List.of(EntityAttribute.builder()
        .key("LlmSystemPromptArtifactId").addStringValue(artifactId).build());
    Map<String, AttributeValue> existing =
        Map.of("LlmSystemPromptDocumentId", AttributeValue.fromS(documentId));
    DynamoDbService db = dbWithDocuments(siteId, documentId, artifactId, "text/plain");

    // when / then
    entity.validateAttributes(db, siteId, attributes, existing);
  }

  /**
   * LLM Prompt requires either an inline or document backed system prompt unless user prompt
   * exists.
   */
  @Test
  void testValidateAttributes05() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();

    // when
    ValidationException exception = assertThrows(ValidationException.class,
        () -> entity.validateAttributes(null, null, List.of(), null));

    // then
    assertEquals("'LlmSystemPrompt' or 'LlmSystemPromptDocumentId' is required",
        exception.getMessage());
  }

  /**
   * LLM Prompt document must exist.
   */
  @Test
  void testValidateAttributes06() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();
    List<EntityAttribute> attributes = List.of(EntityAttribute.builder()
        .key("LlmSystemPromptDocumentId").addStringValue("doc123").build());
    DynamoDbService db = dbWithDocuments(Map.of());

    // when
    ValidationException exception = assertThrows(ValidationException.class,
        () -> entity.validateAttributes(db, "site1", attributes, null));

    // then
    assertEquals("Document 'doc123' not found", exception.getMessage());
  }

  /**
   * LLM Prompt document must be plain text.
   */
  @Test
  void testValidateAttributes07() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();
    String siteId = "site1";
    String documentId = "doc123";
    List<EntityAttribute> attributes = List.of(EntityAttribute.builder()
        .key("LlmSystemPromptDocumentId").addStringValue(documentId).build());
    DynamoDbService db = dbWithDocuments(siteId, documentId, null, "application/pdf");

    // when
    ValidationException exception = assertThrows(ValidationException.class,
        () -> entity.validateAttributes(db, siteId, attributes, null));

    // then
    assertEquals("Document 'doc123' content type must be plain text", exception.getMessage());
  }

  /**
   * LLM Prompt document artifact must exist.
   */
  @Test
  void testValidateAttributes08() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();
    List<EntityAttribute> attributes = List.of(
        EntityAttribute.builder().key("LlmSystemPromptDocumentId").addStringValue("doc123").build(),
        EntityAttribute.builder().key("LlmSystemPromptArtifactId").addStringValue("artifact123")
            .build());
    DynamoDbService db = dbWithDocuments(Map.of());

    // when
    ValidationException exception = assertThrows(ValidationException.class,
        () -> entity.validateAttributes(db, "site1", attributes, null));

    // then
    assertEquals("Document 'doc123' artifact 'artifact123' not found", exception.getMessage());
  }

  /**
   * LLM Prompt validation uses the builder site id when checking documents.
   */
  @Test
  void testValidateAttributes09() {
    // given
    String siteId = "site1";
    String documentId = "doc123";
    List<EntityAttribute> attributes = List.of(EntityAttribute.builder()
        .key("LlmSystemPromptDocumentId").addStringValue(documentId).build());
    DynamoDbService db = dbWithDocuments(siteId, documentId, null, "text/plain");

    // when / then
    new PresetEntityBuilder(db).presetEntity(new LlmPromptPresetEntity()).documentId("entity123")
        .entityTypeId("type123").name("preset").attributes(attributes).build(siteId);
  }
}
