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

import com.formkiq.validation.ValidationChecks;

import java.util.function.BiFunction;

/**
 * {@link BiFunction} that converts a {@link DocumentRecord} into a {@link DocumentRecordBuilder}.
 */
public class DocumentRecordToDocumentRecordBuilder
    implements BiFunction<String, DocumentRecord, DocumentRecordBuilder> {

  /**
   * Convert a {@link DocumentRecord} into a {@link DocumentRecordBuilder}.
   *
   * @param parentDocumentId the parent document id
   * @param record the source document record
   * @return a new {@link DocumentRecordBuilder}
   */
  @Override
  public DocumentRecordBuilder apply(final String parentDocumentId, final DocumentRecord record) {

    ValidationChecks.checkNotNull("record", record);
    ValidationChecks.checkNotNull("documentId", record.documentId());

    return DocumentRecord.builder().parentDocumentId(parentDocumentId)
        .documentId(record.documentId()).artifactId(record.artifactId())
        .belongsToDocumentId(record.belongsToDocumentId()).path(record.path())
        .deepLinkPath(record.deepLinkPath()).contentType(record.contentType())
        .contentLength(record.contentLength()).checksum(record.checksum())
        .checksumType(record.checksumType()).s3version(record.s3version()).userId(record.userId())
        .version(record.version()).width(record.width()).height(record.height())
        .timeToLive(record.timeToLive()).insertedDate(record.insertedDate())
        .lastModifiedDate(record.lastModifiedDate()).metadata(record.metadata());
  }
}
