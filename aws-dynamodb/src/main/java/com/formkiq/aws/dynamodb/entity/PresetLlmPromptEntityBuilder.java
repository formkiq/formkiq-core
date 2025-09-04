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
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.validation.ValidationBuilder;

import java.util.List;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Record representing an Preset LlmPrompt Entity, with its DynamoDB key structure and metadata.
 */
public class PresetLlmPromptEntityBuilder implements DynamoDbEntityBuilder<EntityRecord> {

  /** {@link EntityRecord.Builder}. */
  private EntityRecord.Builder builder = EntityRecord.builder();
  /** Entity Attributes. */
  private List<EntityAttribute> attributes;

  /**
   * Sets the document identifier.
   *
   * @param entityName the entity name
   * @return this Builder
   */
  public PresetLlmPromptEntityBuilder name(final String entityName) {
    builder = builder.name(entityName);
    return this;
  }

  /**
   * Sets the entity type identifier.
   *
   * @param entityTypeId the entity type ID
   * @return this Builder
   */
  public PresetLlmPromptEntityBuilder entityTypeId(final String entityTypeId) {
    builder = builder.entityTypeId(entityTypeId);
    return this;
  }

  /**
   * Sets the document identifier.
   *
   * @param entityDocumentId the document ID
   * @return this Builder
   */
  public PresetLlmPromptEntityBuilder documentId(final String entityDocumentId) {
    builder = builder.documentId(entityDocumentId);
    return this;
  }

  /**
   * Sets the UserPrompt.
   *
   * @param entityAttributes the entity attributes
   * @return this Builder
   */
  public PresetLlmPromptEntityBuilder attributes(final List<EntityAttribute> entityAttributes) {
    this.attributes = entityAttributes;
    return this;
  }

  @Override
  public DynamoDbKey buildKey(final String siteId) {
    return builder.buildKey(siteId);
  }

  @Override
  public EntityRecord build(final String siteId) {
    validate();
    return builder.attributes(attributes).build(siteId);
  }

  private void validate() {
    ValidationBuilder vb = new ValidationBuilder();

    Optional<EntityAttribute> userPrompt = notNull(attributes).stream()
        .filter(new EntityAttributeKeyPredicate("userPrompt")).findFirst();
    vb.isRequired("userPrompt", userPrompt);
    vb.check();
  }
}
