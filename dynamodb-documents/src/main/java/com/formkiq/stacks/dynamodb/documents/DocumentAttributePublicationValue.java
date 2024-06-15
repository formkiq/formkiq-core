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
package com.formkiq.stacks.dynamodb.documents;

/**
 * Document Attribute Publication Value.
 */
public class DocumentAttributePublicationValue {

  /** S3 Version. */
  private String s3version;
  /** Content Type. */
  private String contentType;
  /** Path. */
  private String path;

  /**
   * constructor.
   */
  public DocumentAttributePublicationValue() {}

  /**
   * Get Path.
   * 
   * @return String
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Set Path.
   * 
   * @param documentPath {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentAttributePublicationValue setPath(final String documentPath) {
    this.path = documentPath;
    return this;
  }

  /**
   * Get Content Type.
   * 
   * @return String
   */
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Set Content Type.
   * 
   * @param documentContentType {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentAttributePublicationValue setContentType(final String documentContentType) {
    this.contentType = documentContentType;
    return this;
  }

  /**
   * Get S3 Version.
   * 
   * @return String
   */
  public String getS3version() {
    return this.s3version;
  }

  /**
   * Set S3 Version.
   * 
   * @param version {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentAttributePublicationValue setS3version(final String version) {
    this.s3version = version;
    return this;
  }
}
