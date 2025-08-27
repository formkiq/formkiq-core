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
package com.formkiq.stacks.lambda.s3;

import com.formkiq.stacks.lambda.s3.event.AwsEvent;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds an AWS SQS event payload containing an SNS notification with dynamic siteId and
 * documentId.
 */
public class SqsEventBuilder {
  /** Site Id. */
  private final String siteId;
  /** Document Id. */
  private final String documentId;
  /** {@link Gson}. */
  private static final Gson GSON = new Gson();

  /**
   * constructor.
   * 
   * @param b {@link Builder}
   */
  private SqsEventBuilder(final Builder b) {
    this.siteId = b.siteId;
    this.documentId = b.documentId;
  }

  /**
   * Returns the full event payload as a Map&lt;String, Object&gt;.
   * 
   * @return Map
   */
  public Map<String, Object> build() {

    // Build single SQS record
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("messageId", UUID.randomUUID().toString());
    record.put("receiptHandle", "<your-static-or-dynamic-receiptHandle>");

    // Build SNS notification body
    Map<String, Object> sns = new LinkedHashMap<>();
    sns.put("Type", "Notification");
    sns.put("MessageId", UUID.randomUUID().toString());
    sns.put("TopicArn",
        "arn:aws:sns:us-east-2:111111111111:formkiq-core-test-...-SnsDocumentEvent-...");

    // Inner message JSON with dynamic fields
    Map<String, String> inner = new LinkedHashMap<>();
    inner.put("siteId", siteId);
    inner.put("documentId", documentId);
    inner.put("type", "actions");
    sns.put("Message", GSON.toJson(inner));

    // Static SNS metadata
    sns.put("Timestamp", "2022-06-18T04:19:58.070Z");
    sns.put("SignatureVersion", "1");
    sns.put("Signature", "<signature-string>");
    sns.put("SigningCertURL", "https://sns.us-east-2.amazonaws.com/...pem");
    sns.put("UnsubscribeURL", "https://sns.us-east-2.amazonaws.com/?Action=Unsubscribe&...");

    // SNS MessageAttributes
    Map<String, Object> msgAttrs = new LinkedHashMap<>();
    Map<String, String> siteAttr = new LinkedHashMap<>();
    siteAttr.put("Type", "String");
    siteAttr.put("Value", siteId);
    msgAttrs.put("siteId", siteAttr);
    Map<String, String> typeAttr = new LinkedHashMap<>();
    typeAttr.put("Type", "String");
    typeAttr.put("Value", "actions");
    msgAttrs.put("type", typeAttr);
    sns.put("MessageAttributes", msgAttrs);

    // Embed SNS payload as a JSON string
    record.put("body", GSON.toJson(sns));

    // SQS record attributes
    Map<String, String> attrs = new LinkedHashMap<>();
    attrs.put("ApproximateReceiveCount", "1");
    attrs.put("SentTimestamp", "1655525998108");
    attrs.put("SenderId", "AIDAJQR6QDGQ7PATMSYEY");
    attrs.put("ApproximateFirstReceiveTimestamp", "1655525998113");
    record.put("attributes", attrs);

    record.put("messageAttributes", Collections.emptyMap());
    record.put("md5OfBody", "18825a1dbc4503da7bb93b2b6585d704");
    record.put("eventSource", "aws:sqs");
    record.put("eventSourceARN",
        "arn:aws:sqs:us-east-2:111111111111:formkiq-core-test-...-DocumentActionsQueue-...");
    record.put("awsRegion", "us-east-2");

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("Records", Collections.singletonList(record));
    return root;
  }

  /**
   * Entry point to the builder.
   * 
   * @return Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder.
   */
  public static class Builder {
    /** Site Id. */
    private String siteId;
    /** Document Id. */
    private String documentId;

    /**
     * Sets the eventSiteId to include in the SNS message.
     * 
     * @param eventSiteId {@link String}
     * @return Builder
     */
    public Builder siteId(final String eventSiteId) {
      this.siteId = eventSiteId;
      return this;
    }

    /**
     * Sets the eventDocumentId to include in the SNS message.
     * 
     * @param eventDocumentId {@link String}
     * @return Builder
     */
    public Builder documentId(final String eventDocumentId) {
      this.documentId = eventDocumentId;
      return this;
    }

    /**
     * Builds and returns the event payload as a Map.
     * 
     * @return Map
     */
    public AwsEvent build() {
      Map<String, Object> map = new SqsEventBuilder(this).build();
      String json = GSON.toJson(map);
      return GSON.fromJson(json, AwsEvent.class);
    }
  }
}
