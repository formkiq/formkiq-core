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

/**
 * Extended Attributes for the {@link FolderIndexRecord}.
 */
public class FolderIndexRecordExtended {

  /** Record has changed. */
  private boolean isChanged = false;
  /** {@link FolderIndexRecord}. */
  private FolderIndexRecord record = null;

  /**
   * constructor.
   * 
   * @param folderIndexRecord {@link FolderIndexRecord}
   * @param isRecordChanged boolean
   */
  public FolderIndexRecordExtended(final FolderIndexRecord folderIndexRecord,
      final boolean isRecordChanged) {
    this.isChanged = isRecordChanged;
    this.record = folderIndexRecord;
  }

  /**
   * Set IsChanged.
   * 
   * @param changed boolean
   * @return {@link FolderIndexRecordExtended}
   */
  public FolderIndexRecordExtended changed(final boolean changed) {
    this.isChanged = changed;
    return this;
  }

  /**
   * Is Record Changed.
   * 
   * @return boolean
   */
  public boolean isChanged() {
    return this.isChanged;
  }

  /**
   * Get {@link FolderIndexRecord}.
   * 
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord record() {
    return this.record;
  }
}
