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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AttributeValueToDocumentArtifact}.
 */
public class AttributeValueToDocumentArtifactTest {

  /** {@link AttributeValueToDocumentArtifact}. */
  private static final AttributeValueToDocumentArtifact MAPPER =
      new AttributeValueToDocumentArtifact();

  @Test
  void testApply01() {
    String documentId = "document123";
    String artifactId = "artifact456";

    DocumentArtifact artifact = MAPPER.apply(Map.of("documentId", AttributeValue.fromS(documentId),
        "artifactId", AttributeValue.fromS(artifactId)));

    assertEquals(documentId, artifact.documentId());
    assertEquals(artifactId, artifact.artifactId());
  }

  @Test
  void testApply02() {
    assertNull(MAPPER.apply(Map.of("artifactId", AttributeValue.fromS("artifact456"))));
  }

  @Test
  void testApply03() {
    String documentId = "document123";
    String artifactId = "01KPF6RW32ZC981PB0PVX16W2J";

    DocumentArtifact artifact = MAPPER.apply(Map.of("documentId", AttributeValue.fromS(documentId),
        "SK", AttributeValue.fromS("attr_art#" + artifactId + "#myattr#555")));

    assertEquals(documentId, artifact.documentId());
    assertEquals(artifactId, artifact.artifactId());
  }

  @Test
  void testApply04() {
    String documentId = "document123";
    String artifactId = "artifact456";

    DocumentArtifact artifact = MAPPER.apply(Map.of("documentId", AttributeValue.fromS(documentId),
        "SK", AttributeValue.fromS("document_art#" + artifactId)));

    assertEquals(documentId, artifact.documentId());
    assertEquals(artifactId, artifact.artifactId());
  }

  @Test
  void testApply05() {
    String documentId = "document123";
    String artifactId = "artifact456";

    DocumentArtifact artifact = MAPPER.apply(Map.of("documentId", AttributeValue.fromS(documentId),
        "SK", AttributeValue.fromS("tags_art#" + artifactId + "#status")));

    assertEquals(documentId, artifact.documentId());
    assertEquals(artifactId, artifact.artifactId());
  }

  @Test
  void testApply06() {
    String documentId = "document123";
    String artifactId = "artifact456";

    DocumentArtifact artifact = MAPPER.apply(Map.of("documentId", AttributeValue.fromS(documentId),
        "SK", AttributeValue.fromS("action_art#" + artifactId + "#001#OCR")));

    assertEquals(documentId, artifact.documentId());
    assertEquals(artifactId, artifact.artifactId());
  }
}
