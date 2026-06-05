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
package com.formkiq.server;

import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.events.SqsEvent;
import com.formkiq.aws.sqs.events.SqsEventBuilder;
import com.formkiq.aws.sqs.events.SqsEventRecord;
import com.formkiq.module.lambda.ocr.tesseract.OcrTesseractProcessor;
import com.formkiq.module.lambdaservices.AwsServiceCache;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Polls a local SQS-compatible queue and invokes the OCR processor in-process.
 */
final class LocalOcrSqsPoller implements Runnable {

  /** Maximum messages to read per poll. */
  private static final int MAX_MESSAGES = 10;

  /** {@link AwsServiceCache}. */
  private final AwsServiceCache awsServices;
  /** {@link OcrTesseractProcessor}. */
  private final OcrTesseractProcessor processor;
  /** OCR queue URL. */
  private final String queueUrl;
  /** {@link SqsService}. */
  private final SqsService sqsService;

  /**
   * constructor.
   *
   * @param services {@link AwsServiceCache}
   * @param sqs {@link SqsService}
   * @param ocrProcessor {@link OcrTesseractProcessor}
   * @param ocrQueueUrl {@link String}
   */
  LocalOcrSqsPoller(final AwsServiceCache services, final SqsService sqs,
      final OcrTesseractProcessor ocrProcessor, final String ocrQueueUrl) {
    this.awsServices = services;
    this.sqsService = sqs;
    this.processor = ocrProcessor;
    this.queueUrl = ocrQueueUrl;
  }

  @Override
  public void run() {
    try {
      ReceiveMessageResponse response =
          this.sqsService.receiveMessages(this.queueUrl, MAX_MESSAGES);

      for (Message message : response.messages()) {
        SqsEvent event = SqsEventBuilder.builder().record().body(message.body())
            .eventSourceArn(this.queueUrl).awsRegion(this.awsServices.region().id()).add().build();
        SqsEventRecord record = event.records().getFirst();

        this.processor.handleSqsRequest(this.awsServices.getLogger(), this.awsServices, record);
        this.sqsService.deleteMessage(this.queueUrl, message.receiptHandle());
      }
    } catch (Exception e) {
      this.awsServices.getLogger().error(e);
    }
  }
}
