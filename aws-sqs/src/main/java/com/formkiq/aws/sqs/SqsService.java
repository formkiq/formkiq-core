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

import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;

/**
 * SQS Service.
 *
 */
public interface SqsService {

  /**
   * Add Permission.
   * 
   * @param request {@link AddPermissionRequest}
   * @return {@link AddPermissionResponse}
   */
  AddPermissionResponse addPermission(AddPermissionRequest request);

  /**
   * Create SQS Queue.
   * 
   * @param request {@link CreateQueueRequest}
   * @return {@link CreateQueueResponse}
   */
  CreateQueueResponse createQueue(CreateQueueRequest request);

  /**
   * Create SQS Queue.
   * 
   * @param queueName {@link String}
   * @return {@link CreateQueueResponse}
   */
  CreateQueueResponse createQueue(String queueName);

  /**
   * Delete SQS Message.
   * 
   * @param queueUrl {@link String}
   * @param receiptHandle {@link String}
   */
  void deleteMessage(String queueUrl, String receiptHandle);

  /**
   * Delete SQS Queue.
   * 
   * @param queueUrl {@link String}
   * @return {@link DeleteQueueResponse}
   */
  DeleteQueueResponse deleteQueue(String queueUrl);

  /**
   * Whether SQS Queue exists.
   * 
   * @param queueName {@link String}
   * @return boolean
   */
  boolean exists(String queueName);

  /**
   * Get SQS Queue Arn.
   * 
   * @param queueUrl {@link String}
   * @return {@link GetQueueAttributesResponse}
   */
  String getQueueArn(String queueUrl);

  /**
   * List SQS Queues by Prefix.
   * 
   * @param queueNamePrefix {@link String}
   * @return {@link ListQueuesResponse}
   */
  ListQueuesResponse listQueues(String queueNamePrefix);

  /**
   * Receives SQS Messages from a queueUrl.
   * 
   * @param queueUrl {@link String}
   * @return {@link ReceiveMessageResponse}
   */
  ReceiveMessageResponse receiveMessages(String queueUrl);

  /**
   * Send Message to SQS.
   * 
   * @param queueUrl {@link String}
   * @param message {@link String}
   * @return {@link SendMessageResponse}
   */
  SendMessageResponse sendMessage(String queueUrl, String message);

  /**
   * Set Queue Attributes.
   * 
   * @param request {@link SetQueueAttributesRequest}
   * @return {@link SetQueueAttributesResponse}
   */
  SetQueueAttributesResponse setQueueAttributes(SetQueueAttributesRequest request);
}
