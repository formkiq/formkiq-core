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

/**
 * Builder for {@link DocumentRecordSet}.
 */
public class DocumentRecordSetBuilder {

  /** {@link DocumentRecord}. */
  private DocumentRecord documentRecord;
  /** {@link DocumentAttributeRecord}. */
  private Collection<DocumentAttributeRecord> documentAttributeRecords;
  /** {@link DocumentTagRecord}. */
  private Collection<DocumentTagRecord> documentTagRecords;
  /** Child {@link DocumentRecordSet}. */
  private Collection<DocumentRecordSet> children;

  /**
   * Build {@link DocumentRecordSet}.
   *
   * @return {@link DocumentRecordSet}
   */
  public DocumentRecordSet build() {
    return new DocumentRecordSet(documentRecord, documentAttributeRecords, documentTagRecords,
        children);
  }

  /**
   * Set child document record sets.
   *
   * @param recordSets {@link Collection} {@link DocumentRecordSet}
   * @return {@link DocumentRecordSetBuilder}
   */
  public DocumentRecordSetBuilder children(final Collection<DocumentRecordSet> recordSets) {
    this.children = recordSets;
    return this;
  }

  /**
   * Set document attribute records.
   *
   * @param records {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link DocumentRecordSetBuilder}
   */
  public DocumentRecordSetBuilder documentAttributeRecords(
      final Collection<DocumentAttributeRecord> records) {
    this.documentAttributeRecords = records;
    return this;
  }

  /**
   * Set document record.
   *
   * @param record {@link DocumentRecord}
   * @return {@link DocumentRecordSetBuilder}
   */
  public DocumentRecordSetBuilder documentRecord(final DocumentRecord record) {
    this.documentRecord = record;
    return this;
  }

  /**
   * Set document tag records.
   *
   * @param records {@link Collection} {@link DocumentTagRecord}
   * @return {@link DocumentRecordSetBuilder}
   */
  public DocumentRecordSetBuilder documentTagRecords(final Collection<DocumentTagRecord> records) {
    this.documentTagRecords = records;
    return this;
  }
}
