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
package com.formkiq.module.events;

import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.module.events.document.DocumentEvent;
import com.formkiq.module.events.folder.FolderEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * 
 * SQS implementation of the {@link EventService}.
 *
 */
public class EventServiceSns implements EventService {

  /** Max Sns Message Size. */
  public static final int MAX_SNS_MESSAGE_SIZE = 256000;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link SnsService}. */
  private SnsService snsService;
  /** SNS Topic Arn. */
  private String topicArn;

  /**
   * constructor.
   * 
   * @param snsBuilder {@link SnsConnectionBuilder}
   * @param eventTopicArn {@link String}
   */
  public EventServiceSns(final SnsConnectionBuilder snsBuilder, final String eventTopicArn) {
    if (snsBuilder == null) {
      throw new IllegalArgumentException("'snsBuilder' is null");
    }
    if (eventTopicArn == null) {
      throw new IllegalArgumentException("'eventTopicArn' is null");
    }
    this.snsService = new SnsService(snsBuilder);
    this.topicArn = eventTopicArn;
  }

  String convertToPrintableCharacters(final String s) {
    return s != null ? s.replaceAll("[^A-Za-z0-9/-]", "") : null;
  }

  @Override
  public String publish(final DocumentEvent event) {

    String eventJson = this.gson.toJson(event);
    if (eventJson.length() > MAX_SNS_MESSAGE_SIZE) {
      event.content(null);
      eventJson = this.gson.toJson(event);
    }

    MessageAttributeValue typeAttr =
        MessageAttributeValue.builder().dataType("String").stringValue(event.type()).build();
    MessageAttributeValue siteIdAttr = MessageAttributeValue.builder().dataType("String")
        .stringValue(convertToPrintableCharacters(event.siteId())).build();

    Map<String, MessageAttributeValue> tags = Map.of("type", typeAttr, "siteId", siteIdAttr);

    if (event.userId() != null) {
      MessageAttributeValue userIdAttr = MessageAttributeValue.builder().dataType("String")
          .stringValue(convertToPrintableCharacters(event.userId())).build();
      tags = Map.of("type", typeAttr, "siteId", siteIdAttr, "userId", userIdAttr);
    }

    if (this.topicArn.length() > 0) {
      PublishResponse response = this.snsService.publish(this.topicArn, eventJson, tags);
      LambdaRuntime.getLogger().log("publishing to: " + this.topicArn + " messageId: "
          + response.messageId() + " body: " + eventJson);
    }

    return eventJson;
  }

  @Override
  public String publish(final FolderEvent event) {
    String eventJson = this.gson.toJson(event);

    MessageAttributeValue typeAttr =
        MessageAttributeValue.builder().dataType("String").stringValue(event.type()).build();
    MessageAttributeValue siteIdAttr = MessageAttributeValue.builder().dataType("String")
        .stringValue(convertToPrintableCharacters(event.siteId())).build();

    Map<String, MessageAttributeValue> tags = Map.of("type", typeAttr, "siteId", siteIdAttr);

    LambdaRuntime.getLogger().log("publishing to: " + this.topicArn + " body: " + eventJson);
    PublishResponse response = this.snsService.publish(this.topicArn, eventJson, tags);
    LambdaRuntime.getLogger().log("publishing to: " + this.topicArn + " messageId: "
        + response.messageId() + " body: " + eventJson);

    return eventJson;
  }
}
