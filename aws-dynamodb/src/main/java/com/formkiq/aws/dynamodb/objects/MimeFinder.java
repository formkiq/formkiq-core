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

import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_DOCX;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_HTML;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_JPEG;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_JSON;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_PDF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_PNG;
import java.util.Set;

/**
 * Find Mime Types.
 *
 */
public class MimeFinder {

  /** Supported Conversions. */
  private static final Set<String> CONVERSIONS = Set.of(MIME_DOCX.name() + MIME_PDF.name(),
      MIME_HTML.name() + MIME_PDF.name(), MIME_JPEG.name() + MIME_JSON.name(),
      MIME_PNG.name() + MIME_JSON.name(), MIME_PDF.name() + MIME_JSON.name());

  /**
   * Find {@link MimeType}.
   * 
   * @param s {@link String}
   * @return {@link MimeType}
   */
  public static MimeType find(final String s) {

    MimeType type = MimeType.MIME_UNKNOWN;

    for (MimeType m : MimeType.values()) {
      if (s != null && m.getContentType().equalsIgnoreCase(s)) {
        type = m;
      }
    }

    return type;
  }

  /**
   * Whether conversion is supported.
   * 
   * @param source {@link MimeType}
   * @param dest {@link MimeType}
   * @return boolean
   */
  public static boolean isSupported(final MimeType source, final MimeType dest) {
    return CONVERSIONS.contains(source.name() + dest.name());
  }
}
