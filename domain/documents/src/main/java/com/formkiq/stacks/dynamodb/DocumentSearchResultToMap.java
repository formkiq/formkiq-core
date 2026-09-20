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
package com.formkiq.stacks.dynamodb;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.model.DocumentTag;

/**
 * Converts a {@link DocumentSearchResult} to the public search-result response shape.
 */
public class DocumentSearchResultToMap
    implements Function<DocumentSearchResult, Map<String, Object>> {

  /** Fields exposed for a matched document attribute. */
  private static final Set<String> MATCHED_ATTRIBUTE_FIELDS =
      Set.of("booleanValue", "dateValue", "key", "numberValue", "stringValue");

  /** Converts DynamoDB values to their API representations. */
  private final AttributeValueToMap attributeValueToMap = new AttributeValueToMap();
  /** Converts the document portion of a search result. */
  private final DocumentRecordSetToMap documentRecordToMap = new DocumentRecordSetToMap();

  private void addFolderIndex(final Map<String, Object> result,
      final DocumentSearchFolderIndex folderIndex) {

    if (folderIndex != null) {
      if (folderIndex.folder()) {
        result.put("folder", Boolean.TRUE);
      }
      if (folderIndex.indexKey() != null) {
        result.put("indexKey", folderIndex.indexKey());
      }
    }
  }

  @Override
  public Map<String, Object> apply(final DocumentSearchResult searchResult) {

    Map<String, Object> result = searchResult.documentRecord() != null
        ? this.documentRecordToMap.apply(searchResult.documentRecord())
        : new HashMap<>();
    result.remove("TimeToLive");

    if (searchResult.matchedAttribute() != null) {
      result.put("matchedAttribute", toMatchedAttribute(searchResult.matchedAttribute()));
    }

    if (searchResult.attributeFields() != null) {
      result.put("attributes", searchResult.attributeFields());
    }

    if (searchResult.matchedTags() != null && !searchResult.matchedTags().isEmpty()) {
      result.put("matchedTags",
          searchResult.matchedTags().stream().map(this::toMatchedTag).toList());
    } else if (searchResult.matchedTag() != null) {
      result.put("matchedTag", toMatchedTag(searchResult.matchedTag()));
    }

    addFolderIndex(result, searchResult.folderIndex());

    if (searchResult.value() != null) {
      result.put("value", searchResult.value());
    }

    return result;
  }

  private Map<String, Object> toMatchedAttribute(final DocumentAttributeRecord attribute) {

    Map<String, Object> attributes = this.attributeValueToMap.apply(attribute.getDataAttributes());
    Map<String, Object> result = new HashMap<>();
    MATCHED_ATTRIBUTE_FIELDS.stream().filter(attributes::containsKey)
        .forEach(key -> result.put(key, attributes.get(key)));
    return result;
  }

  private Map<String, Object> toMatchedTag(final DocumentTag tag) {

    Map<String, Object> result = new HashMap<>();
    result.put("key", tag.getKey());
    result.put("value", tag.getValue());
    result.put("type", tag.getType() != null ? tag.getType().name() : null);
    return result;
  }
}
