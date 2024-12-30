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
 * Site Configuration.
 */
@Reflectable
public class SiteConfiguration {
  /** Chat Gpt Api Key. */
  private String chatGptApiKey;
  /** Max Content Length in Bytes. */
  private String maxContentLengthBytes;
  /** Max Documents. */
  private String maxDocuments;
  /** Max Webhooks. */
  private String maxWebhooks;
  /** Notification Email. */
  private String notificationEmail;
  /** Ocr Configuration. */
  private SiteConfigurationOcr ocr;
  /** Google Configuration. */
  private SiteConfigurationGoogle google;
  /** Docusign Configuration. */
  private SiteConfigurationDocusign docusign;

  /**
   * constructor.
   */
  public SiteConfiguration() {

  }

  public SiteConfigurationDocusign getDocusign() {
    return this.docusign;
  }

  public void setDocusign(final SiteConfigurationDocusign docusignConfig) {
    this.docusign = docusignConfig;
  }

  public SiteConfigurationGoogle getGoogle() {
    return this.google;
  }

  public void setGoogle(final SiteConfigurationGoogle googleConfig) {
    this.google = googleConfig;
  }

  public SiteConfigurationOcr getOcr() {
    return ocr;
  }

  public void setOcr(final SiteConfigurationOcr ocrConfig) {
    this.ocr = ocrConfig;
  }

  public String getNotificationEmail() {
    return notificationEmail;
  }

  public void setNotificationEmail(final String email) {
    this.notificationEmail = email;
  }

  public String getMaxWebhooks() {
    return maxWebhooks;
  }

  public void setMaxWebhooks(final String max) {
    this.maxWebhooks = max;
  }

  public String getMaxDocuments() {
    return maxDocuments;
  }

  public void setMaxDocuments(final String max) {
    this.maxDocuments = max;
  }

  public String getMaxContentLengthBytes() {
    return maxContentLengthBytes;
  }

  public void setMaxContentLengthBytes(final String max) {
    this.maxContentLengthBytes = max;
  }

  public String getChatGptApiKey() {
    return chatGptApiKey;
  }

  public void setChatGptApiKey(final String apiKey) {
    this.chatGptApiKey = apiKey;
  }
}
