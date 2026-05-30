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
package com.formkiq.module.ocr.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.IoUtils;

class PdfPortfolioTest {

  /** {@link PdfPortfolio}. */
  private PdfPortfolio pdf = new PdfPortfolio();

  /**
   * Test PDF Portfolio.
   */
  @Test
  void testGetPdfPortfolioByteStreams01() throws Exception {
    // given
    try (InputStream is = getClass().getResourceAsStream("/portfolio.pdf")) {

      byte[] bytes = IoUtils.toByteArray(is);

      try (PDDocument doc = Loader.loadPDF(bytes)) {

        // when
        List<Map<String, Object>> pdfEmbeddedFiles = this.pdf.getPdfEmbeddedFiles(doc);

        // then
        assertTrue(this.pdf.isPdfPortfolio(doc));

        assertEquals(2, pdfEmbeddedFiles.size());
        assertEquals("pdf-test.pdf", pdfEmbeddedFiles.get(0).get("fileName"));
        assertEquals("pdf-test2.pdf", pdfEmbeddedFiles.get(1).get("fileName"));
      }
    }
  }
}
