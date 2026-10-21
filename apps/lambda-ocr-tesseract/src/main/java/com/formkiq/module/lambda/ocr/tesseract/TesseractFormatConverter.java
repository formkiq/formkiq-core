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
package com.formkiq.module.lambda.ocr.tesseract;

import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_BMP;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_GIF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_JPEG;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_JPG;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_PDF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_PNG;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_TIF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_TIFF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_WEBP;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.pdfbox.text.PDFTextStripper;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.ocr.FormatConverter;
import com.formkiq.module.ocr.FormatConverterResult;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.module.ocr.OcrSqsMessage;
import net.sourceforge.tess4j.TesseractException;

/**
 * Tesseract {@link FormatConverter}.
 */
public class TesseractFormatConverter implements FormatConverter {

  /** Supported Mime Type. */
  private static final List<MimeType> SUPPORTED = Arrays.asList(MIME_PNG, MIME_JPEG, MIME_JPG,
      MIME_TIF, MIME_TIFF, MIME_GIF, MIME_WEBP, MIME_BMP, MIME_PDF);

  /** {@link TesseractWrapper}. */
  private final TesseractWrapper tesseract;

  /**
   * constructor.
   * 
   * @param tesseractWrapper {@link TesseractWrapper}
   */
  public TesseractFormatConverter(final TesseractWrapper tesseractWrapper) {
    this.tesseract = tesseractWrapper;
  }

  @Override
  public FormatConverterResult convert(final AwsServiceCache awsServices,
      final OcrSqsMessage sqsMessage, final MimeType mimeType, final File file) throws IOException {

    String text = null;

    int numberOfPages = getOcrNumberOfPages(sqsMessage);

    if (numberOfPages > 0 && isSupportMimeType(mimeType)) {

      if (MimeType.MIME_PDF.equals(mimeType)) {

        PDFTextStripper s = new PDFTextStripper();
        s.setStartPage(0);
        s.setEndPage(numberOfPages);

      } else {

        text = getTiffText(file, numberOfPages);
      }

    } else {

      try {
        text = this.tesseract.doOcr(file);
      } catch (TesseractException e) {
        throw new IOException(e);
      }
    }

    return new FormatConverterResult().text(text).status(OcrScanStatus.SUCCESSFUL);
  }

  private int getOcrNumberOfPages(final OcrSqsMessage sqsMessage) {
    try {
      String numberOfPages =
          sqsMessage.request() != null ? sqsMessage.request().getOcrNumberOfPages() : "-1";
      return Integer.parseInt(numberOfPages);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private String getTiffText(final File file, final int numberOfPages) throws IOException {

    try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {

      Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

      if (!readers.hasNext()) {
        throw new IOException("No image reader found for the specified format");
      }

      ImageReader reader = readers.next();
      reader.setInput(iis);

      try {

        List<String> texts = new ArrayList<>();
        for (int i = 0; i < numberOfPages; i++) {
          BufferedImage image = reader.read(i);
          texts.add(this.tesseract.doOcr(image));
        }

        return String.join("\n", texts);

      } catch (TesseractException e) {
        throw new IOException(e);
      }
    }
  }

  @Override
  public boolean isSupported(final OcrSqsMessage sqsMessage, final MimeType mineType) {
    return SUPPORTED.contains(mineType);
  }

  private boolean isSupportMimeType(final MimeType mt) {
    return MimeType.MIME_TIF.equals(mt) || MimeType.MIME_TIFF.equals(mt)
        || MimeType.MIME_PDF.equals(mt);
  }
}
