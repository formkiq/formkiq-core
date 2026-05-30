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
package com.formkiq.aws.sqs;

import java.util.Map;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Sqs Message Record Object.
 *
 */
@Reflectable
public class SqsMessageRecord {

  /** Record Attributes. */
  private Map<String, String> attributes;
  /** AWS Region. */
  private String awsRegion;
  /** Request body. */
  private String body;
  /** Event Source. */
  private String eventSource;
  /** Md5 of body. */
  private String md5OfBody;
  /** Message Attributes. */
  private Map<String, String> messageAttributes;
  /** MessageId. */
  private String messageId;
  /** Receipt Handle. */
  private String receiptHandle;

  /**
   * constructor.
   */
  public SqsMessageRecord() {}

  /**
   * Get Attributes.
   * 
   * @return {@link Map}
   */
  public Map<String, String> attributes() {
    return this.attributes;
  }

  /**
   * Set Attributes.
   * 
   * @param map {@link Map}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord attributes(final Map<String, String> map) {
    this.attributes = map;
    return this;
  }

  /**
   * Get AWS Region.
   * 
   * @return {@link String}
   */
  public String awsRegion() {
    return this.awsRegion;
  }

  /**
   * Set AWS Region.
   * 
   * @param region {@link String}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord awsRegion(final String region) {
    this.awsRegion = region;
    return this;
  }

  /**
   * Get Body.
   * 
   * @return {@link String}
   */
  public String body() {
    return this.body;
  }

  /**
   * Set Body.
   * 
   * @param s {@link String}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord body(final String s) {
    this.body = s;
    return this;
  }

  /**
   * Get Event Source.
   * 
   * @return {@link String}
   */
  public String eventSource() {
    return this.eventSource;
  }

  /**
   * Set Event Source.
   * 
   * @param source {@link String}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord eventSource(final String source) {
    this.eventSource = source;
    return this;
  }

  /**
   * Get Md5 Hash of Body.
   * 
   * @return {@link String}
   */
  public String md5OfBody() {
    return this.md5OfBody;
  }

  /**
   * Set Md5 Hash.
   * 
   * @param md5 {@link String}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord md5OfBody(final String md5) {
    this.md5OfBody = md5;
    return this;
  }

  /**
   * Get Message Attributes.
   * 
   * @return {@link Map}
   */
  public Map<String, String> messageAttributes() {
    return this.messageAttributes;
  }

  /**
   * Set Message Attributes.
   * 
   * @param map {@link Map}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord messageAttributes(final Map<String, String> map) {
    this.messageAttributes = map;
    return this;
  }

  /**
   * Get Message Id.
   * 
   * @return {@link String}
   */
  public String messageId() {
    return this.messageId;
  }

  /**
   * Set Message Id.
   * 
   * @param id {@link String}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord messageId(final String id) {
    this.messageId = id;
    return this;
  }

  /**
   * Get Receipt Handle.
   * 
   * @return {@link String}
   */
  public String receiptHandle() {
    return this.receiptHandle;
  }

  /**
   * Set Receipt Handle.
   * 
   * @param receipt {@link String}
   * @return {@link SqsMessageRecord}
   */
  public SqsMessageRecord receiptHandle(final String receipt) {
    this.receiptHandle = receipt;
    return this;
  }
}
