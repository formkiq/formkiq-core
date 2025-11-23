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

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_CONFIG;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.config.ConfigService.CHATGPT_API_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.DOCUMENT_TIME_TO_LIVE;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_HMAC_SIGNATURE;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_INTEGRATION_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_RSA_PRIVATE_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_USER_ID;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL;
import static com.formkiq.stacks.dynamodb.config.ConfigService.WEBHOOK_TIME_TO_LIVE;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.graalvm.annotations.Reflectable;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Site Configuration.
 */
@Reflectable
public record SiteConfiguration(DynamoDbKey key, String chatGptApiKey, String maxContentLengthBytes,
    String maxDocuments, String maxWebhooks, String notificationEmail,
    SiteConfigurationDocument document, SiteConfigurationOcr ocr, SiteConfigurationGoogle google,
    SiteConfigurationDocusign docusign, String documentTimeToLive, String webhookTimeToLive) {

  /**
   * Construct a {@link SiteConfiguration} from a DynamoDB attribute map.
   *
   * @param attributes DynamoDB attribute map (required)
   * @return populated {@link SiteConfiguration} record
   * @throws NullPointerException if {@code attributes} is null
   */
  public static SiteConfiguration fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");

    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);

    String chatGptApiKey = DynamoDbTypes.toString(attributes.get(CHATGPT_API_KEY));
    String maxContentLengthBytes = DynamoDbTypes.toString(attributes.get(MAX_DOCUMENT_SIZE_BYTES));
    String maxDocuments = DynamoDbTypes.toString(attributes.get(MAX_DOCUMENTS));
    String maxWebhooks = DynamoDbTypes.toString(attributes.get(MAX_WEBHOOKS));
    String notificationEmail = DynamoDbTypes.toString(attributes.get(NOTIFICATION_EMAIL));
    String documentTimeToLive = DynamoDbTypes.toString(attributes.get(DOCUMENT_TIME_TO_LIVE));
    String webhookTimeToLive = DynamoDbTypes.toString(attributes.get(WEBHOOK_TIME_TO_LIVE));

    SiteConfigurationOcr ocr = getSiteConfigurationOcr(attributes);
    SiteConfigurationGoogle google = getSiteConfigurationGoogle(attributes);
    SiteConfigurationDocusign docusign = getSiteConfigurationDocusign(attributes);
    SiteConfigurationDocument document = getSiteConfigurationDocument(attributes);

    return new SiteConfiguration(key, chatGptApiKey, maxContentLengthBytes, maxDocuments,
        maxWebhooks, notificationEmail, document, ocr, google, docusign, documentTimeToLive,
        webhookTimeToLive);
  }

  private static SiteConfigurationDocument getSiteConfigurationDocument(
      final Map<String, AttributeValue> attributes) {
    List<String> allowlist =
        DynamoDbTypes.toStrings(attributes.get("documentContentTypesAllowlist"));
    List<String> denylist = DynamoDbTypes.toStrings(attributes.get("documentContentTypesDenylist"));
    return new SiteConfigurationDocument(
        new SiteConfigurationDocumentContentTypes(allowlist, denylist));
  }

  private static SiteConfigurationDocusign getSiteConfigurationDocusign(
      final Map<String, AttributeValue> attributes) {
    SiteConfigurationDocusign docusign = null;
    String docusignUserId = DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_USER_ID));
    String docusignIntegrationKey =
        DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_INTEGRATION_KEY));
    String docusignRsaPrivateKey =
        DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_RSA_PRIVATE_KEY));
    String docusignHmacSignature =
        DynamoDbTypes.toString(attributes.get(KEY_DOCUSIGN_HMAC_SIGNATURE));

    if (!Strings.isEmpty(docusignUserId) || !Strings.isEmpty(docusignIntegrationKey)
        || !Strings.isEmpty(docusignRsaPrivateKey) || !Strings.isEmpty(docusignHmacSignature)) {
      docusign = new SiteConfigurationDocusign(docusignUserId, docusignIntegrationKey,
          docusignRsaPrivateKey, docusignHmacSignature);
    }

    return docusign;
  }

  private static SiteConfigurationOcr getSiteConfigurationOcr(
      final Map<String, AttributeValue> attributes) {

    SiteConfigurationOcr ocr = null;
    Long maxTx = DynamoDbTypes.toLong(attributes.get("maxTransactions"));
    Long maxPages = DynamoDbTypes.toLong(attributes.get("maxPagesPerTransaction"));

    if (maxTx != null && maxPages != null) {
      ocr = new SiteConfigurationOcr(maxPages, maxTx);
    }
    return ocr;
  }

  private static SiteConfigurationGoogle getSiteConfigurationGoogle(
      final Map<String, AttributeValue> attributes) {

    String googleAudience =
        DynamoDbTypes.toString(attributes.get("googleWorkloadIdentityAudience"));
    String googleServiceAccount =
        DynamoDbTypes.toString(attributes.get("googleWorkloadIdentityServiceAccount"));

    SiteConfigurationGoogle google = null;
    if (!Strings.isEmpty(googleAudience) && !Strings.isEmpty(googleServiceAccount)) {
      google = new SiteConfigurationGoogle(googleAudience, googleServiceAccount);
    }

    return google;
  }

  /**
   * Convert this configuration record into a DynamoDB attribute map.
   * <p>
   * Mirrors {@link ConfigServiceDynamoDb#save(String, SiteConfiguration)} by:
   * <ul>
   * <li>Only persisting non-null / non-empty values</li>
   * <li>Using the same attribute names for nested configs</li>
   * <li>Defaulting OCR numbers to -1 when unset</li>
   * </ul>
   *
   * @return DynamoDB item map containing key attributes and config values
   */
  public Map<String, AttributeValue> getAttributes() {

    DynamoDbAttributeMapBuilder map =
        key.getAttributesBuilder().withString(CHATGPT_API_KEY, chatGptApiKey)
            .withString(MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)
            .withString(MAX_DOCUMENTS, maxDocuments).withString(MAX_WEBHOOKS, maxWebhooks)
            .withString(NOTIFICATION_EMAIL, notificationEmail)
            .withString(DOCUMENT_TIME_TO_LIVE, documentTimeToLive)
            .withString(WEBHOOK_TIME_TO_LIVE, webhookTimeToLive);

    if (document != null) {
      map.withStrings("documentContentTypesAllowlist", notNull(document.contentTypes().allowlist()))
          .withStrings("documentContentTypesDenylist", notNull(document.contentTypes().denylist()));
    }

    if (google != null) {
      map.withString("googleWorkloadIdentityAudience", google.workloadIdentityAudience())
          .withString("googleWorkloadIdentityServiceAccount",
              google.workloadIdentityServiceAccount());
    }

    if (ocr != null) {
      long maxTx = ocr.maxTransactions();
      long maxPages = ocr.maxPagesPerTransaction();
      map.withNumber("maxTransactions", maxTx != 0 ? maxTx : -1);
      map.withNumber("maxPagesPerTransaction", maxPages != 0 ? maxPages : -1);
    }

    if (docusign != null) {
      map.withString(KEY_DOCUSIGN_USER_ID, docusign.userId().trim())
          .withString(KEY_DOCUSIGN_INTEGRATION_KEY, docusign.integrationKey().trim())
          .withString(KEY_DOCUSIGN_RSA_PRIVATE_KEY, docusign.rsaPrivateKey().trim())
          .withString(KEY_DOCUSIGN_HMAC_SIGNATURE, docusign.hmacSignature().trim());
    }

    return map.build();
  }

  /**
   * Create a new {@link Builder} for {@link SiteConfiguration}.
   *
   * @return builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link SiteConfiguration} that computes the {@link DynamoDbKey} using the same
   * PREFIX_CONFIG pattern as {@link ConfigServiceDynamoDb}.
   */
  public static class Builder implements DynamoDbEntityBuilder<SiteConfiguration> {

    /** ChatGPT API key for AI features. */
    private String chatGptApiKey;
    /** Maximum content length in bytes (stored as string). */
    private String maxContentLengthBytes;
    /** Maximum number of documents (stored as string). */
    private String maxDocuments;
    /** Maximum number of webhooks (stored as string). */
    private String maxWebhooks;
    /** Notification email address. */
    private String notificationEmail;
    /** OCR configuration. */
    private SiteConfigurationOcr ocr;
    /** Google integration configuration. */
    private SiteConfigurationGoogle google;
    /** Docusign integration configuration. */
    private SiteConfigurationDocusign docusign;
    /** Document TTL value (stored as string). */
    private String documentTimeToLive;
    /** Webhook TTL value (stored as string). */
    private String webhookTimeToLive;
    /** {@link SiteConfigurationDocument}. */
    private SiteConfigurationDocument document;

    /**
     * Build a {@link SiteConfiguration} for the given site.
     *
     * @param siteId site identifier (nullable → default site)
     * @return new {@link SiteConfiguration} instance
     */
    @Override
    public SiteConfiguration build(final String siteId) {
      DynamoDbKey key = buildKey(siteId);
      return new SiteConfiguration(key, chatGptApiKey, maxContentLengthBytes, maxDocuments,
          maxWebhooks, notificationEmail, document, ocr, google, docusign, documentTimeToLive,
          webhookTimeToLive);
    }

    /**
     * Build the DynamoDB key matching the service’s {@code keysGeneric(null, PREFIX_CONFIG, site)}
     * behavior.
     * <p>
     * This produces a single configuration item per site.
     *
     * @param siteId site identifier (nullable → default site)
     * @return computed {@link DynamoDbKey}
     */
    @Override
    public DynamoDbKey buildKey(final String siteId) {
      String sk = siteId != null ? siteId : DEFAULT_SITE_ID;
      return DynamoDbKey.builder().pk(null, PREFIX_CONFIG).sk(sk).build();
    }

    /**
     * Set the ChatGPT API key.
     *
     * @param value API key
     * @return this builder
     */
    public Builder chatGptApiKey(final String value) {
      this.chatGptApiKey = value;
      return this;
    }

    /**
     * Clone {@link SiteConfiguration}.
     *
     * @param config {@link SiteConfiguration}
     * @return this builder
     */
    public Builder configuration(final SiteConfiguration config) {
      chatGptApiKey(config.chatGptApiKey);
      webhookTimeToLive(config.webhookTimeToLive);
      ocr(config.ocr);
      notificationEmail(config.notificationEmail);
      maxWebhooks(config.maxWebhooks);
      maxDocuments(config.maxDocuments);
      maxContentLengthBytes(config.maxContentLengthBytes);
      google(config.google);
      docusign(config.docusign);
      documentTimeToLive(config.documentTimeToLive);
      document(config.document);
      return this;
    }

    /**
     * Set the {@link SiteConfigurationDocument} configuration.
     *
     * @param value {@link SiteConfigurationDocument}
     * @return this builder
     */
    public Builder document(final SiteConfigurationDocument value) {
      this.document = value;
      return this;
    }

    /**
     * Set the document time-to-live value.
     *
     * @param value document TTL string
     * @return this builder
     */
    public Builder documentTimeToLive(final String value) {
      this.documentTimeToLive = value;
      return this;
    }

    /**
     * Set the Docusign integration configuration.
     *
     * @param value Docusign config
     * @return this builder
     */
    public Builder docusign(final SiteConfigurationDocusign value) {
      this.docusign = value;
      return this;
    }

    /**
     * Set the Google integration configuration.
     *
     * @param value Google config
     * @return this builder
     */
    public Builder google(final SiteConfigurationGoogle value) {
      this.google = value;
      return this;
    }

    /**
     * Set the maximum content length in bytes.
     *
     * @param value max content length as string
     * @return this builder
     */
    public Builder maxContentLengthBytes(final String value) {
      this.maxContentLengthBytes = value;
      return this;
    }

    /**
     * Set the maximum number of documents.
     *
     * @param value max documents as string
     * @return this builder
     */
    public Builder maxDocuments(final String value) {
      this.maxDocuments = value;
      return this;
    }

    /**
     * Set the maximum number of webhooks.
     *
     * @param value max webhooks as string
     * @return this builder
     */
    public Builder maxWebhooks(final String value) {
      this.maxWebhooks = value;
      return this;
    }

    /**
     * Set the notification email address.
     *
     * @param value email address
     * @return this builder
     */
    public Builder notificationEmail(final String value) {
      this.notificationEmail = value;
      return this;
    }

    /**
     * Set the OCR configuration.
     *
     * @param value OCR config
     * @return this builder
     */
    public Builder ocr(final SiteConfigurationOcr value) {
      this.ocr = value;
      return this;
    }

    /**
     * Set the webhook time-to-live value.
     *
     * @param value webhook TTL string
     * @return this builder
     */
    public Builder webhookTimeToLive(final String value) {
      this.webhookTimeToLive = value;
      return this;
    }
  }
}
