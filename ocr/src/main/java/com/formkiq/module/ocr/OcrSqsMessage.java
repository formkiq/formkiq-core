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
package com.formkiq.module.ocr;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * 
 * Ocr Sqs Message.
 *
 */
@Reflectable
public class OcrSqsMessage {

  /** Content Type. */
  private String contentType;
  /** Document Id. */
  private String documentId;
  /** OCR Job Id. */
  private String jobId;
  /** Site Id. */
  private String siteId;

  /**
   * constructor.
   */
  public OcrSqsMessage() {

  }

  /**
   * Get Content-Type.
   * 
   * @return {@link String}
   */
  public String contentType() {
    return this.contentType;
  }

  /**
   * Set Content-Type.
   * 
   * @param type {@link String}
   * @return {@link OcrSqsMessage}
   */
  public OcrSqsMessage contentType(final String type) {
    this.contentType = type;
    return this;
  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String documentId() {
    return this.documentId;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   * @return {@link OcrSqsMessage}
   */
  public OcrSqsMessage documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Get Job Id.
   * 
   * @return {@link String}
   */
  public String jobId() {
    return this.jobId;
  }

  /**
   * Set Job Id.
   * 
   * @param id {@link String}
   * @return {@link OcrSqsMessage}
   */
  public OcrSqsMessage jobId(final String id) {
    this.jobId = id;
    return this;
  }

  /**
   * Get SiteId.
   * 
   * @return {@link String}
   */
  public String siteId() {
    return this.siteId;
  }

  /**
   * Set Site Id.
   * 
   * @param id {@link String}
   * @return {@link OcrSqsMessage}
   */
  public OcrSqsMessage siteId(final String id) {
    this.siteId = id;
    return this;
  }
}
