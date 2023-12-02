package com.formkiq.module.ocr.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

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

      try (PDDocument doc = PDDocument.load(is)) {

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
