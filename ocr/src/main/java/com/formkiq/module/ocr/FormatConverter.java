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
package com.formkiq.module.ocr;

import java.io.File;
import java.io.IOException;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/**
 * Interface for File Conversion.
 */
public interface FormatConverter {

  /**
   * Convert File.
   * 
   * @param awsServices {@link AwsServiceCache}
   * @param sqsMessage {@link OcrSqsMessage}
   * @param file {@link File}
   * @return {@link String}
   * @throws IOException IOException
   */
  String convert(AwsServiceCache awsServices, OcrSqsMessage sqsMessage, File file)
      throws IOException;

  /**
   * Is {@link MimeType} supported in converter.
   * 
   * @param sqsMessage {@link OcrSqsMessage}
   * @param mineType {@link MimeType}
   * @return boolean
   */
  boolean isSupported(OcrSqsMessage sqsMessage, MimeType mineType);
}
