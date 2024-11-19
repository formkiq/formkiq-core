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
package com.formkiq.aws.dynamodb;

import java.util.Collection;

/**
 * {@link DynamodbRecord} Transaction.
 */
public class DynamodbRecordTx {
  /** Records to Save. */
  private final Collection<? extends DynamodbRecord<?>> saves;
  /** Records to Delete. */
  private final Collection<? extends DynamodbRecord<?>> deletes;

  /**
   * constructor.
   * 
   * @param recordsToSave {@link Collection}
   * @param recordsToDelete {@link Collection}
   */
  public DynamodbRecordTx(final Collection<? extends DynamodbRecord<?>> recordsToSave,
      final Collection<? extends DynamodbRecord<?>> recordsToDelete) {
    this.saves = recordsToSave;
    this.deletes = recordsToDelete;
  }

  /**
   * Get {@link DynamodbRecord} to Save.
   * 
   * @return {@link Collection} {@link DynamodbRecord}
   */
  public Collection<? extends DynamodbRecord<?>> getSaves() {
    return this.saves;
  }

  /**
   * Get {@link DynamodbRecord} to Delete.
   * 
   * @return {@link Collection} {@link DynamodbRecord}
   */
  public Collection<? extends DynamodbRecord<?>> getDeletes() {
    return this.deletes;
  }
}
