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
package com.formkiq.aws.s3;

import java.util.Map;

/**
 * S3Object Metadata.
 *
 */
public class S3ObjectMetadata {
  /** S3 Object ETag. */
  private String etag;
  /** Object Content Type. */
  private String contentType;
  /** Object Metadata. */
  private Map<String, String> metadata;
  /** boolean. */
  private boolean objectExists;
  /** Content Length. */
  private Long contentLength;

  /**
   * constructor.
   */
  public S3ObjectMetadata() {}

  /**
   * Get Content Length.
   * 
   * @return {@link Long}
   */
  public Long getContentLength() {
    return this.contentLength;
  }

  /**
   * Get Content Type.
   * 
   * @return {@link String}
   */
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Get Object ETag.
   * 
   * @return {@link String}
   */
  public String getEtag() {
    return this.etag;
  }

  /**
   * Get Metadata.
   * 
   * @return {@link Map}
   */
  public Map<String, String> getMetadata() {
    return this.metadata;
  }

  /**
   * Does Object Exist.
   * 
   * @return boolean
   */
  public boolean isObjectExists() {
    return this.objectExists;
  }

  /**
   * Set Content Length.
   * 
   * @param length {@link Long}
   */
  public void setContentLength(final Long length) {
    this.contentLength = length;
  }

  /**
   * Set Object Content Type.
   * 
   * @param objectContentType {@link String}
   * @return {@link S3ObjectMetadata}
   */
  public S3ObjectMetadata setContentType(final String objectContentType) {
    this.contentType = objectContentType;
    return this;
  }

  /**
   * Set Object ETag.
   * 
   * @param objectEtag {@link String}
   * @return {@link S3ObjectMetadata}
   */
  public S3ObjectMetadata setEtag(final String objectEtag) {
    this.etag = objectEtag;
    return this;
  }

  /**
   * Set Metadata.
   * 
   * @param map {@link Map}
   * @return {@link S3ObjectMetadata}
   */
  public S3ObjectMetadata setMetadata(final Map<String, String> map) {
    this.metadata = map;
    return this;
  }

  /**
   * Sets Whether Object Exists.
   * 
   * @param exists boolean
   * @return {@link S3ObjectMetadata}
   */
  public S3ObjectMetadata setObjectExists(final boolean exists) {
    this.objectExists = exists;
    return this;
  }
}
