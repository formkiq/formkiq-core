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

import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.validation.ValidationChecks;

import java.util.function.BiFunction;

/**
 * {@link BiFunction} that converts a {@link DocumentItem} into a {@link DocumentRecord}.
 *
 * <p>
 * The {@code siteId} parameter is required to build the
 * {@link com.formkiq.aws.dynamodb.DynamoDbKey} using {@link DocumentRecordBuilder#build(String)}.
 *
 * <p>
 * This implementation delegates key generation to the {@link DocumentRecordBuilder}, ensuring
 * consistent PK/SK/GSI logic.
 */
public class DocumentItemToDocumentRecordBuilder
    implements BiFunction<String, DocumentItem, DocumentRecordBuilder> {

  /**
   * Convert a {@link DocumentItem} into a {@link DocumentRecord}.
   *
   * @param parentDocumentId the parent document id
   * @param item the source document item
   * @return a new {@link DocumentRecordBuilder}
   * @throws NullPointerException if {@code siteId} or {@code item} is null
   */
  public DocumentRecordBuilder apply(final String parentDocumentId, final DocumentItem item) {

    ValidationChecks.checkNotNull("item", item);
    ValidationChecks.checkNotNull("documentId", item.getDocumentId());

    return DocumentRecord.builder().parentDocumentId(parentDocumentId)
        .documentId(item.getDocumentId()).artifactId(item.getArtifactId())
        .belongsToDocumentId(item.getBelongsToDocumentId()).path(item.getPath())
        .deepLinkPath(item.getDeepLinkPath()).contentType(item.getContentType())
        .contentLength(item.getContentLength()).checksum(item.getChecksum())
        .checksumType(item.getChecksumType()).s3version(item.getS3version())
        .userId(item.getUserId()).version(item.getVersion()).width(item.getWidth())
        .height(item.getHeight()).timeToLive(item.getTimeToLive())
        .insertedDate(item.getInsertedDate()).lastModifiedDate(item.getLastModifiedDate())
        .metadata(item.getMetadata());
  }
}
