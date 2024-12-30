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
package com.formkiq.stacks.api.handler;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Site Configuration Docusign.
 */
@Reflectable
public class SiteConfigurationDocusign {
  /** User Id. */
  private String userId;
  /** Integration Key. */
  private String integrationKey;
  /** RSA Private Key. */
  private String rsaPrivateKey;
  /** HMAC Signature. */
  private String hmacSignature;

  /**
   * constructor.
   */
  public SiteConfigurationDocusign() {

  }

  /**
   * Get User Id.
   * 
   * @return String
   */
  public String getUserId() {
    return this.userId;
  }

  /**
   * Set User Id.
   * 
   * @param user {@link String}
   */
  public void setUserId(final String user) {
    this.userId = user;
  }

  /**
   * Get Integration Key.
   * 
   * @return String
   */
  public String getIntegrationKey() {
    return this.integrationKey;
  }

  /**
   * Set Integration Key.
   * 
   * @param key {@link String}
   */
  public void setIntegrationKey(final String key) {
    this.integrationKey = key;
  }

  /**
   * Get Rsa Private Key.
   * 
   * @return String
   */
  public String getRsaPrivateKey() {
    return this.rsaPrivateKey;
  }

  /**
   * Set Rsa Private Key.
   * 
   * @param privateKey {@link String}
   */
  public void setRsaPrivateKey(final String privateKey) {
    this.rsaPrivateKey = privateKey;
  }

  /**
   * Get HMAC Signature.
   * 
   * @return String
   */
  public String getHmacSignature() {
    return hmacSignature;
  }

  /**
   * Set HMAC Signature.
   * 
   * @param signature {@link String}
   */
  public void setHmacSignature(final String signature) {
    this.hmacSignature = signature;
  }
}
