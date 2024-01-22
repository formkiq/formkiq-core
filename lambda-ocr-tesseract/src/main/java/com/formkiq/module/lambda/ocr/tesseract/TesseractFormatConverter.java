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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
  private TesseractWrapper tesseract;

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
      final OcrSqsMessage sqsMessage, final File file) throws IOException {
    try {
      String text = this.tesseract.doOcr(file);
      return new FormatConverterResult().text(text).status(OcrScanStatus.SUCCESSFUL);
    } catch (TesseractException e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean isSupported(final OcrSqsMessage sqsMessage, final MimeType mineType) {
    return SUPPORTED.contains(mineType);
  }
}
