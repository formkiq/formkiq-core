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
 * Options to use when saving a document.
 *
 */
public class SaveDocumentOptions {

  /** Whether to Save the Document Date. */
  private boolean saveDocumentDate = false;
  /** Time to Live. */
  private String timeToLive;

  /**
   * constructor.
   */
  public SaveDocumentOptions() {

  }

  /**
   * Whether to Save Document Date.
   * 
   * @return boolean
   */
  public boolean saveDocumentDate() {
    return this.saveDocumentDate;
  }

  /**
   * Set Save Document Date.
   * 
   * @param save boolean
   * @return {@link SaveDocumentOptions}
   */
  public SaveDocumentOptions saveDocumentDate(final boolean save) {
    this.saveDocumentDate = save;
    return this;
  }

  /**
   * Get Time to Live.
   * 
   * @return {@link String}
   */
  public String timeToLive() {
    return this.timeToLive;
  }

  /**
   * Set Time to Live.
   * 
   * @param ttl {@link String}
   * @return {@link SaveDocumentOptions}
   */
  public SaveDocumentOptions timeToLive(final String ttl) {
    this.timeToLive = ttl;
    return this;
  }
}
