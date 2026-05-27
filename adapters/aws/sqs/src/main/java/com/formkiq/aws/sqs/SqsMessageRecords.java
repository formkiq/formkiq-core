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
package com.formkiq.aws.sqs;

import java.util.ArrayList;
import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;
import com.google.gson.annotations.SerializedName;

/**
 * Sqs Records Object.
 *
 */
@Reflectable
public class SqsMessageRecords {

  /** Input Records. */
  @SerializedName("Records")
  private List<SqsMessageRecord> records;

  /**
   * constructor.
   */
  public SqsMessageRecords() {
    this.records = new ArrayList<>();
  }

  /**
   * Get Records.
   * 
   * @return {@link List} {@link SqsMessageRecord}
   */
  public List<SqsMessageRecord> records() {
    return this.records;
  }

  /**
   * Set Records.
   * 
   * @param list {@link List} {@link SqsMessageRecord}
   * @return {@link SqsMessageRecords}
   */
  public SqsMessageRecords records(final List<SqsMessageRecord> list) {
    this.records = list;
    return this;
  }
}
