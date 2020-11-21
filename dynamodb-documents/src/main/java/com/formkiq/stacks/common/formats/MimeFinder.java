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

import static com.formkiq.stacks.common.formats.MimeType.MIME_DOCX;
import static com.formkiq.stacks.common.formats.MimeType.MIME_HTML;
import static com.formkiq.stacks.common.formats.MimeType.MIME_JPEG;
import static com.formkiq.stacks.common.formats.MimeType.MIME_JSON;
import static com.formkiq.stacks.common.formats.MimeType.MIME_PDF;
import static com.formkiq.stacks.common.formats.MimeType.MIME_PNG;
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
