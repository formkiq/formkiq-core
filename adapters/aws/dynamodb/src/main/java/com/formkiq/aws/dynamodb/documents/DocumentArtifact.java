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

/**
 * Represents a document artifact with document and artifact identifiers.
 */
public record DocumentArtifact(String documentId, String artifactId) {

  /**
   * constructor.
   */
  public DocumentArtifact {
    if (documentId == null) {
      throw new IllegalArgumentException("documentId must not be null");
    }
  }

  /**
   * Creates a new {@link DocumentArtifact} instance from the given document and artifact
   * identifiers.
   *
   * @param documentId the unique identifier of the document; must not be {@code null}
   * @param artifactId the unique identifier of the artifact;
   * @return a new {@link DocumentArtifact} containing the provided identifiers
   */
  public static DocumentArtifact of(final String documentId, final String artifactId) {
    return new DocumentArtifact(documentId, artifactId);
  }
}
