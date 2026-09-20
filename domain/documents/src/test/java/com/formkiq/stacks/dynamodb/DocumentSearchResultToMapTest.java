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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.documents.DocumentMetadata;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;

/** Unit tests for {@link DocumentSearchResultToMap}. */
class DocumentSearchResultToMapTest {

  @Test
  @SuppressWarnings("unchecked")
  void testApply01() {
    DocumentRecord document = DocumentRecord.builder().documentId("document1").userId("jsmith")
        .metadata(List.of(new DocumentMetadata("category", "person", null))).s3version("version1")
        .timeToLive("1000").build((String) null);
    DocumentAttributeRecord matchedAttribute = new DocumentAttributeRecord()
        .setDocument(document.document()).setKey("security").setStringValue("confidential")
        .setValueType(DocumentAttributeValueType.STRING).setUserId("jsmith");
    DocumentTag matchedTag =
        new DocumentTag(null, "category", "person", null, null, DocumentTagType.USERDEFINED);
    Map<String, Object> attributeFields =
        Map.of("playerId", Map.of("stringValues", List.of("123"), "valueType", "STRING"));
    DocumentSearchResult searchResult = new DocumentSearchResult(document, matchedAttribute,
        attributeFields, matchedTag, null, new DocumentSearchFolderIndex(true, "index1"), null);

    Map<String, Object> result = new DocumentSearchResultToMap().apply(searchResult);

    assertEquals("document1", result.get("documentId"));
    assertFalse(result.containsKey("documentRecord"));
    assertFalse(result.containsKey("s3version"));
    assertFalse(result.containsKey("TimeToLive"));
    assertEquals(Boolean.TRUE, result.get("folder"));
    assertEquals("index1", result.get("indexKey"));
    assertEquals(attributeFields, result.get("attributes"));

    Map<String, Object> attribute = (Map<String, Object>) result.get("matchedAttribute");
    assertEquals("security", attribute.get("key"));
    assertEquals("confidential", attribute.get("stringValue"));
    assertFalse(attribute.containsKey("documentId"));
    assertFalse(attribute.containsKey("valueType"));

    Map<String, Object> tag = (Map<String, Object>) result.get("matchedTag");
    assertEquals(Map.of("key", "category", "value", "person", "type", "USERDEFINED"), tag);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testApplyMatchedTags01() {
    DocumentTag matchedTag =
        new DocumentTag(null, "category", "person", null, null, DocumentTagType.USERDEFINED);
    DocumentSearchResult searchResult =
        new DocumentSearchResult(null, null, null, matchedTag, List.of(matchedTag), null, null);

    Map<String, Object> result = new DocumentSearchResultToMap().apply(searchResult);

    assertFalse(result.containsKey("matchedTag"));
    List<Map<String, Object>> tags = (List<Map<String, Object>>) result.get("matchedTags");
    assertEquals(1, tags.size());
    assertEquals("category", tags.getFirst().get("key"));
  }

  @Test
  void testApplyValue01() {
    Map<String, Object> result =
        new DocumentSearchResultToMap().apply(new DocumentSearchResult("category"));

    assertEquals(Map.of("value", "category"), result);
  }
}
