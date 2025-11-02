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
package com.formkiq.testutils.aws.sqs;

import com.formkiq.aws.sqs.SqsService;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.concurrent.TimeUnit;

/**
 * Builder for {@link AddDocumentAttributesRequest}.
 */
public class SqsMessageReceiver {

  /** Retry Limit. */
  private static final int RETRY_LIMIT = 10;
  /** {@link SqsService}. */
  private final SqsService sqs;
  /** Sqs Queue Url. */
  private final String queueUrl;

  /**
   * constructor.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   * @param sqsQueueUrl {@link String}
   */
  public SqsMessageReceiver(final AwsServiceCache awsServiceCache, final String sqsQueueUrl) {
    this(awsServiceCache.getExtension(SqsService.class), sqsQueueUrl);
  }

  /**
   * constructor.
   *
   * @param sqsService {@link SqsService}
   * @param sqsQueueUrl {@link String}
   */
  public SqsMessageReceiver(final SqsService sqsService, final String sqsQueueUrl) {
    this.sqs = sqsService;
    this.queueUrl = sqsQueueUrl;
  }

  /**
   * Clears all messages in queue.
   */
  public void clear() {
    ReceiveMessageResponse response = sqs.receiveMessages(queueUrl);
    response.messages().forEach(m -> sqs.deleteMessage(queueUrl, m.receiptHandle()));
  }

  /**
   * Get Messages.
   *
   * @return {@link ReceiveMessageResponse}
   * @throws InterruptedException InterruptedException
   */
  public ReceiveMessageResponse get() throws InterruptedException {
    int retry = 0;
    ReceiveMessageResponse response = sqs.receiveMessages(queueUrl);
    while (response.messages().isEmpty()) {
      TimeUnit.SECONDS.sleep(1);
      response = sqs.receiveMessages(queueUrl);
      retry++;
      if (retry > RETRY_LIMIT) {
        throw new RuntimeException("Timeout waiting for SQS message");
      }
    }

    response.messages().forEach(m -> sqs.deleteMessage(queueUrl, m.receiptHandle()));
    return response;
  }
}
