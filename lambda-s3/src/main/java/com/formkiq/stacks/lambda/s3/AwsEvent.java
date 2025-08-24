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

// AwsEvent.java
import com.formkiq.graalvm.annotations.Reflectable;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

@Reflectable
public record AwsEvent(@SerializedName("Records") List<Record> records) {

  /** A single record that can be SQS, S3, or DynamoDB Streams. */
  @Reflectable
  public record Record(
      // Common
      @SerializedName("eventID") String eventID, @SerializedName("eventName") String eventName,
      @SerializedName("eventVersion") String eventVersion,
      @SerializedName("eventSource") String eventSource, // e.g., aws:sqs, aws:s3, aws:dynamodb
      @SerializedName("awsRegion") String awsRegion, @SerializedName("eventTime") String eventTime,
      @SerializedName("eventSourceARN") String eventSourceArn,

      // ---- SQS fields ----
      @SerializedName("messageId") String messageId,
      @SerializedName("receiptHandle") String receiptHandle, @SerializedName("body") String body,
      @SerializedName("attributes") SqsAttributes attributes,
      @SerializedName("messageAttributes") Map<String, SqsMessageAttribute> messageAttributes,
      @SerializedName("md5OfBody") String md5OfBody,

      // ---- S3 block (null for non-S3) ----
      @SerializedName("userIdentity") UserIdentity userIdentity,
      @SerializedName("requestParameters") RequestParameters requestParameters,
      @SerializedName("responseElements") ResponseElements responseElements,
      @SerializedName("s3") S3Entity s3,

      // ---- DynamoDB Streams block (null for non-DDB) ----
      @SerializedName("dynamodb") DynamodbEntity dynamodb) {
  }

  // ========= SQS =========

  /** Typed subset of SQS message attributes found in "attributes". */
  @Reflectable
  public record SqsAttributes(
      @SerializedName("ApproximateReceiveCount") String approximateReceiveCount,
      @SerializedName("SentTimestamp") String sentTimestamp,
      @SerializedName("SenderId") String senderId,
      @SerializedName("ApproximateFirstReceiveTimestamp") String approximateFirstReceiveTimestamp) {
  }

  /** Optional per-message attributes in "messageAttributes". */
  @Reflectable
  public record SqsMessageAttribute(@SerializedName("stringValue") String stringValue,
      @SerializedName("binaryValue") String binaryValue,
      @SerializedName("stringListValues") List<String> stringListValues,
      @SerializedName("binaryListValues") List<String> binaryListValues,
      @SerializedName("dataType") String dataType) {
  }

  /** SNS notification envelope commonly carried inside SQS "body". */
  @Reflectable
  public record SnsNotification(@SerializedName("Type") String type,
      @SerializedName("MessageId") String messageId, @SerializedName("TopicArn") String topicArn,
      @SerializedName("Message") String message, // often a JSON string
      @SerializedName("Timestamp") String timestamp,
      @SerializedName("SignatureVersion") String signatureVersion,
      @SerializedName("Signature") String signature,
      @SerializedName("SigningCertURL") String signingCertUrl,
      @SerializedName("UnsubscribeURL") String unsubscribeUrl) {
  }

  /** Example leaf payload from your nested SNS Message (document create/update). */
  @Reflectable
  public record DocumentMessage(@SerializedName("documentId") String documentId,
      @SerializedName("s3key") String s3key, @SerializedName("s3bucket") String s3bucket) {
  }

  // ========= S3 =========

  @Reflectable
  public record UserIdentity(@SerializedName("principalId") String principalId) {
  }
  @Reflectable
  public record RequestParameters(@SerializedName("sourceIPAddress") String sourceIpAddress) {
  }
  @Reflectable
  public record ResponseElements(@SerializedName("x-amz-request-id") String xAmzRequestId,
      @SerializedName("x-amz-id-2") String xAmzId2) {
  }

  @Reflectable
  public record S3Entity(@SerializedName("s3SchemaVersion") String s3SchemaVersion,
      @SerializedName("configurationId") String configurationId,
      @SerializedName("bucket") Bucket bucket, @SerializedName("object") S3Object object) {
  }

  @Reflectable
  public record Bucket(@SerializedName("name") String name,
      @SerializedName("ownerIdentity") OwnerIdentity ownerIdentity,
      @SerializedName("arn") String arn) {
  }

  @Reflectable
  public record OwnerIdentity(@SerializedName("principalId") String principalId) {
  }

  @Reflectable
  public record S3Object(@SerializedName("key") String key, @SerializedName("size") Long size,
      @SerializedName("eTag") String eTag, @SerializedName("sequencer") String sequencer) {
  }

  // ========= DynamoDB Streams =========

  @Reflectable
  public record DynamodbEntity(
      @SerializedName("ApproximateCreationDateTime") Long approximateCreationDateTime,
      @SerializedName("Keys") Keys keys, @SerializedName("NewImage") NewImage newImage,
      @SerializedName("SequenceNumber") String sequenceNumber,
      @SerializedName("SizeBytes") Long sizeBytes,
      @SerializedName("StreamViewType") String streamViewType) {
  }

  @Reflectable
  public record Keys(@SerializedName("PK") AttributeValue pk,
      @SerializedName("SK") AttributeValue sk) {
  }

  @Reflectable
  public record NewImage(@SerializedName("activityKeys") AttributeValue activityKeys,
      @SerializedName("TimeToLive") AttributeValue timeToLive,
      @SerializedName("inserteddate") AttributeValue insertedDate,
      @SerializedName("SK") AttributeValue sk, @SerializedName("siteId") AttributeValue siteId,
      @SerializedName("documentId") AttributeValue documentId,
      @SerializedName("type") AttributeValue type, @SerializedName("PK") AttributeValue pk) {
  }

  /** Minimal DynamoDB AttributeValue shape (S, N, L, M). Extend as needed. */
  @Reflectable
  public record AttributeValue(@SerializedName("S") String s, @SerializedName("N") String n,
      @SerializedName("L") List<AttributeValue> l,
      @SerializedName("M") Map<String, AttributeValue> m) {
  }
}
