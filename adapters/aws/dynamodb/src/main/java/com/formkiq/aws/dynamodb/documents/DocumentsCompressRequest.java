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

import java.util.List;

import com.formkiq.graalvm.annotations.Reflectable;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Represents a document compression request.
 *
 * @param documentIds {@link List} of document ids to compress
 * @param documents {@link List} of document artifacts to compress
 * @param compressionId {@link String}
 * @param downloadUrl {@link String}
 * @param siteId {@link String}
 */
@Reflectable
public record DocumentsCompressRequest(@Reflectable List<String> documentIds,
    @Reflectable List<DocumentArtifact> documents, @Reflectable String compressionId,
    @Reflectable String downloadUrl, @Reflectable String siteId) {

  private static String emptyToNull(final String value) {
    return value != null && value.isEmpty() ? null : value;
  }

  /**
   * Gets the documents to compress.
   *
   * @return {@link List} {@link DocumentArtifact}
   */
  public List<DocumentArtifact> documentArtifacts() {
    List<DocumentArtifact> artifacts = List.of();

    if (!notNull(this.documents).isEmpty()) {
      artifacts = this.documents.stream()
          .map(d -> DocumentArtifact.of(d.documentId(), emptyToNull(d.artifactId()))).toList();
    } else if (this.documentIds != null) {
      artifacts = this.documentIds.stream().map(d -> DocumentArtifact.of(d, null)).toList();
    }

    return artifacts;
  }

  /**
   * Creates a {@link DocumentsCompressRequest} with compression task details.
   *
   * @param taskCompressionId {@link String}
   * @param taskDownloadUrl {@link String}
   * @param taskSiteId {@link String}
   * @return {@link DocumentsCompressRequest}
   */
  public DocumentsCompressRequest withTaskDetails(final String taskCompressionId,
      final String taskDownloadUrl, final String taskSiteId) {
    return new DocumentsCompressRequest(this.documentIds, this.documents, taskCompressionId,
        taskDownloadUrl, taskSiteId);
  }
}
