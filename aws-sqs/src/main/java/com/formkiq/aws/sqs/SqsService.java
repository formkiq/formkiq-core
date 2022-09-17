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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;

/**
 * SQS Service.
 *
 */
public class SqsService {

  /**
   * constructor.
   * 
   */
  public SqsService() {}

  /**
   * Add Permission.
   * 
   * @param sqsClient {@link SqsClient}
   * @param request {@link AddPermissionRequest}
   * @return {@link AddPermissionResponse}
   */
  public AddPermissionResponse addPermission(final SqsClient sqsClient,
      final AddPermissionRequest request) {
    return sqsClient.addPermission(request);
  }

  /**
   * Create SQS Queue.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueName {@link String}
   * @return {@link CreateQueueResponse}
   */
  public CreateQueueResponse createQueue(final SqsClient sqsClient, final String queueName) {
    return createQueue(sqsClient, CreateQueueRequest.builder().queueName(queueName).build());
  }

  /**
   * Create SQS Queue.
   * 
   * @param sqsClient {@link SqsClient}
   * @param request {@link CreateQueueRequest}
   * @return {@link CreateQueueResponse}
   */
  public CreateQueueResponse createQueue(final SqsClient sqsClient,
      final CreateQueueRequest request) {
    return sqsClient.createQueue(request);
  }

  /**
   * Delete SQS Message.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueUrl {@link String}
   * @param receiptHandle {@link String}
   */
  public void deleteMessage(final SqsClient sqsClient, final String queueUrl,
      final String receiptHandle) {
    sqsClient.deleteMessage(
        DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
  }

  /**
   * Delete SQS Queue.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueUrl {@link String}
   * @return {@link DeleteQueueResponse}
   */
  public DeleteQueueResponse deleteQueue(final SqsClient sqsClient, final String queueUrl) {
    return sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
  }

  /**
   * Whether SQS Queue exists.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueName {@link String}
   * @return boolean
   */
  public boolean exists(final SqsClient sqsClient, final String queueName) {
    ListQueuesResponse response =
        sqsClient.listQueues(ListQueuesRequest.builder().queueNamePrefix(queueName).build());
    return response.queueUrls().stream().filter(q -> q.equals(queueName)).findFirst().isPresent();
  }

  /**
   * Get Queue Arn from queueUrl.
   * 
   * @param queueUrl {@link String}
   * @return {@link String}
   */
  public String getQueueArn(final String queueUrl) {
    String queueName = "";
    String region = "";
    String account = "";

    Pattern pattern = Pattern.compile("^(https://)(\\w+)\\.([^\\.]+)[^/]+/([^/]+)/(.*)$");
    Matcher m = pattern.matcher(queueUrl);

    if (m.find()) {
      region = m.group(m.groupCount() - 2);
      account = m.group(m.groupCount() - 1);
      queueName = m.group(m.groupCount());
    }

    return String.format("arn:aws:sqs:%s:%s:%s", region, account, queueName);
  }

  /**
   * Get SQS Queue Attributes.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueUrl {@link String}
   * @return {@link GetQueueAttributesResponse}
   */
  public GetQueueAttributesResponse getQueueAttributes(final SqsClient sqsClient,
      final String queueUrl) {
    return sqsClient
        .getQueueAttributes(GetQueueAttributesRequest.builder().queueUrl(queueUrl).build());
  }

  /**
   * List SQS Queues by Prefix.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueNamePrefix {@link String}
   * @return {@link ListQueuesResponse}
   */
  public ListQueuesResponse listQueues(final SqsClient sqsClient, final String queueNamePrefix) {
    return sqsClient
        .listQueues(ListQueuesRequest.builder().queueNamePrefix(queueNamePrefix).build());
  }

  /**
   * Receives SQS Messages from a queueUrl.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueUrl {@link String}
   * @return {@link ReceiveMessageResponse}
   */
  public ReceiveMessageResponse receiveMessages(final SqsClient sqsClient, final String queueUrl) {
    ReceiveMessageResponse response =
        sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build());

    return response;
  }

  /**
   * Send Message to SQS.
   * 
   * @param sqsClient {@link SqsClient}
   * @param queueUrl {@link String}
   * @param message {@link String}
   * @return {@link SendMessageResponse}
   */
  public SendMessageResponse sendMessage(final SqsClient sqsClient, final String queueUrl,
      final String message) {
    SendMessageResponse response = sqsClient
        .sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build());
    return response;
  }

  /**
   * Set Queue Attributes.
   * 
   * @param sqsClient {@link SqsClient}
   * @param request {@link SetQueueAttributesRequest}
   * @return {@link SetQueueAttributesResponse}
   */
  public SetQueueAttributesResponse setQueueAttributes(final SqsClient sqsClient,
      final SetQueueAttributesRequest request) {
    return sqsClient.setQueueAttributes(request);
  }
}
