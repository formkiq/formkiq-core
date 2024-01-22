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
package com.formkiq.aws.s3;

import java.util.Collections;
import java.util.Map;

/**
 * S3Object Metadata.
 *
 */
public class S3ObjectMetadata {
  /** Content Length. */
  private Long contentLength;
  /** Object Content Type. */
  private String contentType;
  /** S3 Object ETag. */
  private String etag;
  /** Object Metadata. */
  private Map<String, String> metadata;
  /** boolean. */
  private boolean objectExists;
  /** S3 Version Id. */
  private String versionId;

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
    return this.metadata != null ? Collections.unmodifiableMap(this.metadata)
        : Collections.emptyMap();
  }

  /**
   * Get S3 Version Id.
   * 
   * @return {@link String}
   */
  public String getVersionId() {
    return this.versionId;
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
    this.metadata = Collections.unmodifiableMap(map);
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

  /**
   * Set S3 Version Id.
   * 
   * @param s3VersionId {@link String}
   * @return {@link S3ObjectMetadata}
   */
  public S3ObjectMetadata setVersionId(final String s3VersionId) {
    this.versionId = s3VersionId;
    return this;
  }
}
