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
package com.formkiq.aws.sqs.events;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SqsEventBuilder {
  /** {@link List} {@link SqsEventRecord}. */
  private final List<SqsEventRecord> records;

  private SqsEventBuilder() {
    this.records = new java.util.ArrayList<>();
  }

  public static SqsEventBuilder builder() {
    return new SqsEventBuilder();
  }

  /**
   * Begin building a new SQSEventRecord within this SQSEvent.
   * 
   * @return RecordBuilder
   */
  public RecordBuilder record() {
    return new RecordBuilder(this);
  }

  /**
   * Build the finalized SQSEvent containing all added records.
   * 
   * @return SqsEvent
   */
  public SqsEvent build() {
    return new SqsEvent(List.copyOf(records));
  }

  /**
   * Nested builder for a single SQSEventRecord.
   */
  public static class RecordBuilder {
    /** {@link SqsEventBuilder}. */
    private final SqsEventBuilder parent;
    /** Message Id. */
    private String messageId = UUID.randomUUID().toString();
    /** Receipt Handle. */
    private String receiptHandle = UUID.randomUUID().toString();
    /** Sqs Body. */
    private String body;
    /** {@link Map}. */
    private final Map<String, String> attributes = new HashMap<>();
    /** {@link Map}. */
    private final Map<String, MessageAttribute> messageAttributes = new HashMap<>();
    /** Event Source. */
    private String eventSource = "aws:sqs";
    /** Event Source Arn. */
    private String eventSourceArn;
    /** Aws Region. */
    private String awsRegion = "us-east-1";

    private RecordBuilder(final SqsEventBuilder sqsParent) {
      this.parent = sqsParent;
      String ts = String.valueOf(Instant.now().toEpochMilli());
      attributes.put("ApproximateReceiveCount", "1");
      attributes.put("SentTimestamp", ts);
      attributes.put("ApproximateFirstReceiveTimestamp", ts);
    }

    public RecordBuilder messageId(final String sqsMessageId) {
      this.messageId = sqsMessageId;
      return this;
    }

    public RecordBuilder receiptHandle(final String sqsReceiptHandle) {
      this.receiptHandle = sqsReceiptHandle;
      return this;
    }

    public RecordBuilder body(final String sqsBody) {
      this.body = sqsBody;
      return this;
    }

    public RecordBuilder attribute(final String key, final String value) {
      this.attributes.put(key, value);
      return this;
    }

    public RecordBuilder messageAttribute(final String key, final MessageAttribute attr) {
      this.messageAttributes.put(key, attr);
      return this;
    }

    public RecordBuilder eventSourceArn(final String sqsEventSourceArn) {
      this.eventSourceArn = sqsEventSourceArn;
      return this;
    }

    public RecordBuilder awsRegion(final String sqsAwsRegion) {
      this.awsRegion = sqsAwsRegion;
      return this;
    }

    /**
     * Finalize this record and add to parent SQSEventBuilder.
     * 
     * @return SqsEventBuilder
     */
    public SqsEventBuilder add() {
      Objects.requireNonNull(body, "body must be set");
      Objects.requireNonNull(eventSourceArn, "eventSourceARN must be set");
      SqsEventRecord rec =
          new SqsEventRecord(messageId, receiptHandle, body, Map.copyOf(attributes),
              Map.copyOf(messageAttributes), null, eventSource, eventSourceArn, awsRegion);

      parent.records.add(rec);
      return parent;
    }
  }
}
