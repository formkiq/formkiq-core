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
package com.formkiq.aws.dynamodb.objects;

/**
 * Supported Conversion Formats.
 *
 */
public enum MimeType {

  /** image/bmp. */
  MIME_BMP("image/bmp"),
  /** application/vnd.openxmlformats-officedocument.wordprocessingml.document. */
  MIME_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
  /** image/gif. */
  MIME_GIF("image/gif"),
  /** text/html. */
  MIME_HTML("text/html"),
  /** image/jpeg. */
  MIME_JPEG("image/jpeg"),
  /** image/jpg. */
  MIME_JPG("image/jpg"),
  /** application/json. */
  MIME_JSON("application/json"),
  /** application/pdf. */
  MIME_PDF("application/pdf"),
  /** image/png. */
  MIME_PNG("image/png"),
  /** image/tif. */
  MIME_TIF("image/tif"),
  /** image/tiff. */
  MIME_TIFF("image/tiff"),
  /** Unknown Mime. */
  MIME_UNKNOWN("UNKNOWN"),
  /** image/webp. */
  MIME_WEBP("image/webp");

  /**
   * Is Content Type plain text.
   * 
   * @param contentType {@link String}
   * @return boolean
   */
  public static boolean isPlainText(final String contentType) {
    return contentType != null
        && (contentType.startsWith("text/") || "application/json".equals(contentType)
            || "application/x-www-form-urlencoded".equals(contentType));
  }

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

  /**
   * Get File Extension.
   * 
   * @return {@link String}
   */
  public String getExtension() {
    int pos = this.contentType.lastIndexOf("/");
    return pos > -1 ? this.contentType.substring(pos + 1).toLowerCase() : null;
  }

  /**
   * Find {@link MimeType} from Content-Type.
   * 
   * @param ct {@link String}
   * @return {@link MimeType}
   */
  public static MimeType fromContentType(final String ct) {

    MimeType type = MimeType.MIME_UNKNOWN;

    if (ct != null) {
      for (MimeType mt : MimeType.values()) {
        if (ct.equals(mt.getContentType())) {
          type = mt;
          break;
        }
      }
    }

    return type;
  }
}
