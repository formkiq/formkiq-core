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

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Supported Conversion Formats.
 *
 */
public enum MimeType {

  /** image/bmp. */
  MIME_BMP("image/bmp", "bmp"),
  /** image/heic. */
  MIME_HEIC("image/heic", "heic"),
  /** application/msword. */
  MIME_DOC("application/msword", "doc"),
  /** application/vnd.openxmlformats-officedocument.wordprocessingml.document. */
  MIME_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
  /** image/gif. */
  MIME_GIF("image/gif", "gif"),
  /** text/html. */
  MIME_HTML("text/html", "html"),
  /** image/jpeg. */
  MIME_JPEG("image/jpeg", "jpg"),
  /** image/jpg. */
  MIME_JPG("image/jpg", "jpg"),
  /** application/json. */
  MIME_JSON("application/json", "json"),
  /** application/octet-stream. */
  MIME_OCTET_STREAM("application/octet-stream", ""),
  /** application/pdf. */
  MIME_PDF("application/pdf", "pdf"),
  /** text/plain. */
  MIME_PLAIN_TEXT("text/plain", "txt"),
  /** text/csv. */
  MIME_PLAIN_CSV("text/csv", "csv"),
  /** text/plain. */
  MIME_PLAIN_MARKDOWN("text/markdown", "md"),
  /** image/png. */
  MIME_PNG("image/png", "png"),
  /** image/tif. */
  MIME_TIF("image/tif", "tif"),
  /** image/tiff. */
  MIME_TIFF("image/tiff", "tif"),
  /** application/xml. */
  MIME_XML("application/xml", "xml"),
  /** application/zip. */
  MIME_ZIP("application/zip", "zip"),
  /** application/vnd.ms-excel. */
  MIME_XLS("application/vnd.ms-excel", "xls"),
  /** application/vnd.ms-excel. */
  MIME_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
  /** application/vnd.ms-powerpoint. */
  MIME_PPT("application/vnd.ms-powerpoint", "ppt"),
  /** application/vnd.openxmlformats-officedocument.presentationml.presentation. */
  MIME_PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
  /** application/javascript. */
  MIME_JS("application/javascript", "js"),
  /** application/x-rar-compressed. */
  MIME_RAR("application/x-rar-compressed", "rar"),
  /** application/gzip. */
  MIME_GZIP("application/gzip", "gz"),
  /** image/svg+xml. */
  MIME_SVG("image/svg+xml", "svg"),
  /** Unknown Mime. */
  MIME_UNKNOWN("UNKNOWN", ""),
  /** image/webp. */
  MIME_WEBP("image/webp", "webp");

  /**
   * Find {@link MimeType} from Content-Type.
   * 
   * @param ct {@link String}
   * @return {@link MimeType}
   */
  public static MimeType fromContentType(final String ct) {

    MimeType type = MimeType.MIME_UNKNOWN;

    if (!isEmpty(ct)) {
      for (MimeType mt : MimeType.values()) {
        if (ct.equals(mt.getContentType())) {
          type = mt;
          break;
        }
      }
    }

    return type;
  }

  /**
   * Find {@link MimeType} from Extension.
   *
   * @param ext {@link String}
   * @return {@link MimeType}
   */
  public static MimeType fromExtension(final String ext) {

    MimeType type = MimeType.MIME_UNKNOWN;

    if (!isEmpty(ext)) {
      for (MimeType mt : MimeType.values()) {
        if (ext.equals(mt.getExtension())) {
          type = mt;
          break;
        }
      }
    }

    return type;
  }

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
  private final String contentType;
  /** Extension. */
  private final String extension;

  /**
   * constructor.
   * 
   * @param type {@link String}
   * @param ext {@link String}
   */
  MimeType(final String type, final String ext) {

    this.contentType = type;
    this.extension = ext;
  }

  /**
   * Find {@link MimeType} by Path.
   * 
   * @param path {@link String}
   * @return MimeType
   */
  public static MimeType findByPath(final String path) {
    String ext = Strings.getExtension(path);
    return MimeType.fromExtension(ext);
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
    return this.extension;
  }
}
