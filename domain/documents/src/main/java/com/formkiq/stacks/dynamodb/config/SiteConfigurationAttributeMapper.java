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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.config.ConfigService.ADDITIONAL_DOCUSIGN_ACCOUNTS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.CHATGPT_API_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.DOCUMENT_TIME_TO_LIVE;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_WEBUI_SSO_LOGIN_REDIRECT_ENABLED;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL;
import static com.formkiq.stacks.dynamodb.config.ConfigService.WEBHOOK_TIME_TO_LIVE;

import java.util.Map;

import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Maps {@link SiteConfiguration} to DynamoDB attributes.
 */
final class SiteConfigurationAttributeMapper {

  private static void addDocumentAttributes(final DynamoDbAttributeMapBuilder map,
      final SiteConfigurationDocument document) {
    if (document != null) {
      SiteConfigurationDocumentContentTypes contentTypes = document.contentTypes();
      if (contentTypes != null) {
        map.withStrings("documentContentTypesAllowlist", notNull(contentTypes.allowlist()))
            .withStrings("documentContentTypesDenylist", notNull(contentTypes.denylist()));
      }

      SiteConfigurationDocumentRetentionAndDisposition retentionAndDisposition =
          document.withDefaults().retentionAndDisposition();
      map.withString("documentDispositionAction",
          retentionAndDisposition.dispositionAction().name())
          .withNumber("documentSoftDeleteRetentionInDays",
              retentionAndDisposition.softDeleteRetentionInDays());
    }
  }

  /**
   * Convert a site configuration to DynamoDB attributes.
   *
   * @param config site configuration
   * @return DynamoDB attributes
   */
  static Map<String, AttributeValue> toAttributeMap(final SiteConfiguration config) {
    DynamoDbAttributeMapBuilder map =
        config.key().getAttributesBuilder().withString(CHATGPT_API_KEY, config.chatGptApiKey())
            .withString(MAX_DOCUMENT_SIZE_BYTES, config.maxContentLengthBytes())
            .withString(MAX_DOCUMENTS, config.maxDocuments())
            .withString(MAX_WEBHOOKS, config.maxWebhooks())
            .withString(NOTIFICATION_EMAIL, config.notificationEmail())
            .withString(DOCUMENT_TIME_TO_LIVE, config.documentTimeToLive())
            .withString(WEBHOOK_TIME_TO_LIVE, config.webhookTimeToLive());

    SiteConfigurationBranding branding = config.branding();
    if (branding != null) {
      map.withString("brandingTheme", branding.theme());
    }

    addDocumentAttributes(map, config.document());

    SiteConfigurationGoogle google = config.google();
    if (google != null) {
      map.withString("googleWorkloadIdentityAudience", google.workloadIdentityAudience())
          .withString("googleWorkloadIdentityServiceAccount",
              google.workloadIdentityServiceAccount());
    }

    SiteConfigurationOcr ocr = config.ocr();
    if (ocr != null) {
      long maxTx = ocr.maxTransactions();
      long maxPages = ocr.maxPagesPerTransaction();
      map.withNumber("maxTransactions", maxTx != 0 ? maxTx : -1);
      map.withNumber("maxPagesPerTransaction", maxPages != 0 ? maxPages : -1);
    }

    SiteConfigurationDocusign docusign = config.docusign();
    if (docusign != null) {
      map.withMap(docusign.getAttributes());
    }
    if (config.additionalDocusignAccounts() != null) {
      map.withAttributeValue(ADDITIONAL_DOCUSIGN_ACCOUNTS,
          AttributeValue.fromL(config.additionalDocusignAccounts().stream()
              .map(account -> AttributeValue.fromM(account.getAttributes())).toList()));
    }

    SiteConfigurationNotification.addConfigurationAttributes(map, config.notification(),
        config.notificationEmail());

    SiteConfigurationWebUi webui = config.webui();
    if (webui != null) {
      map.withBoolean(KEY_WEBUI_SSO_LOGIN_REDIRECT_ENABLED, webui.ssoAutomaticSignIn());
    }

    return map.build();
  }

  private SiteConfigurationAttributeMapper() {}
}
