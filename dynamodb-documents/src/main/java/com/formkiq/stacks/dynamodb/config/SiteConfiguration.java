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
package com.formkiq.stacks.dynamodb.config;

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
  /** Document Time to Live. */
  private String documentTimeToLive;
  /** Webhook Time to Live. */
  private String webhookTimeToLive;

  /**
   * constructor.
   */
  public SiteConfiguration() {

  }

  public String getChatGptApiKey() {
    return chatGptApiKey;
  }

  public String getDocumentTimeToLive() {
    return this.documentTimeToLive;
  }

  public SiteConfigurationDocusign getDocusign() {
    return this.docusign;
  }

  public SiteConfigurationGoogle getGoogle() {
    return this.google;
  }

  public String getMaxContentLengthBytes() {
    return maxContentLengthBytes;
  }

  public String getMaxDocuments() {
    return maxDocuments;
  }

  public String getMaxWebhooks() {
    return maxWebhooks;
  }

  public String getNotificationEmail() {
    return notificationEmail;
  }

  public SiteConfigurationOcr getOcr() {
    return ocr;
  }

  public String getWebhookTimeToLive() {
    return this.webhookTimeToLive;
  }

  public SiteConfiguration setChatGptApiKey(final String apiKey) {
    this.chatGptApiKey = apiKey;
    return this;
  }

  public SiteConfiguration setDocumentTimeToLive(final String ttl) {
    this.documentTimeToLive = ttl;
    return this;
  }

  public SiteConfiguration setDocusign(final SiteConfigurationDocusign docusignConfig) {
    this.docusign = docusignConfig;
    return this;
  }

  public SiteConfiguration setGoogle(final SiteConfigurationGoogle googleConfig) {
    this.google = googleConfig;
    return this;
  }

  public SiteConfiguration setMaxContentLengthBytes(final String max) {
    this.maxContentLengthBytes = max;
    return this;
  }

  public SiteConfiguration setMaxDocuments(final String max) {
    this.maxDocuments = max;
    return this;
  }

  public SiteConfiguration setMaxWebhooks(final String max) {
    this.maxWebhooks = max;
    return this;
  }

  public SiteConfiguration setNotificationEmail(final String email) {
    this.notificationEmail = email;
    return this;
  }

  public SiteConfiguration setOcr(final SiteConfigurationOcr ocrConfig) {
    this.ocr = ocrConfig;
    return this;
  }

  public SiteConfiguration setWebhookTimeToLive(final String ttl) {
    this.webhookTimeToLive = ttl;
    return this;
  }
}
