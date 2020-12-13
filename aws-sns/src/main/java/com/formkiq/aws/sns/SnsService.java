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

import java.util.Map;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionResponse;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeResponse;

/**
 * SNS Service.
 *
 */
public class SnsService {

  /** SnsClient. */
  private SnsClient snsClient;

  /**
   * constructor.
   * 
   * @param builder {@link SnsConnectionBuilder}
   */
  public SnsService(final SnsConnectionBuilder builder) {
    this.snsClient = builder.build();
  }

  /**
   * Confirm SNS Subscription.
   * 
   * @param topicArn {@link String}
   * @param token {@link String}
   * @return {@link ConfirmSubscriptionResponse}
   */
  public ConfirmSubscriptionResponse confirmSubscription(final String topicArn,
      final String token) {
    return this.snsClient.confirmSubscription(
        ConfirmSubscriptionRequest.builder().topicArn(topicArn).token(token).build());
  }

  /**
   * Create SNS Topic.
   * 
   * @param topicName {@link String}
   * @return {@link CreateTopicResponse}
   */
  public CreateTopicResponse createTopic(final String topicName) {
    return this.snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build());
  }

  /**
   * Get a List of SNS Topics.
   * 
   * @param nextToken {@link String}
   * @return {@link ListTopicsResponse}
   */
  public ListTopicsResponse listTopics(final String nextToken) {
    return this.snsClient.listTopics(ListTopicsRequest.builder().nextToken(nextToken).build());
  }

  /**
   * Publish Message to SNS Topic.
   * 
   * @param topicArn {@link String}
   * @param message {@link String}
   * @param messageAttributes {@link Map}
   * @return {@link PublishResponse}
   */
  public PublishResponse publish(final String topicArn, final String message,
      final Map<String, MessageAttributeValue> messageAttributes) {
    return this.snsClient.publish(PublishRequest.builder().topicArn(topicArn).message(message)
        .messageAttributes(messageAttributes).build());
  }

  /**
   * Subscribe to SNS Topic.
   * 
   * @param topicArn {@link String}
   * @param protocol {@link String}
   * @param endpoint {@link String}
   * @return {@link SubscribeResponse}
   */
  public SubscribeResponse subscribe(final String topicArn, final String protocol,
      final String endpoint) {
    return this.snsClient.subscribe(SubscribeRequest.builder().protocol(protocol).topicArn(topicArn)
        .endpoint(endpoint).build());
  }

  /**
   * Deletes a subscription.
   * 
   * @param subscriptionArn {@link String}
   * @return {@link UnsubscribeResponse}
   */
  public UnsubscribeResponse unsubscribe(final String subscriptionArn) {
    return this.snsClient
        .unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());
  }
}
