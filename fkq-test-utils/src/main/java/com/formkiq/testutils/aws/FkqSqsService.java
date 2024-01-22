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
package com.formkiq.testutils.aws;

import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceImpl;
import software.amazon.awssdk.regions.Region;
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
 * 
 * {@link SqsService} configured specifically for FormKiQ.
 *
 */
public class FkqSqsService implements SqsService {

  /** {@link SqsService}. */
  private SqsService service;
  /** {@link SqsConnectionBuilder}. */
  private SqsConnectionBuilder sqsBuilder;

  /**
   * constructor.
   * 
   * @param awsProfile {@link String}
   * @param awsRegion {@link Region}
   */
  public FkqSqsService(final String awsProfile, final Region awsRegion) {
    this.sqsBuilder =
        new SqsConnectionBuilder(false).setCredentials(awsProfile).setRegion(awsRegion);
    this.service = new SqsServiceImpl(this.sqsBuilder);
  }

  @Override
  public AddPermissionResponse addPermission(final AddPermissionRequest request) {
    return this.service.addPermission(request);
  }

  @Override
  public CreateQueueResponse createQueue(final CreateQueueRequest request) {
    return this.service.createQueue(request);
  }

  @Override
  public CreateQueueResponse createQueue(final String queueName) {
    return this.service.createQueue(queueName);
  }

  @Override
  public void deleteMessage(final String queueUrl, final String receiptHandle) {
    this.service.deleteMessage(queueUrl, receiptHandle);
  }

  @Override
  public DeleteQueueResponse deleteQueue(final String queueUrl) {
    return this.service.deleteQueue(queueUrl);
  }

  @Override
  public boolean exists(final String queueName) {
    return this.service.exists(queueName);
  }

  @Override
  public String getQueueArn(final String queueUrl) {
    return this.service.getQueueArn(queueUrl);
  }

  @Override
  public ListQueuesResponse listQueues(final String queueNamePrefix) {
    return this.service.listQueues(queueNamePrefix);
  }

  @Override
  public ReceiveMessageResponse receiveMessages(final String queueUrl) {
    return this.service.receiveMessages(queueUrl);
  }

  @Override
  public SendMessageResponse sendMessage(final String queueUrl, final String message) {
    return this.service.sendMessage(queueUrl, message);
  }

  @Override
  public SetQueueAttributesResponse setQueueAttributes(final SetQueueAttributesRequest request) {
    return this.service.setQueueAttributes(request);
  }
}
