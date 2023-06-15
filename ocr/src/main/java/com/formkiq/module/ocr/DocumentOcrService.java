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

import java.util.List;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/**
 * 
 * Document OCR Service.
 *
 */
public interface DocumentOcrService {

  /** Prefix Temp File. */
  String PREFIX_TEMP_FILES = "tempfiles/";

  /**
   * Optical character recognition of Document.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param request {@link OcrRequest}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param userId {@link String}
   */
  void convert(LambdaLogger logger, AwsServiceCache awsservice, OcrRequest request, String siteId,
      String documentId, String userId);

  /**
   * Delete Document OCR.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void delete(String siteId, String documentId);

  /**
   * Get Document OCR.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link DynamicObject}
   */
  DynamicObject get(String siteId, String documentId);

  /**
   * Get / Find OCR Document S3 Key.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param jobId {@link String}
   * @return {@link List} {@link String}
   */
  List<String> getOcrS3Keys(String siteId, String documentId, String jobId);

  /**
   * Get S3 Key for OCR document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param jobId {@link String}
   * @return {@link String}
   */
  String getS3Key(String siteId, String documentId, String jobId);

  /**
   * Save {@link Ocr}.
   * 
   * @param ocr {@link Ocr}
   */
  void save(Ocr ocr);

  /**
   * Set Optical character recognition of Document.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param userId {@link String}
   * @param content {@link String}
   * @param contentType {@link String}
   */
  void set(AwsServiceCache awsservice, String siteId, String documentId, String userId,
      String content, String contentType);

  /**
   * Converts OCR to Raw text.
   * 
   * @param content {@link String}
   * @return {@link String}
   */
  String toText(String content);

  /**
   * Update OCR Scan Status and call next action.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param status {@link OcrScanStatus}
   */
  void updateOcrScanStatus(AwsServiceCache awsservice, String siteId, String documentId,
      OcrScanStatus status);

  /**
   * Update OCR Scan Status.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param status {@link OcrScanStatus}
   */
  void updateOcrScanStatus(String siteId, String documentId, OcrScanStatus status);
}
