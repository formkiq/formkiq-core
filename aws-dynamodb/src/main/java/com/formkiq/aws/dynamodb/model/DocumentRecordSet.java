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
package com.formkiq.aws.dynamodb.model;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper record for a document and its related attribute and tag records.
 *
 * @param documentRecord {@link DocumentRecord}
 * @param documentAttributeRecords {@link Collection} {@link DocumentAttributeRecord}
 * @param documentTagRecords {@link Collection} {@link DocumentTagRecord}
 * @param children {@link Collection} {@link DocumentRecordSet}
 */
public record DocumentRecordSet(DocumentRecord documentRecord,
    Collection<DocumentAttributeRecord> documentAttributeRecords,
    Collection<DocumentTagRecord> documentTagRecords, Collection<DocumentRecordSet> children) {

  /**
   * Canonical constructor.
   */
  public DocumentRecordSet {
    Objects.requireNonNull(documentRecord, "documentRecord must not be null");

    documentAttributeRecords =
        documentAttributeRecords != null ? List.copyOf(documentAttributeRecords) : List.of();
    documentTagRecords = documentTagRecords != null ? List.copyOf(documentTagRecords) : List.of();
    children = children != null ? List.copyOf(children) : List.of();
  }

  /**
   * Creates a new {@link DocumentRecordSetBuilder}.
   *
   * @return {@link DocumentRecordSetBuilder}
   */
  public static DocumentRecordSetBuilder builder() {
    return new DocumentRecordSetBuilder();
  }
}
