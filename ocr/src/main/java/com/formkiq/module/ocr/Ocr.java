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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Ocr Data Holder.
 *
 */
public class Ocr implements DynamodbRecord<Ocr>, DbKeys {

  /** AddPdfDetectedCharactersAsText. */
  private boolean addPdfDetectedCharactersAsText = false;
  /** Content Type. */
  private String contentType;
  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Document Id. */
  private String documentId;
  /** {@link OcrEngine}. */
  private OcrEngine engine;
  /** Record inserted date. */
  private Date insertedDate;
  /** Job Id. */
  private String jobId;
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

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> pkvalues = new HashMap<>();

    addS(pkvalues, PK, pk(siteId));
    addS(pkvalues, SK, sk());

    addS(pkvalues, "documentId", documentId());

    String fulldate = this.df.format(new Date());
    addS(pkvalues, "insertedDate", fulldate);
    addS(pkvalues, "contentType", contentType());
    addS(pkvalues, "userId", userId());
    addS(pkvalues, "jobId", jobId());
    addS(pkvalues, "ocrEngine", engine().name().toLowerCase());
    addS(pkvalues, "ocrStatus", status().name().toLowerCase());
    addS(pkvalues, "addPdfDetectedCharactersAsText",
        addPdfDetectedCharactersAsText() ? "true" : "false");

    return pkvalues;
  }

  @Override
  public Ocr getFromAttributes(final String siteId, final Map<String, AttributeValue> attrs) {

    Ocr ocr = new Ocr().documentId(ss(attrs, "documentId")).userId(ss(attrs, "userId"))
        .contentType(ss(attrs, "contentType")).jobId(ss(attrs, "jobId"));

    if (attrs.containsKey("ocrEngine")) {
      ocr.engine(OcrEngine.valueOf(ss(attrs, "ocrEngine").toUpperCase()));
    }

    if (attrs.containsKey("ocrStatus")) {
      ocr.status(OcrScanStatus.valueOf(ss(attrs, "ocrStatus").toUpperCase()));
    }

    if (attrs.containsKey("addPdfDetectedCharactersAsText")) {
      ocr.addPdfDetectedCharactersAsText(
          "true".equals(attrs.get("addPdfDetectedCharactersAsText").s()));
    }

    if (attrs.containsKey("insertedDate")) {
      try {
        ocr = ocr.insertedDate(this.df.parse(ss(attrs, "insertedDate")));
      } catch (ParseException e) {
        // ignore
      }
    }

    return ocr;
  }

  /**
   * Get Inserted Date.
   * 
   * @return {@link Date}
   */
  public Date insertedDate() {
    return this.insertedDate;
  }

  /**
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   * @return {@link Ocr}
   */
  public Ocr insertedDate(final Date date) {
    this.insertedDate = date;
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

  @Override
  public String pk(final String siteId) {
    return createDatabaseKey(siteId, PREFIX_DOCS + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    return null;
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {
    return "ocr" + DbKeys.TAG_DELIMINATOR;
  }

  @Override
  public String skGsi1() {
    return null;
  }

  @Override
  public String skGsi2() {
    return null;
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
