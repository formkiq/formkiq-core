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
package com.formkiq.stacks.lambda.s3.event;

import com.formkiq.graalvm.annotations.Reflectable;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/** A single record that can be SQS, S3, or DynamoDB Streams. */
@Reflectable
public record AwsEventRecord(
    // Common
    @SerializedName("eventID") String eventID, @SerializedName("eventName") String eventName,
    @SerializedName("eventVersion") String eventVersion,
    @SerializedName("eventSource") String eventSource, // e.g., aws:sqs, aws:s3, aws:dynamodb
    @SerializedName("awsRegion") String awsRegion, @SerializedName("eventTime") String eventTime,
    @SerializedName("eventSourceARN") String eventSourceArn,

    // ---- SQS fields ----
    @SerializedName("messageId") String messageId,
    @SerializedName("receiptHandle") String receiptHandle, @SerializedName("body") String body,
    @SerializedName("attributes") AwsEventSqsAttributes attributes,
    @SerializedName("messageAttributes") Map<String, AwsEventSqsMessageAttribute> messageAttributes,
    @SerializedName("md5OfBody") String md5OfBody,

    // ---- S3 block (null for non-S3) ----
    @SerializedName("userIdentity") AwsEventUserIdentity userIdentity,
    @SerializedName("requestParameters") AwsEventRequestParameters requestParameters,
    @SerializedName("responseElements") AwsEventResponseElements responseElements,
    @SerializedName("s3") AwsEventS3Entity s3,

    // ---- DynamoDB Streams block (null for non-DDB) ----
    @SerializedName("dynamodb") AwsEventDynamodbEntity dynamodb) {
}
