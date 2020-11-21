/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.aws.sns;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionResponse;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
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
   * @return {@link PublishResponse}
   */
  public PublishResponse publish(final String topicArn, final String message) {
    return this.snsClient
        .publish(PublishRequest.builder().topicArn(topicArn).message(message).build());
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
