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
package com.formkiq.aws.sns;

import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionResponse;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeResponse;

import java.util.Map;

/**
 * SNS Service.
 *
 */
public interface SnsService {


  /**
   * Confirm SNS Subscription.
   * 
   * @param topicArn {@link String}
   * @param token {@link String}
   * @return {@link ConfirmSubscriptionResponse}
   */
  ConfirmSubscriptionResponse confirmSubscription(String topicArn, String token);

  /**
   * Create SNS Topic.
   * 
   * @param topicName {@link String}
   * @return {@link CreateTopicResponse}
   */
  CreateTopicResponse createTopic(String topicName);

  /**
   * Get a List of SNS Topics.
   * 
   * @param nextToken {@link String}
   * @return {@link ListTopicsResponse}
   */
  ListTopicsResponse listTopics(String nextToken);

  /**
   * Publish Message to SNS Topic.
   * 
   * @param topicArn {@link String}
   * @param message {@link String}
   * @param messageAttributes {@link Map}
   * @return {@link PublishResponse}
   */
  PublishResponse publish(String topicArn, String message,
      Map<String, MessageAttributeValue> messageAttributes);

  /**
   * Subscribe to SNS Topic.
   * 
   * @param topicArn {@link String}
   * @param protocol {@link String}
   * @param endpoint {@link String}
   * @return {@link SubscribeResponse}
   */
  SubscribeResponse subscribe(String topicArn, String protocol, String endpoint);

  /**
   * Deletes a subscription.
   * 
   * @param subscriptionArn {@link String}
   * @return {@link UnsubscribeResponse}
   */
  UnsubscribeResponse unsubscribe(String subscriptionArn);

  /**
   * Unsubscribe all subscriptions.
   */
  void unsubscribeAll();
}
