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

import java.util.concurrent.ExecutorService;

import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.events.SqsEvent;
import com.formkiq.aws.sqs.events.SqsEventBuilder;
import com.formkiq.aws.sqs.events.SqsEventRecord;
import com.formkiq.module.lambda.ocr.tesseract.OcrTesseractProcessor;
import com.formkiq.module.lambdaservices.AwsServiceCache;

import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;

/**
 * Local SQS implementation that invokes the OCR processor in-process.
 */
final class LocalOcrSqsService implements SqsService {

  /** {@link AwsServiceCache}. */
  private final AwsServiceCache awsServices;
  /** {@link ExecutorService}. */
  private final ExecutorService executorService;
  /** {@link OcrTesseractProcessor}. */
  private final OcrTesseractProcessor processor;

  /**
   * constructor.
   *
   * @param services {@link AwsServiceCache}
   * @param ocrProcessor {@link OcrTesseractProcessor}
   * @param executor {@link ExecutorService}
   */
  LocalOcrSqsService(final AwsServiceCache services, final OcrTesseractProcessor ocrProcessor,
      final ExecutorService executor) {
    this.awsServices = services;
    this.processor = ocrProcessor;
    this.executorService = executor;
  }

  @Override
  public AddPermissionResponse addPermission(final AddPermissionRequest request) {
    throw unsupported();
  }

  @Override
  public void clearQueue(final String queueUrl) {
    throw unsupported();
  }

  @Override
  public CreateQueueResponse createQueue(final CreateQueueRequest request) {
    return CreateQueueResponse.builder().queueUrl(request.queueName()).build();
  }

  @Override
  public CreateQueueResponse createQueue(final String queueName) {
    return CreateQueueResponse.builder().queueUrl(queueName).build();
  }

  @Override
  public void deleteMessage(final String queueUrl, final String receiptHandle) {
    throw unsupported();
  }

  @Override
  public DeleteQueueResponse deleteQueue(final String queueUrl) {
    throw unsupported();
  }

  @Override
  public boolean exists(final String queueName) {
    return true;
  }

  @Override
  public String getQueueArn(final String queueUrl) {
    return queueUrl;
  }

  @Override
  public ListQueuesResponse listQueues(final String queueNamePrefix) {
    return ListQueuesResponse.builder().queueUrls(queueNamePrefix).build();
  }

  @Override
  public ReceiveMessageResponse receiveMessages(final String queueUrl) {
    throw unsupported();
  }

  @Override
  public ReceiveMessageResponse receiveMessages(final String queueUrl,
      final int maxNumberOfMessages) {
    throw unsupported();
  }

  @Override
  public SendMessageResponse sendMessage(final String queueUrl, final String message) {
    SqsEvent event = SqsEventBuilder.builder().record().body(message).eventSourceArn(queueUrl)
        .awsRegion(this.awsServices.region().id()).add().build();
    SqsEventRecord record = event.records().get(0);

    this.executorService.submit(() -> this.processor.handleSqsRequest(this.awsServices.getLogger(),
        this.awsServices, record));

    return SendMessageResponse.builder().messageId(record.messageId()).build();
  }

  @Override
  public SetQueueAttributesResponse setQueueAttributes(final SetQueueAttributesRequest request) {
    throw unsupported();
  }

  private UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("Local OCR SQS only supports sendMessage");
  }
}
