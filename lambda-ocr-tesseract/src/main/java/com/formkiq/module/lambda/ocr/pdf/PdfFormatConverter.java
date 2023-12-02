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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.text.PDFTextStripper;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.ocr.FormatConverter;
import com.formkiq.module.ocr.FormatConverterResult;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.module.ocr.OcrSqsMessage;

/**
 * DOCX {@link FormatConverter}.
 */
public class PdfFormatConverter implements FormatConverter {

  @Override
  public FormatConverterResult convert(final AwsServiceCache awsServices,
      final OcrSqsMessage sqsMessage, final File file) throws IOException {

    StringBuilder sb = new StringBuilder();

    PDFTextStripper pdfTextStripper = new PDFTextStripper();

    try (PDDocument document = PDDocument.load(file)) {

      if (isPdfPortfolio(document)) {

        List<Map<String, String>> texts = getPortfolioTextMap(pdfTextStripper, document);

        for (Map<String, String> map : texts) {
          sb.append(map.get("text"));
        }

      } else {

        sb.append(pdfTextStripper.getText(document));
      }

      String text = sb.toString();
      return new FormatConverterResult().text(text).status(OcrScanStatus.SUCCESSFUL);
    }
  }

  private List<Map<String, String>> extractFiles(final PDFTextStripper pdfTextStripper,
      final Map<String, PDComplexFileSpecification> names) throws IOException {

    List<Map<String, String>> list = new ArrayList<>();

    for (Entry<String, PDComplexFileSpecification> e : names.entrySet()) {
      String filename = e.getKey();

      PDComplexFileSpecification fileSpec = names.get(filename);
      PDEmbeddedFile embeddedFile = fileSpec.getEmbeddedFile();

      if (filename.endsWith(".pdf")) {
        try (PDDocument document = PDDocument.load(embeddedFile.toByteArray())) {
          list.add(Map.of("fileName", filename, "text", pdfTextStripper.getText(document)));
        }
      }
    }

    return list;
  }

  /**
   * Get {@link Map} of Portfolio and Text.
   * 
   * @param pdfTextStripper {@link PDFTextStripper}
   * @param document {@link PDDocument}
   * @return {@link Map}
   * @throws IOException IOException
   */
  private List<Map<String, String>> getPortfolioTextMap(final PDFTextStripper pdfTextStripper,
      final PDDocument document) throws IOException {

    List<Map<String, String>> list = new ArrayList<>();

    String text = pdfTextStripper.getText(document);
    list.add(Map.of("fileName", "root", "text", text));

    PDDocumentNameDictionary names = new PDDocumentNameDictionary(document.getDocumentCatalog());
    PDEmbeddedFilesNameTreeNode efTree = names.getEmbeddedFiles();

    if (efTree != null) {

      Map<String, PDComplexFileSpecification> namesMap = efTree.getNames();

      if (namesMap != null) {

        list.addAll(extractFiles(pdfTextStripper, namesMap));

      } else {

        List<PDNameTreeNode<PDComplexFileSpecification>> kids = efTree.getKids();
        for (PDNameTreeNode<PDComplexFileSpecification> node : kids) {
          namesMap = node.getNames();
          list.addAll(extractFiles(pdfTextStripper, namesMap));
        }
      }
    }

    return list;
  }

  /**
   * Whether {@link PDDocument} is a Portfolio.
   * 
   * @param document {@link PDDocument}
   * @return boolean
   */
  private boolean isPdfPortfolio(final PDDocument document) {
    PDDocumentCatalog catalog = document.getDocumentCatalog();
    COSDictionary cosObject = catalog.getCOSObject();
    return cosObject.containsKey("Collection");
  }

  @Override
  public boolean isSupported(final OcrSqsMessage sqsMessage, final MimeType mineType) {
    return MimeType.MIME_PDF.equals(mineType);
  }
}
