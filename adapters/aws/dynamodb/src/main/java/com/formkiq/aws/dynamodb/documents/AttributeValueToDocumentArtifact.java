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
package com.formkiq.aws.dynamodb.documents;

import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.Function;

/**
 * Convert {@link AttributeValue} to {@link DocumentArtifact}.
 */
public class AttributeValueToDocumentArtifact
    implements Function<Map<String, AttributeValue>, DocumentArtifact> {

  /** Artifact segment marker. */
  private static final String ARTIFACT_MARKER = "art#";

  @Override
  public DocumentArtifact apply(final Map<String, AttributeValue> av) {
    if (!av.containsKey("documentId")) {
      return null;
    }

    String documentId = DynamoDbTypes.toString(av.get("documentId"));
    String artifactId = DynamoDbTypes.toString(av.get("artifactId"));

    if (artifactId == null) {
      artifactId = parseArtifactId(DynamoDbTypes.toString(av.get("SK")));
    }

    return DocumentArtifact.of(documentId, artifactId);
  }

  private String parseArtifactId(final String sk) {

    String ret = null;
    if (sk != null && !sk.isEmpty()) {

      if (sk.startsWith("action_art#")) {

        String[] parts = sk.split("#");
        ret = parts.length > 1 ? parts[1] : null;

      } else {

        int marker = sk.indexOf(ARTIFACT_MARKER);
        if (marker < 0) {
          return null;
        }

        int start = marker + ARTIFACT_MARKER.length();
        int end = sk.indexOf('#', start);
        ret = end >= 0 ? sk.substring(start, end) : sk.substring(start);
      }
    }

    return ret;
  }
}
