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
package com.formkiq.stacks.dynamodb.config;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Site Configuration Ocr.
 */
@Reflectable
public class SiteConfigurationOcr {
  /** Max Pages per transaction. */
  private long maxPagesPerTransaction;
  /** Max Ocr transactions. */
  private long maxTransactions;

  /**
   * constructor.
   */
  public SiteConfigurationOcr() {}

  /**
   * Get Max Pages Per Transaction.
   * 
   * @return long
   */
  public long getMaxPagesPerTransaction() {
    return this.maxPagesPerTransaction;
  }

  /**
   * Get Max Transactions.
   * 
   * @return long
   */
  public long getMaxTransactions() {
    return this.maxTransactions;
  }

  /**
   * Set Max Pages Per Transaction.
   *
   * @param maxPages long
   * @return SiteConfigurationOcr
   */
  public SiteConfigurationOcr setMaxPagesPerTransaction(final long maxPages) {
    this.maxPagesPerTransaction = maxPages;
    return this;
  }

  /**
   * Set Max Transactions.
   *
   * @param maxTx long
   * @return SiteConfigurationOcr
   */
  public SiteConfigurationOcr setMaxTransactions(final long maxTx) {
    this.maxTransactions = maxTx;
    return this;
  }
}
