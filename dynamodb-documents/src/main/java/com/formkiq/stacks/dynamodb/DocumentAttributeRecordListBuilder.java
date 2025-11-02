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
package com.formkiq.stacks.dynamodb;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;

import java.util.Collection;

/**
 * Class for creating list {@link DocumentAttributeRecord}.
 */
public class DocumentAttributeRecordListBuilder {

  /** New Attributes. */
  private Collection<DocumentAttributeRecord> newAttributes;
  /** Previously saved attributes. */
  private Collection<DocumentAttributeRecord> previousAttributes;
  /** Attributes that will be deleted. */
  private Collection<DocumentAttributeRecord> toBeDeletedAttributes;
  /** Previous Composite Keys. */
  private Collection<DocumentAttributeRecord> previousCompositeKeys;
  /** CompositeKeys To Be Deleted. */
  private Collection<DocumentAttributeRecord> compositeKeysToBeDeleted;

  /**
   * constructor.
   */
  public DocumentAttributeRecordListBuilder() {}

  /**
   * Get Composite Keys To Be Deleted.
   * 
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  public Collection<DocumentAttributeRecord> getCompositeKeysToBeDeleted() {
    return this.compositeKeysToBeDeleted;
  }

  /**
   * Get New Attributes.
   *
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  public Collection<DocumentAttributeRecord> getNewAttributes() {
    return this.newAttributes;
  }

  /**
   * Get Previous Attributes.
   *
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  public Collection<DocumentAttributeRecord> getPreviousAttributes() {
    return this.previousAttributes;
  }

  /**
   * Get Previous Composite Keys.
   *
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  public Collection<DocumentAttributeRecord> getPreviousCompositeKeys() {
    return this.previousCompositeKeys;
  }

  /**
   * Get {@link DocumentAttributeRecord} that will be deleted.
   *
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  public Collection<DocumentAttributeRecord> getToBeDeletedAttributes() {
    return this.toBeDeletedAttributes;
  }

  /**
   * Set Composite Keys To Be Deleted.
   * 
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   */
  public void setCompositeKeysToBeDeleted(final Collection<DocumentAttributeRecord> attributes) {
    this.compositeKeysToBeDeleted = attributes;
  }

  /**
   * Set New Attributes.
   *
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link DocumentAttributeRecordListBuilder}
   */
  public DocumentAttributeRecordListBuilder setNewAttributes(
      final Collection<DocumentAttributeRecord> attributes) {
    this.newAttributes = attributes;
    return this;
  }

  /**
   * Set Previous Attributes.
   *
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link DocumentAttributeRecordListBuilder}
   */
  public DocumentAttributeRecordListBuilder setPreviousAttributes(
      final Collection<DocumentAttributeRecord> attributes) {
    this.previousAttributes = attributes;
    return this;
  }

  /**
   * Set Previous Composite Keys.
   *
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link DocumentAttributeRecordListBuilder}
   */
  public DocumentAttributeRecordListBuilder setPreviousCompositeKeys(
      final Collection<DocumentAttributeRecord> attributes) {
    this.previousCompositeKeys = attributes;
    return this;
  }

  /**
   * Set To Be Deleted Attributes.
   *
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link DocumentAttributeRecordListBuilder}
   */
  public DocumentAttributeRecordListBuilder setToBeDeletedAttributes(
      final Collection<DocumentAttributeRecord> attributes) {
    this.toBeDeletedAttributes = attributes;
    return this;
  }
}
