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

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagRecord;
import com.formkiq.module.lambdaservices.AwsServiceCache;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Convert {@link com.formkiq.stacks.dynamodb.documents.AddDocumentRequest} to
 * {@link DocumentRecordSet}.
 */
public class AddDocumentRequestToDocumentRecordSet implements
    BiFunction<String, AddDocumentRequest, DocumentRecordSet>, Predicate<AddDocumentRequest> {

  /** {@link AwsServiceCache}. */
  private final AwsServiceCache awsservice;
  /** User Id. */
  private final String userId;
  /** Parent Document Id. */
  private final String belongsToDocumentId;
  /** Existing Document. */
  private final DocumentRecord existing;

  /**
   * constructor.
   *
   * @param serviceCache {@link AwsServiceCache}
   * @param existingItem {@link DocumentRecord}
   * @param username {@link String}
   */
  public AddDocumentRequestToDocumentRecordSet(final AwsServiceCache serviceCache,
      final DocumentRecord existingItem, final String username) {
    this(serviceCache, existingItem, username, null);
  }

  private AddDocumentRequestToDocumentRecordSet(final AwsServiceCache serviceCache,
      final DocumentRecord existingItem, final String username, final String belongsToDocument) {
    this.awsservice = serviceCache;
    this.existing = existingItem;
    this.userId = username;
    this.belongsToDocumentId = belongsToDocument;
  }

  @Override
  public DocumentRecordSet apply(final String siteId, final AddDocumentRequest request) {
    if (!test(request)) {
      throw new IllegalArgumentException("'request.documentId' is required");
    }

    DocumentRecord documentRecord =
        new AddDocumentRequestToDocumentRecord(this.existing, this.userId, this.belongsToDocumentId)
            .apply(siteId, request);

    return DocumentRecordSet.builder().documentRecord(documentRecord)
        .documentAttributeRecords(getDocumentAttributeRecords(siteId, request, documentRecord))
        .documentTagRecords(getDocumentTagRecords(siteId, request, documentRecord))
        .children(getChildren(siteId, request, documentRecord)).build();
  }

  private Collection<DocumentRecordSet> getChildren(final String siteId,
      final AddDocumentRequest request, final DocumentRecord record) {

    return notNull(request.getDocuments()).stream()
        .map(child -> new AddDocumentRequestToDocumentRecordSet(this.awsservice, null, this.userId,
            record.documentId()).apply(siteId, child))
        .toList();
  }

  private Collection<DocumentAttributeRecord> getDocumentAttributeRecords(final String siteId,
      final AddDocumentRequest request, final DocumentRecord record) {

    List<AddDocumentAttribute> attributes = notNull(request.getAttributes());
    if (attributes.isEmpty()) {
      return List.of();
    }

    AddDocumentAttributeToDocumentAttributeRecord transform =
        new AddDocumentAttributeToDocumentAttributeRecord(this.awsservice, siteId,
            DocumentArtifact.of(record.documentId(), record.artifactId()));

    return attributes.stream().flatMap(a -> transform.apply(a).stream()).toList();
  }

  private Collection<DocumentTagRecord> getDocumentTagRecords(final String siteId,
      final AddDocumentRequest request, final DocumentRecord record) {

    AddDocumentTagToDocumentTag transform =
        new AddDocumentTagToDocumentTag(record.documentId(), this.userId);

    return notNull(request.getTags()).stream().map(transform)
        .map(tag -> toDocumentTagRecords(siteId, tag, record)).flatMap(Collection::stream).toList();
  }

  @Override
  public boolean test(final AddDocumentRequest request) {
    return request != null && !isEmpty(request.getDocumentId());
  }

  private Collection<DocumentTagRecord> toDocumentTagRecords(final String siteId,
      final DocumentTag tag, final DocumentRecord record) {
    return DocumentTagRecord.builder().tag(tag).artifactId(record.artifactId()).build(siteId);
  }
}
