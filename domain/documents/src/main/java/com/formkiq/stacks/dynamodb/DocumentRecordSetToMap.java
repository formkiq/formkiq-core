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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.AttributeValueToMapConfig;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * {@link Function} to convert a {@link DocumentRecordSet} to its public document response map.
 */
public class DocumentRecordSetToMap implements Function<DocumentRecordSet, Map<String, Object>> {

  /** Document fields exposed by the API. */
  private static final Set<String> DOCUMENT_FIELDS = Set.of("artifactCategory", "artifactId",
      "belongsToDocumentId", "checksum", "checksumType", "contentLength", "contentType",
      "deepLinkPath", "documentId", "hasArtifacts", "height", "insertedDate", "lastModifiedDate",
      "path", "promotedArtifactId", "resourceType", "TimeToLive", "userId", "width");

  /** Converts DynamoDB values to their API representations. */
  private final AttributeValueToMap toMap =
      new AttributeValueToMap(AttributeValueToMapConfig.builder().removeDbKeys(true).build());

  /**
   * Convert a single {@link DocumentRecord}.
   *
   * @param record {@link DocumentRecord}
   * @return public document response map
   */
  public Map<String, Object> apply(final DocumentRecord record) {

    Map<String, Object> attributes = this.toMap.apply(record.getAttributes());
    Map<String, Object> result = new HashMap<>();
    DOCUMENT_FIELDS.stream().filter(attributes::containsKey)
        .forEach(key -> result.put(key, attributes.get(key)));
    result.put("metadata", record.metadata());

    if (!isEmpty(record.deepLinkPath())) {
      result.remove("lastModifiedDate");
      result.remove("contentLength");
    }

    return result;
  }

  @Override
  public Map<String, Object> apply(final DocumentRecordSet recordSet) {

    Map<String, Object> result = apply(recordSet.documentRecord());

    List<Map<String, Object>> children =
        recordSet.children().stream().map(child -> apply(child.documentRecord())).toList();

    if (!children.isEmpty()) {
      result.put("documents", children);
    }

    return result;
  }
}
