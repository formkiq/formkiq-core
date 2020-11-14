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
package com.formkiq.stacks.common.formats;

/**
 * Supported Conversion Formats.
 *
 */
public enum MimeType {

  /** text/html. */
  MIME_HTML("text/html"),
  /** application/vnd.openxmlformats-officedocument.wordprocessingml.document. */
  MIME_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
  /** application/pdf. */
  MIME_PDF("application/pdf"),
  /** image/png. */
  MIME_PNG("image/png"),
  /** image/jpeg. */
  MIME_JPEG("image/jpeg"),
  /** application/json. */
  MIME_JSON("application/json"),
  /** Unknown Mime. */
  MIME_UNKNOWN("UNKNOWN");

  /** Content Type. */
  private String contentType;

  /**
   * constructor.
   * 
   * @param type {@link String}
   */
  MimeType(final String type) {
    this.contentType = type;
  }

  /**
   * Get Content Type.
   * 
   * @return {@link String}
   */
  public String getContentType() {
    return this.contentType;
  }
}
