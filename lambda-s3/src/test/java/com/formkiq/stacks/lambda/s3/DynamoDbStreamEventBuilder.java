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

import com.google.gson.Gson;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds an AWS SQS event payload containing a DynamoDB Stream record with dynamic documentId, SK,
 * PK, and type.
 */
public class DynamoDbStreamEventBuilder {
  /** Document Id. */
  private final String documentId;
  /** Sk. */
  private final String sk;
  /** Type. */
  private final String type;
  /** Pk. */
  private final String pk;
  /** {@link Gson}. */
  private static final Gson GSON = new Gson();

  /**
   * constructor.
   * 
   * @param b {@link Builder}
   */
  private DynamoDbStreamEventBuilder(final Builder b) {
    this.documentId = b.documentId;
    this.sk = b.sk;
    this.type = b.type;
    this.pk = b.pk;
  }

  /**
   * Returns {@link Map}.
   * 
   * @return Map
   */
  public Map<String, Object> build() {
    Map<String, Object> record = new LinkedHashMap<>();

    // Top-level SQS record fields
    record.put("messageId", UUID.randomUUID().toString());
    record.put("receiptHandle", "<your-receipt-handle>");

    // Body: JSON string of DynamoDB stream event
    Map<String, Object> bodyMap = new LinkedHashMap<>();
    bodyMap.put("eventID", UUID.randomUUID().toString());
    bodyMap.put("eventName", "INSERT");
    bodyMap.put("eventVersion", "1.1");
    bodyMap.put("eventSource", "aws:dynamodb");
    bodyMap.put("awsRegion", "us-east-2");

    // DynamoDB inner map
    Map<String, Object> dynamodb = new LinkedHashMap<>();

    // Keys map
    Map<String, Object> keys = new LinkedHashMap<>();
    Map<String, String> skMap = Collections.singletonMap("S", sk);
    Map<String, String> pkMap = Collections.singletonMap("S", pk);
    keys.put("SK", skMap);
    keys.put("PK", pkMap);
    dynamodb.put("Keys", keys);

    // NewImage map
    Map<String, Object> newImage = new LinkedHashMap<>();
    newImage.put("service", Collections.singletonMap("S", "EVENTBRIDGE"));
    newImage.put("inserteddate", Collections.singletonMap("S", sk.substring(sk.indexOf('#') + 1)));
    newImage.put("SK", skMap);
    newImage.put("documentId", Collections.singletonMap("S", documentId));
    newImage.put("PK", pkMap);
    newImage.put("message", Collections.singletonMap("S", "added Document Metadata"));
    newImage.put("type", Collections.singletonMap("S", type));
    newImage.put("userId", Collections.singletonMap("S", "arn:aws:iam::1111111111111:user/mike"));
    newImage.put("status", Collections.singletonMap("S", "PENDING"));
    dynamodb.put("NewImage", newImage);

    bodyMap.put("dynamodb", dynamodb);
    record.put("body", GSON.toJson(bodyMap));

    // SQS record attributes
    record.put("eventSource", "aws:sqs");
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
    /** Document Id. */
    private String documentId;
    /** Sk. */
    private String sk;
    /** Type. */
    private String type;
    /** Pk. */
    private String pk;

    /**
     * Sets the docId (e.g. "7e4a43d6-b74a-4fb8-a751-e724cff5c3de").
     * 
     * @param docId {@link String}
     * @return Builder
     */
    public Builder documentId(final String docId) {
      this.documentId = docId;
      return this;
    }

    /**
     * Sets the SK (e.g. "syncs#2025-01-22T15:53:12.342Z").
     * 
     * @param eventSk {@link String}
     * @return Builder
     */
    public Builder sk(final String eventSk) {
      this.sk = eventSk;
      return this;
    }

    /**
     * Sets the eventType (e.g. "METADATA").
     * 
     * @param eventType {@link String}
     * @return Builder
     */
    public Builder type(final String eventType) {
      this.type = eventType;
      return this;
    }

    /**
     * Sets the PK (e.g. "docs#7e4a43d6-b74a-4fb8-a751-e724cff5c3de").
     * 
     * @param eventPk {@link String}
     * @return Builder
     */
    public Builder pk(final String eventPk) {
      this.pk = eventPk;
      return this;
    }

    private void validate() {
      if (documentId == null || sk == null || type == null || pk == null) {
        throw new IllegalStateException("documentId, sk, type, and pk must be set");
      }
    }

    /**
     * Builds and returns the event payload as a Map.
     * 
     * @return {@link AwsEvent}
     */
    public AwsEvent build() {
      validate();
      Map<String, Object> map = new DynamoDbStreamEventBuilder(this).build();
      String json = GSON.toJson(map);
      return GSON.fromJson(json, AwsEvent.class);
    }
  }
}
