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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.FindDocumentById;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.validation.ValidationBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_ANALYSIS_CATEGORY;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_RESPONSE_FIELD_KEY;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_RESPONSE_PRESET_ENTITY_TYPES;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_SYSTEM_PROMPT_ARTIFACT_ID;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_SYSTEM_PROMPT_DOCUMENT_ID;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_SYSTEM_PROMPT;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_USER_PROMPT;

/**
 * PresetEntity for LLM Prompt.
 */
public class LlmPromptPresetEntity implements PresetEntity {

  /** Entity Name. */
  public static final String ENTITY_NAME = "LlmPrompt";

  @Override
  public List<String> getAttributeKeys() {
    return List.of(LLM_USER_PROMPT.getKey(), LLM_SYSTEM_PROMPT.getKey(),
        LLM_SYSTEM_PROMPT_DOCUMENT_ID.getKey(), LLM_SYSTEM_PROMPT_ARTIFACT_ID.getKey(),
        LLM_RESPONSE_PRESET_ENTITY_TYPES.getKey(), LLM_RESPONSE_FIELD_KEY.getKey(),
        LLM_ANALYSIS_CATEGORY.getKey());
  }

  @Override
  public String getName() {
    return ENTITY_NAME;
  }

  @Override
  public List<String> getRequiredAttributeKeys() {
    return List.of();
  }

  private String getStringValue(final List<EntityAttribute> attributes,
      final Map<String, AttributeValue> existing, final String attributeKey) {

    Optional<EntityAttribute> attribute = findAttribute(attributes, attributeKey);
    if (attribute.isPresent()) {
      List<String> values = attribute.get().getStringValues();
      return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    AttributeValue existingValue = existing != null ? existing.get(attributeKey) : null;
    return DynamoDbTypes.toString(existingValue);
  }

  private boolean hasValue(final String value) {
    return value != null && !value.isBlank();
  }

  @Override
  public void validateAttributes(final DynamoDbService db, final String siteId,
      final List<EntityAttribute> attributes, final Map<String, AttributeValue> existing) {

    ValidationBuilder vb = new ValidationBuilder();
    boolean hasSystemPrompt = hasAttribute(attributes, existing, LLM_SYSTEM_PROMPT.getKey());
    boolean hasUserPrompt = hasAttribute(attributes, existing, LLM_USER_PROMPT.getKey());
    boolean hasDocument =
        hasAttribute(attributes, existing, LLM_SYSTEM_PROMPT_DOCUMENT_ID.getKey());

    if (!hasSystemPrompt && !hasUserPrompt && !hasDocument) {
      vb.addError(null, "'LlmSystemPrompt' or 'LlmSystemPromptDocumentId' is required");
    } else if (hasDocument) {
      String documentId =
          getStringValue(attributes, existing, LLM_SYSTEM_PROMPT_DOCUMENT_ID.getKey());
      String artifactId =
          getStringValue(attributes, existing, LLM_SYSTEM_PROMPT_ARTIFACT_ID.getKey());
      validateDocument(db, siteId, vb, documentId, artifactId);
    }


    vb.check();
  }

  private void validateDocument(final DynamoDbService db, final String siteId,
      final ValidationBuilder vb, final String documentId, final String artifactId) {

    if (hasValue(documentId)) {

      DocumentRecord document = new FindDocumentById().find(db, null, siteId,
          DocumentArtifact.of(documentId, artifactId));

      if (document == null) {
        String artifact = hasValue(artifactId) ? " artifact '" + artifactId + "'" : "";
        vb.addError(LLM_SYSTEM_PROMPT_DOCUMENT_ID.getKey(),
            "Document '" + documentId + "'" + artifact + " not found");
      } else if (!MimeType.isPlainText(document.contentType())) {
        String artifact = hasValue(artifactId) ? " artifact '" + artifactId + "'" : "";
        vb.addError(LLM_SYSTEM_PROMPT_DOCUMENT_ID.getKey(),
            "Document '" + documentId + "'" + artifact + " content type must be plain text");
      }
    }
  }
}
