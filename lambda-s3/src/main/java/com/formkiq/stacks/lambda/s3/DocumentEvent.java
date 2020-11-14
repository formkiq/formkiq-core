/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.lambda.s3;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Response to a Document Create Event.
 *
 */
@Reflectable
public class DocumentEvent {

  /** Document SiteId. */
  private String siteId;
  /** Document Id. */
  private String documentId;
  /** S3 Key. */
  private String s3key;
  /** S3 Bucket. */
  private String s3bucket;
  /** Document Type. */
  private String type;

  /**
   * constructor.
   */
  public DocumentEvent() {

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
   * @return {@link DocumentEvent}
   */
  public DocumentEvent documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Get S3 Bucket.
   * 
   * @return {@link String}
   */
  public String s3bucket() {
    return this.s3bucket;
  }

  /**
   * Set S3 Bucket.
   * 
   * @param bucket {@link String}
   * @return {@link DocumentEvent}
   */
  public DocumentEvent s3bucket(final String bucket) {
    this.s3bucket = bucket;
    return this;
  }

  /**
   * Get S3 Key.
   * 
   * @return {@link String}
   */
  public String s3key() {
    return this.s3key;
  }

  /**
   * Set S3 Key.
   * 
   * @param key {@link String}
   * @return {@link DocumentEvent}
   */
  public DocumentEvent s3key(final String key) {
    this.s3key = key;
    return this;
  }

  /**
   * Get Site Id.
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
   * @return {@link DocumentEvent}
   */
  public DocumentEvent siteId(final String id) {
    this.siteId = id;
    return this;
  }

  /**
   * Get {@link DocumentEvent} type.
   * 
   * @return {@link String}
   */
  public String type() {
    return this.type;
  }

  /**
   * Set {@link DocumentEvent} type.
   * 
   * @param eventtype {@link String}
   */
  public void type(final String eventtype) {
    this.type = eventtype;
  }
}
