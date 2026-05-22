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
package com.formkiq.stacks.dynamodb.documents;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentMetadata;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.documents.DocumentRecordToDocumentRecordBuilder;

import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Convert {@link AddDocumentRequest} to {@link DocumentRecord}.
 */
public class AddDocumentRequestToDocumentRecord
    implements BiFunction<String, AddDocumentRequest, DocumentRecord> {

  /** BelongsToDocumentId. */
  private final String belongsToDocumentId;
  /** User Id. */
  private final String userId;
  /** Existing {@link DocumentRecord}. */
  private final DocumentRecord existing;

  /**
   * constructor.
   * 
   * @param existingDocument {@link DocumentRecord}
   * @param username {@link String}
   * @param belongsToDocument {@link String}
   */
  public AddDocumentRequestToDocumentRecord(final DocumentRecord existingDocument,
      final String username, final String belongsToDocument) {
    this.belongsToDocumentId = belongsToDocument;
    this.userId = username;
    this.existing = existingDocument;
  }

  @Override
  public DocumentRecord apply(final String siteId, final AddDocumentRequest r) {
    DocumentRecordBuilder builder =
        this.existing != null ? new DocumentRecordToDocumentRecordBuilder().apply(null, existing)
            : DocumentRecord.builder();

    builder.belongsToDocumentId(this.belongsToDocumentId);

    if (!isEmpty(r.getContentType())) {
      builder.contentType(r.getContentType());
    }

    if (!isEmpty(r.getContent())) {
      builder.deepLinkPath(null);
    } else if (!isEmpty(r.getDeepLinkPath())) {
      builder.deepLinkPath(r.getDeepLinkPath());
    }

    if (this.existing == null) {
      builder.documentId(r.getDocumentId()).userId(this.userId);
    } else {
      builder.lastModifiedDate(new Date());
    }

    if (!notNull(r.getMetadata()).isEmpty()) {
      builder.removeMetadata(r.getMetadata().stream().filter(DocumentMetadata::isEmpty)
          .map(DocumentMetadata::key).toList());
      builder.addMetadata(
          r.getMetadata().stream().filter(Predicate.not(DocumentMetadata::isEmpty)).toList());
    }

    if (!isEmpty(r.getPath())) {
      builder.path(r.getPath());
    }

    builder.width(r.getWidth()).height(r.getHeight()).checksum(r.getChecksum())
        .checksumType(r.getChecksumType()).timeToLive(r.getTimeToLive());

    updateArtifactId(r, builder);

    return builder.build(siteId);
  }

  private void updateArtifactId(final AddDocumentRequest r, final DocumentRecordBuilder builder) {
    if (r.isArtifacts() && (this.existing == null || this.existing.artifactId() == null)) {
      builder.artifactId(ID.ulid());
    }
  }
}
