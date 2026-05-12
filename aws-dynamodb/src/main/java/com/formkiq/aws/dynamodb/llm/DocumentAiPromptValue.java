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
package com.formkiq.aws.dynamodb.llm;

import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Structured result extracted from an LLM prompt response.
 *
 * @param resultType {@link DocumentAiPromptResultType}
 * @param entityType optional entity type when {@code resultType} is {@code ENTITY}
 * @param entityNamespace optional entity namespace when {@code resultType} is {@code ENTITY}
 * @param attributes extracted key/value attributes
 */
public record DocumentAiPromptValue(DocumentAiPromptResultType resultType, String entityType,
    EntityTypeNamespace entityNamespace, List<DocumentAiPromptResultAttribute> attributes) {

  /**
   * Canonical constructor.
   */
  public DocumentAiPromptValue {
    Objects.requireNonNull(resultType, "resultType must not be null");
    Objects.requireNonNull(attributes, "attributes must not be null");
    if (resultType == DocumentAiPromptResultType.ENTITY) {
      Objects.requireNonNull(entityType, "entityType must not be null for ENTITY results");
      Objects.requireNonNull(entityNamespace,
          "entityNamespace must not be null for ENTITY results");
    } else if (entityType != null || entityNamespace != null) {
      throw new IllegalArgumentException(
          "entityType and entityNamespace are only supported for ENTITY results");
    }
    attributes = List.copyOf(attributes);
  }

  /**
   * Creates an entity result.
   *
   * @param entityType {@link String}
   * @param entityNamespace {@link EntityTypeNamespace}
   * @param attributes {@link List} {@link DocumentAiPromptResultAttribute}
   * @return {@link DocumentAiPromptValue}
   */
  public static DocumentAiPromptValue entity(final String entityType,
      final EntityTypeNamespace entityNamespace,
      final List<DocumentAiPromptResultAttribute> attributes) {
    Objects.requireNonNull(entityType, "entityType must not be null");
    Objects.requireNonNull(entityNamespace, "entityNamespace must not be null");
    return new DocumentAiPromptValue(DocumentAiPromptResultType.ENTITY, entityType, entityNamespace,
        attributes);
  }

  /**
   * Creates a {@link DocumentAiPromptValue} from a map.
   *
   * @param map {@link Map}
   * @return {@link DocumentAiPromptValue}
   */
  public static DocumentAiPromptValue fromMap(final Map<String, Object> map) {
    Objects.requireNonNull(map, "map must not be null");
    var resultType = DocumentAiPromptResultType.fromString((String) map.get("resultType"));
    var entityType = (String) map.get("entityType");
    var entityNamespace = map.get("entityNamespace") != null
        ? EntityTypeNamespace.fromString(String.valueOf(map.get("entityNamespace")))
        : null;

    var attributes = ((List<Map<String, Object>>) map.getOrDefault("attributes", List.of()))
        .stream().map(DocumentAiPromptResultAttribute::fromMap).toList();

    return new DocumentAiPromptValue(resultType, entityType, entityNamespace, attributes);
  }

  /**
   * Creates a key/value result.
   *
   * @param attributes {@link List} {@link DocumentAiPromptResultAttribute}
   * @return {@link DocumentAiPromptValue}
   */
  public static DocumentAiPromptValue keyValue(
      final List<DocumentAiPromptResultAttribute> attributes) {
    return new DocumentAiPromptValue(DocumentAiPromptResultType.KEY_VALUE, null, null, attributes);
  }

  /**
   * Converts this result to a map for DynamoDB persistence.
   *
   * @return {@link Map}
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("resultType", resultType.name());

    if (entityType != null) {
      map.put("entityType", entityType);
    }

    if (entityNamespace != null) {
      map.put("entityNamespace", entityNamespace.name());
    }

    map.put("attributes", attributes.stream().map(DocumentAiPromptResultAttribute::toMap).toList());
    return map;
  }
}
