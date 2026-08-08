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

import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL_PROVIDER;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL_SMTP_CONNECTION_SECURITY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL_SMTP_CREDENTIALS_SECRET_ARN;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL_SMTP_HOST;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL_SMTP_PORT;

import java.util.Map;

import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.graalvm.annotations.Reflectable;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Site notification configuration.
 *
 * @param email notification From address
 * @param provider email provider
 * @param smtp SMTP configuration
 */
@Reflectable
public record SiteConfigurationNotification(String email,
    SiteConfigurationNotificationProvider provider, SiteConfigurationNotificationSmtp smtp) {

  static void addConfigurationAttributes(final DynamoDbAttributeMapBuilder attributes,
      final SiteConfigurationNotification notification, final String legacyEmail) {
    if (notification != null) {
      notification.addAttributes(attributes);
    } else if (legacyEmail != null) {
      new SiteConfigurationNotification(legacyEmail, SiteConfigurationNotificationProvider.SES,
          null).addAttributes(attributes);
    }
  }

  /**
   * Construct a notification configuration from DynamoDB attributes.
   *
   * @param attributes DynamoDB attributes
   * @return notification configuration or null
   */
  public static SiteConfigurationNotification fromAttributeMap(
      final Map<String, AttributeValue> attributes) {
    String email = DynamoDbTypes.toString(attributes.get(NOTIFICATION_EMAIL));
    String providerValue = DynamoDbTypes.toString(attributes.get(NOTIFICATION_EMAIL_PROVIDER));
    SiteConfigurationNotificationProvider provider =
        providerValue != null ? SiteConfigurationNotificationProvider.valueOf(providerValue)
            : email != null ? SiteConfigurationNotificationProvider.SES : null;

    if (provider == null) {
      return null;
    }

    SiteConfigurationNotificationSmtp smtp = null;
    if (provider == SiteConfigurationNotificationProvider.SMTP) {
      String host = DynamoDbTypes.toString(attributes.get(NOTIFICATION_EMAIL_SMTP_HOST));
      Long port = DynamoDbTypes.toLong(attributes.get(NOTIFICATION_EMAIL_SMTP_PORT));
      String security =
          DynamoDbTypes.toString(attributes.get(NOTIFICATION_EMAIL_SMTP_CONNECTION_SECURITY));
      String secretArn =
          DynamoDbTypes.toString(attributes.get(NOTIFICATION_EMAIL_SMTP_CREDENTIALS_SECRET_ARN));
      smtp = new SiteConfigurationNotificationSmtp(host, port != null ? port.intValue() : null,
          security != null ? SiteConfigurationNotificationSmtpConnectionSecurity.valueOf(security)
              : null,
          secretArn);
    }

    return new SiteConfigurationNotification(email, provider, smtp);
  }

  /**
   * Add this notification configuration to DynamoDB attributes.
   *
   * @param attributes DynamoDB attribute builder
   */
  public void addAttributes(final DynamoDbAttributeMapBuilder attributes) {
    attributes.withString(NOTIFICATION_EMAIL, email).withString(NOTIFICATION_EMAIL_PROVIDER,
        provider != null ? provider.name() : null);

    if (smtp != null) {
      attributes.withString(NOTIFICATION_EMAIL_SMTP_HOST, smtp.host())
          .withNumber(NOTIFICATION_EMAIL_SMTP_PORT, smtp.port())
          .withString(NOTIFICATION_EMAIL_SMTP_CONNECTION_SECURITY,
              smtp.connectionSecurity() != null ? smtp.connectionSecurity().name() : null)
          .withString(NOTIFICATION_EMAIL_SMTP_CREDENTIALS_SECRET_ARN, smtp.credentialsSecretArn());
    }
  }
}
