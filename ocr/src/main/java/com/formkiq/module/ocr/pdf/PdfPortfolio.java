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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.itextpdf.kernel.pdf.PdfCatalog;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNameTree;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;

/**
 * 
 * Helper methods for working with Pdf Portfolio.
 *
 */
public class PdfPortfolio {

  /**
   * Get PDF Embbeded File Names.
   * 
   * @param pdfDoc {@link PdfDocument}
   * @return {@link Map} {@link PdfObject}
   */
  private static Map<String, PdfObject> getEmbeddedFileNames(final PdfDocument pdfDoc) {
    PdfCatalog catalog = pdfDoc.getCatalog();
    PdfNameTree embeddedFiles = catalog.getNameTree(PdfName.EmbeddedFiles);
    Map<String, PdfObject> names = embeddedFiles.getNames();
    return names;
  }

  /**
   * Get {@link List} of Pdf Portfolio Byte Streams.
   * 
   * @param pdfDoc {@link PdfDocument}
   * @return {@link List}
   */
  static List<byte[]> getPdfPortfolioByteStreams(final PdfDocument pdfDoc) {

    Map<String, PdfObject> names = getEmbeddedFileNames(pdfDoc);

    List<byte[]> list = new ArrayList<>();
    for (Entry<String, PdfObject> entry : names.entrySet()) {

      if (entry.getValue() instanceof PdfDictionary) {

        PdfDictionary filespecDict = (PdfDictionary) entry.getValue();
        PdfDictionary embeddedFileDict = filespecDict.getAsDictionary(PdfName.EF);
        PdfStream stream = embeddedFileDict.getAsStream(PdfName.UF);
        if (stream == null) {
          stream = embeddedFileDict.getAsStream(PdfName.F);
        }

        if (stream != null) {
          list.add(stream.getBytes());
        }
      }
    }

    return list;
  }

  /**
   * Is Pdf Portfolio {@link PdfDocument}.
   * 
   * @param doc {@link PdfDocument}
   * @return boolean
   */
  public static boolean isPdfPortfolio(final PdfDocument doc) {
    Map<String, PdfObject> names = getEmbeddedFileNames(doc);
    return !names.isEmpty();
  }

  /**
   * Merge Multiple PDF Documents into a single {@link PdfDocument}.
   * 
   * @param doc {@link PdfDocument}
   * @param os {@link OutputStream}
   * @throws IOException IOException
   */
  public static void mergePdfPortfolios(final PdfDocument doc, final OutputStream os)
      throws IOException {

    try (PdfWriter writer = new PdfWriter(os)) {

      try (PdfDocument document = new PdfDocument(writer)) {
        PdfMerger merger = new PdfMerger(document);

        List<byte[]> streams = getPdfPortfolioByteStreams(doc);
        for (byte[] bytes : streams) {
          InputStream is = new ByteArrayInputStream(bytes);

          try (PdfReader reader = new PdfReader(is);
              PdfReader setUnethicalReading = reader.setUnethicalReading(true)) {

            try (PdfDocument pdf = new PdfDocument(reader)) {

              merger.merge(pdf, 1, pdf.getNumberOfPages());
            }
          }
        }
      }
    }
  }
}
