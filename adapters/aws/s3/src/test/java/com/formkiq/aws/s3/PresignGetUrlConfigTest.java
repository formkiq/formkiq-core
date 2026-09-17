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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Tests for safe presigned download disposition. */
public class PresignGetUrlConfigTest {

  /** Reading the header does not discard the original inline preference. */
  @Test
  public void testContentTypeChangesAfterReadingDisposition() {
    PresignGetUrlConfig config = new PresignGetUrlConfig()
        .contentDispositionByPath("image.png", true).contentType("image/png");
    assertEquals("inline; filename*=UTF-8''image.png", config.contentDisposition());

    config.contentType("image/svg+xml");
    assertEquals("attachment; filename*=UTF-8''image.png", config.contentDisposition());

    config.contentType("image/png");
    assertEquals("inline; filename*=UTF-8''image.png", config.contentDisposition());
  }

  /** The most recent disposition setter determines how the header is constructed. */
  @Test
  public void testDispositionSetterPrecedence() {
    PresignGetUrlConfig config = new PresignGetUrlConfig().contentType("application/pdf")
        .contentDisposition("attachment; filename=old.pdf")
        .contentDispositionByPath("new report.pdf", true);
    assertEquals("inline; filename*=UTF-8''new report.pdf", config.contentDisposition());

    config.contentDisposition("attachment; filename=explicit.pdf");
    assertEquals("attachment; filename=explicit.pdf", config.contentDisposition());

    config.contentDispositionByPath(null, true);
    assertNull(config.contentDisposition());
  }

  /** Existing inline and attachment behavior is preserved for other document types. */
  @Test
  public void testOtherContentTypesPreserveDisposition() {
    for (String contentType : Arrays.asList("application/pdf", "image/png")) {
      for (boolean inline : Arrays.asList(true, false)) {
        PresignGetUrlConfig config = new PresignGetUrlConfig().contentType(contentType)
            .contentDispositionByPath("document", inline);
        assertEquals((inline ? "inline" : "attachment") + "; filename*=UTF-8''document",
            config.contentDisposition());
      }
    }
    assertNull(new PresignGetUrlConfig().contentDisposition());
  }

  /** SVG MIME types force attachments regardless of setter order or filename. */
  @Test
  public void testSvgContentTypeForcesAttachment() {
    for (String contentType : Arrays.asList("image/svg+xml", "IMAGE/SVG+XML",
        " image/SVG+xml ; charset=utf-8")) {
      PresignGetUrlConfig config = new PresignGetUrlConfig().contentType(contentType)
          .contentDispositionByPath("image.png", true);
      assertEquals("attachment; filename*=UTF-8''image.png", config.contentDisposition());

      config = new PresignGetUrlConfig().contentDispositionByPath("image.png", true)
          .contentType(contentType);
      assertEquals("attachment; filename*=UTF-8''image.png", config.contentDisposition());
    }
  }

  /** SVG MIME types force attachments even with an explicit disposition or missing filename. */
  @Test
  public void testSvgExplicitDispositionAndMissingFilename() {
    PresignGetUrlConfig config = new PresignGetUrlConfig().contentType("image/svg+xml");
    assertEquals("attachment", config.contentDisposition());
    config.contentDisposition("inline; filename=diagram.svg");
    assertEquals("attachment; filename=diagram.svg", config.contentDisposition());
  }

  /** SVG filenames force attachments even when the MIME type is absent or misleading. */
  @Test
  public void testSvgFilenameForcesAttachment() {
    for (String contentType : Arrays.asList(null, "", "image/png", "application/octet-stream")) {
      for (String filename : Arrays.asList("image.svg", "image.SVG")) {
        PresignGetUrlConfig config = new PresignGetUrlConfig().contentType(contentType)
            .contentDispositionByPath(filename, true);
        assertEquals("attachment; filename*=UTF-8''" + filename, config.contentDisposition());

        config = new PresignGetUrlConfig().contentDispositionByPath(filename, true)
            .contentType(contentType);
        assertEquals("attachment; filename*=UTF-8''" + filename, config.contentDisposition());
      }
    }
  }
}
