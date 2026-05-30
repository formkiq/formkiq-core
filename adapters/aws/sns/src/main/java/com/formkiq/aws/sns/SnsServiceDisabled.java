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
 * Disabled {@link SnsServiceImpl}.
 */
public class SnsServiceDisabled implements SnsService {
  @Override
  public ConfirmSubscriptionResponse confirmSubscription(final String topicArn,
      final String token) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public CreateTopicResponse createTopic(final String topicName) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public ListTopicsResponse listTopics(final String nextToken) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public PublishResponse publish(final String topicArn, final String message,
      final Map<String, MessageAttributeValue> messageAttributes) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public SubscribeResponse subscribe(final String topicArn, final String protocol,
      final String endpoint) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public UnsubscribeResponse unsubscribe(final String subscriptionArn) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public void unsubscribeAll() {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }
}
