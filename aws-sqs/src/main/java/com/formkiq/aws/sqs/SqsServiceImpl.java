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
package com.formkiq.aws.sqs;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;

/**
 * Implementation of {@link SqsService}.
 */
public class SqsServiceImpl implements SqsService {

  /** {@link SqsClient}. */
  private SqsClient sqsClient;

  /**
   * constructor.
   * 
   * @param connection {@link SqsConnectionBuilder}
   * 
   */
  public SqsServiceImpl(final SqsConnectionBuilder connection) {
    this.sqsClient = connection.build();
  }

  @Override
  public AddPermissionResponse addPermission(final AddPermissionRequest request) {
    return this.sqsClient.addPermission(request);
  }

  @Override
  public CreateQueueResponse createQueue(final CreateQueueRequest request) {
    return this.sqsClient.createQueue(request);
  }

  @Override
  public CreateQueueResponse createQueue(final String queueName) {
    return createQueue(CreateQueueRequest.builder().queueName(queueName).build());
  }

  @Override
  public void deleteMessage(final String queueUrl, final String receiptHandle) {
    this.sqsClient.deleteMessage(
        DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
  }

  @Override
  public DeleteQueueResponse deleteQueue(final String queueUrl) {
    return this.sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
  }

  @Override
  public boolean exists(final String queueName) {
    ListQueuesResponse response =
        this.sqsClient.listQueues(ListQueuesRequest.builder().queueNamePrefix(queueName).build());
    return response.queueUrls().stream().filter(q -> q.equals(queueName)).findFirst().isPresent();
  }

  @Override
  public String getQueueArn(final String queueUrl) {
    return this.sqsClient
        .getQueueAttributes(GetQueueAttributesRequest.builder()
            .attributeNamesWithStrings("QueueArn").queueUrl(queueUrl).build())
        .attributesAsStrings().get("QueueArn");
  }

  @Override
  public ListQueuesResponse listQueues(final String queueNamePrefix) {
    return this.sqsClient
        .listQueues(ListQueuesRequest.builder().queueNamePrefix(queueNamePrefix).build());
  }

  @Override
  public ReceiveMessageResponse receiveMessages(final String queueUrl) {
    ReceiveMessageResponse response =
        this.sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build());

    return response;
  }

  @Override
  public SendMessageResponse sendMessage(final String queueUrl, final String message) {
    SendMessageResponse response = this.sqsClient
        .sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build());
    return response;
  }

  @Override
  public SetQueueAttributesResponse setQueueAttributes(final SetQueueAttributesRequest request) {
    return this.sqsClient.setQueueAttributes(request);
  }
}
