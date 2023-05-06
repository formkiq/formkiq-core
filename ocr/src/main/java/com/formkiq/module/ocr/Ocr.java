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

/**
 * 
 * Ocr Data Holder.
 *
 */
public class Ocr {
  /** AddPdfDetectedCharactersAsText. */
  private boolean addPdfDetectedCharactersAsText = false;
  /** Content Type. */
  private String contentType;
  /** Document Id. */
  private String documentId;
  /** {@link OcrEngine}. */
  private OcrEngine engine;
  /** Job Id. */
  private String jobId;
  /** SiteId. */
  private String siteId;
  /** {@link OcrScanStatus}. */
  private OcrScanStatus status;
  /** UserId. */
  private String userId;

  /**
   * constructor.
   */
  public Ocr() {

  }

  /**
   * Is AddPdfDetectedCharactersAsText.
   * 
   * @return boolean
   */
  public boolean addPdfDetectedCharactersAsText() {
    return this.addPdfDetectedCharactersAsText;
  }

  /**
   * Set AddPdfDetectedCharactersAsText.
   * 
   * @param bool boolean
   * @return {@link Ocr}
   */
  public Ocr addPdfDetectedCharactersAsText(final boolean bool) {
    this.addPdfDetectedCharactersAsText = bool;
    return this;
  }

  /**
   * Get Content Type.
   * 
   * @return {@link String}
   */
  public String contentType() {
    return this.contentType;
  }

  /**
   * Set Content Type.
   * 
   * @param type {@link String}
   * @return {@link Ocr}
   */
  public Ocr contentType(final String type) {
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
   * @return {@link Ocr}
   */
  public Ocr documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Get {@link OcrEngine}.
   * 
   * @return {@link OcrEngine}
   */
  public OcrEngine engine() {
    return this.engine;
  }

  /**
   * Set {@link OcrEngine}.
   * 
   * @param ocrEngine {@link OcrEngine}
   * @return {@link Ocr}
   */
  public Ocr engine(final OcrEngine ocrEngine) {
    this.engine = ocrEngine;
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
   * @return {@link Ocr}
   */
  public Ocr jobId(final String id) {
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
   * Set SiteId.
   * 
   * @param id {@link String}
   * @return {@link Ocr}
   */
  public Ocr siteId(final String id) {
    this.siteId = id;
    return this;
  }

  /**
   * Get {@link OcrScanStatus}.
   * 
   * @return {@link OcrScanStatus}
   */
  public OcrScanStatus status() {
    return this.status;
  }

  /**
   * Set {@link OcrScanStatus}.
   * 
   * @param ocrScanStatus {@link OcrScanStatus}
   * @return {@link Ocr}
   */
  public Ocr status(final OcrScanStatus ocrScanStatus) {
    this.status = ocrScanStatus;
    return this;
  }

  /**
   * Get User Id.
   * 
   * @return {@link String}
   */
  public String userId() {
    return this.userId;
  }

  /**
   * Set UserId.
   * 
   * @param user {@link String}
   * @return {@link Ocr}
   */
  public Ocr userId(final String user) {
    this.userId = user;
    return this;
  }
}
