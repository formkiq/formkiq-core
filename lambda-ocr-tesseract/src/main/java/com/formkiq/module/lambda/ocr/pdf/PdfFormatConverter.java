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
package com.formkiq.module.lambda.ocr.pdf;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.ocr.FormatConverter;
import com.formkiq.module.ocr.FormatConverterResult;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.module.ocr.OcrSqsMessage;
import com.formkiq.module.ocr.pdf.PdfPortfolio;

/**
 * PDF {@link FormatConverter}.
 */
public class PdfFormatConverter implements FormatConverter {

  /** {@link PdfPortfolio}. */
  private PdfPortfolio pdfPortfolio = new PdfPortfolio();

  @Override
  public FormatConverterResult convert(final AwsServiceCache awsServices,
      final OcrSqsMessage sqsMessage, final File file) throws IOException {

    StringBuilder sb = new StringBuilder();

    PDFTextStripper pdfTextStripper = new PDFTextStripper();

    try (PDDocument document = Loader.loadPDF(file)) {

      sb.append(pdfTextStripper.getText(document));

      if (this.pdfPortfolio.isPdfPortfolio(document)) {

        List<Map<String, Object>> pdfEmbeddedFiles =
            this.pdfPortfolio.getPdfEmbeddedFiles(document);

        for (Map<String, Object> map : pdfEmbeddedFiles) {
          String filename = map.get("fileName").toString();

          if (filename.endsWith(".pdf")) {
            byte[] data = (byte[]) map.get("data");

            try (PDDocument embeddedDocument = Loader.loadPDF(data)) {
              sb.append(pdfTextStripper.getText(embeddedDocument));
            }
          }
        }

      } else {

        sb.append(pdfTextStripper.getText(document));
      }

      String text = sb.toString();
      return new FormatConverterResult().text(text).status(OcrScanStatus.SUCCESSFUL);
    }
  }

  @Override
  public boolean isSupported(final OcrSqsMessage sqsMessage, final MimeType mineType) {
    return MimeType.MIME_PDF.equals(mineType);
  }
}
