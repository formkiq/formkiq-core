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
package com.formkiq.aws.services.lambda;

import java.util.Map;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Lambda Record Object.
 *
 */
@Reflectable
public class LambdaInputRecord {

  /** MessageId. */
  private String messageId;

  /** Receipt Handle. */
  private String receiptHandle;

  /** Request body. */
  private String body;
  /** Record Attributes. */
  private Map<String, String> attributes;
  /** Message Attributes. */
  private Map<String, String> messageAttributes;
  /** Md5 of body. */
  private String md5OfBody;
  /** Event Source. */
  private String eventSource;
  /** AWS Region. */
  private String awsRegion;

  /**
   * constructor.
   */
  public LambdaInputRecord() {}
  
  /**
   * Get Message Id.
   * @return {@link String}
   */
  public String getMessageId() {
    return this.messageId;
  }

  /**
   * Set Message Id.
   * @param id {@link String}
   */
  public void setMessageId(final String id) {
    this.messageId = id;
  }

  /**
   * Get Receipt Handle.
   * @return {@link String}
   */
  public String getReceiptHandle() {
    return this.receiptHandle;
  }

  /**
   * Set Receipt Handle.
   * @param receipt {@link String}
   */
  public void setReceiptHandle(final String receipt) {
    this.receiptHandle = receipt;
  }

  /**
   * Get Body.
   * @return {@link String}
   */
  public String getBody() {
    return this.body;
  }

  /**
   * Set Body.
   * @param s {@link String}
   */
  public void setBody(final String s) {
    this.body = s;
  }

  /**
   * Get Attributes.
   * @return {@link Map}
   */
  public Map<String, String> getAttributes() {
    return this.attributes;
  }

  /**
   * Set Attributes.
   * @param map {@link Map}
   */
  public void setAttributes(final Map<String, String> map) {
    this.attributes = map;
  }

  /**
   * Get Message Attributes.
   * @return {@link Map}
   */
  public Map<String, String> getMessageAttributes() {
    return this.messageAttributes;
  }

  /**
   * Set Message Attributes.
   * @param map {@link Map}
   */
  public void setMessageAttributes(final Map<String, String> map) {
    this.messageAttributes = map;
  }

  /**
   * Get Md5 Hash of Body.
   * @return {@link String}
   */
  public String getMd5OfBody() {
    return this.md5OfBody;
  }

  /**
   * Set Md5 Hash.
   * @param md5 {@link String}
   */
  public void setMd5OfBody(final String md5) {
    this.md5OfBody = md5;
  }

  /**
   * Get Event Source.
   * @return {@link String}
   */
  public String getEventSource() {
    return this.eventSource;
  }

  /**
   * Set Event Source.
   * @param source {@link String}
   */
  public void setEventSource(final String source) {
    this.eventSource = source;
  }

  /**
   * Get AWS Region.
   * @return {@link String}
   */
  public String getAwsRegion() {
    return this.awsRegion;
  }

  /**
   * Set AWS Region.
   * @param region {@link String}
   */
  public void setAwsRegion(final String region) {
    this.awsRegion = region;
  }
}