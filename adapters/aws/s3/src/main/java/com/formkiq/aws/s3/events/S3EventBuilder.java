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
package com.formkiq.aws.s3.events;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Collections;

/**
 * Builder for creating an S3Event with a single S3Record. Allows setting eventName, bucketName, and
 * objectKey.
 */
public class S3EventBuilder {
  public static S3EventBuilder builder() {
    return new S3EventBuilder();
  }

  /** Event Name. */
  private String eventName = "ObjectCreated:Put";
  /** S3 Bucket Name. */
  private String bucketName;

  /** S3 Object Key. */
  private String objectKey;

  private S3EventBuilder() {}

  /**
   * Set the S3 bucket name (e.g., "my-bucket").
   * 
   * @param s3BucketName {@link String}
   * @return S3EventBuilder
   */
  public S3EventBuilder bucketName(final String s3BucketName) {
    this.bucketName = s3BucketName;
    return this;
  }

  /**
   * Build the S3Event record object.
   * 
   * @return S3Event
   */
  public S3Event build() {
    Objects.requireNonNull(eventName, "eventName must be set");
    Objects.requireNonNull(bucketName, "bucketName must be set");
    Objects.requireNonNull(objectKey, "objectKey must be set");

    String eventVersion = "2.1";
    String awsRegion = "us-east-1";
    String configurationId = "exampleConfigRule";
    S3Record record = new S3Record(eventVersion, "aws:s3", awsRegion, Date.from(Instant.now()),
        eventName, new UserIdentity("AWS:EXAMPLE"), new RequestParameters("127.0.0.1"),
        new ResponseElements("EXAMPLEREQUESTID", "EXAMPLEID2"),
        new S3Entity("1.0", configurationId, new Bucket(bucketName,
            new UserIdentity("EXAMPLEOWNERID"), "arn:aws:s3:::" + bucketName),
            new S3Object(objectKey, 0L, "", "")));

    return new S3Event(Collections.singletonList(record));
  }

  /**
   * Override the S3 event name (e.g., "ObjectCreated:Put").
   * 
   * @param s3EventName {@link String}
   * @return S3EventBuilder
   */
  public S3EventBuilder eventName(final String s3EventName) {
    this.eventName = s3EventName;
    return this;
  }

  /**
   * Set the S3 object key (e.g., "path/to/object.txt").
   * 
   * @param s3ObjectKey {@link String}
   * @return S3EventBuilder
   */
  public S3EventBuilder objectKey(final String s3ObjectKey) {
    this.objectKey = s3ObjectKey;
    return this;
  }
}
