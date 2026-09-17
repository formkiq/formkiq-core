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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 
 * Configuration Options for Presigned Get Url.
 *
 */
public class PresignGetUrlConfig {

  /** Sets the <code>Content-Disposition</code> header of the response. */
  private String contentDisposition;
  /** Sets the <code>Content-Type</code> header of the response. */
  private String contentType;
  /** Path used for the download filename. */
  private String downloadPath;
  /** Whether inline delivery was requested. */
  private boolean inlineDelivery;

  /**
   * constructor.
   */
  public PresignGetUrlConfig() {

  }

  /**
   * Get Content Disposition, forcing attachment delivery for SVG documents.
   * 
   * @return {@link String}
   */
  public String contentDisposition() {
    String disposition = this.contentDisposition;
    if (this.downloadPath != null) {
      String encodedPath =
          URLEncoder.encode(this.downloadPath, StandardCharsets.UTF_8).replaceAll("\\+", " ");
      disposition = String.format("%s; filename*=UTF-8''%s",
          this.inlineDelivery ? "inline" : "attachment", encodedPath);
    }

    if (isSvg(this.contentType, this.downloadPath)) {
      int parameters = disposition != null ? disposition.indexOf(';') : -1;
      disposition = "attachment" + (parameters >= 0 ? disposition.substring(parameters) : "");
    }
    return disposition;
  }

  /**
   * Set Content Disposition.
   * 
   * @param contentDispositionString {@link String}
   * @return {@link PresignGetUrlConfig}
   */
  public PresignGetUrlConfig contentDisposition(final String contentDispositionString) {
    this.contentDisposition = contentDispositionString;
    this.downloadPath = null;
    return this;
  }

  /**
   * Set Content Disposition By Path.
   * 
   * @param path {@link String}
   * @param inline boolean
   * @return {@link PresignGetUrlConfig}
   */
  public PresignGetUrlConfig contentDispositionByPath(final String path, final boolean inline) {

    this.downloadPath = path;
    this.inlineDelivery = inline;
    this.contentDisposition = null;
    return this;
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
   * @param contentTypeString {@link String}
   * @return {@link PresignGetUrlConfig}
   */
  public PresignGetUrlConfig contentType(final String contentTypeString) {
    this.contentType = contentTypeString;
    return this;
  }

  private boolean isSvg(final String type, final String path) {
    String mediaType = type != null ? type.split(";", 2)[0].trim() : "";
    String filename = path != null ? path.trim() : "";
    String extension = ".svg";
    return "image/svg+xml".equalsIgnoreCase(mediaType) || filename.regionMatches(true,
        filename.length() - extension.length(), extension, 0, extension.length());
  }

}
