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
package com.formkiq.stacks.lambda.s3.actions;

import static com.formkiq.strings.Strings.isEmpty;
import static com.formkiq.strings.Strings.trim;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_HTML;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_SUBJECT;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TEXT;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_BCC;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_CC;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TYPE;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.ses.SesService;
import com.formkiq.aws.secretsmanager.SecretsManagerService;
import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.aws.dynamodb.actions.ActionStatus;
import com.formkiq.aws.dynamodb.actions.ActionType;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotification;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationProvider;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationSmtp;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import com.google.gson.JsonParseException;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.Message.Builder;

/**
 * {@link DocumentAction} for Notification {@link ActionType}.
 */
public class NotificationAction implements DocumentAction {

  /** {@link AwsServiceCache}. */
  private final AwsServiceCache serviceCache;
  /** SMTP email sender. */
  private final SmtpEmailSender smtpEmailSender;

  /**
   * constructor.
   * 
   * @param siteId {@link String}
   * @param cache {@link AwsServiceCache}
   */
  public NotificationAction(final String siteId, final AwsServiceCache cache) {
    this(siteId, cache, new SmtpEmailSender());
  }

  NotificationAction(final String siteId, final AwsServiceCache cache,
      final SmtpEmailSender smtpSender) {
    this.serviceCache = cache;
    this.smtpEmailSender = smtpSender;
  }

  private String getCredential(final Map<?, ?> credentials, final String key) throws IOException {
    Object value = credentials != null ? credentials.get(key) : null;
    if (!(value instanceof String string) || isEmpty(trim(string))) {
      throw new IOException("SMTP credentials secret must contain a non-empty '" + key + "'");
    }
    return string;
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId,
      final DocumentArtifact document, final List<Action> actions, final Action action)
      throws IOException {

    String notificationType = (String) action.parameters().get(PARAMETER_NOTIFICATION_TYPE);
    if ("IN_APP".equalsIgnoreCase(notificationType)) {
      return new ProcessActionStatus(ActionStatus.COMPLETE);
    }

    ConfigService configService = serviceCache.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get(siteId);
    String cc = (String) action.parameters().get(PARAMETER_NOTIFICATION_TO_CC);
    String bcc = (String) action.parameters().get(PARAMETER_NOTIFICATION_TO_BCC);
    String subject = (String) action.parameters().get(PARAMETER_NOTIFICATION_SUBJECT);
    String text = (String) action.parameters().get(PARAMETER_NOTIFICATION_TEXT);
    String html = (String) action.parameters().get(PARAMETER_NOTIFICATION_HTML);

    sendEmail(config, null, cc, bcc, subject, text, html);

    return new ProcessActionStatus(ActionStatus.COMPLETE);
  }

  private void sendEmail(final SiteConfiguration config, final String to, final String cc,
      final String bcc, final String subject, final String text, final String html)
      throws IOException {
    SiteConfigurationNotification notification = config.notification();
    if (notification != null
        && notification.provider() == SiteConfigurationNotificationProvider.SMTP) {
      sendSmtp(notification, to, cc, bcc, subject, text, html);
    } else {
      String source = notification != null ? notification.email() : config.notificationEmail();
      if (isEmpty(trim(source))) {
        throw new IOException("Site notification configuration is not set");
      }
      sendSes(source, to, cc, bcc, subject, text, html);
    }
  }

  private void sendSes(final String source, final String to, final String cc, final String bcc,
      final String subject, final String text, final String html) {
    SesService ses = serviceCache.getExtension(SesService.class);

    Builder msg = Message.builder().subject(Content.builder().data(subject).build());

    if (!isEmpty(text)) {
      msg = msg.body(Body.builder().text(Content.builder().data(text).build()).build());
    }

    if (!isEmpty(html)) {
      msg = msg.body(Body.builder().html(Content.builder().data(html).build()).build());
    }

    ses.sendEmail(source, to, cc, bcc, msg.build());
  }

  private void sendSmtp(final SiteConfigurationNotification notification, final String to,
      final String cc, final String bcc, final String subject, final String text, final String html)
      throws IOException {
    
    SiteConfigurationNotificationSmtp smtp = notification.smtp();
    if (smtp == null) {
      throw new IOException("SMTP notification configuration is not set");
    }

    SecretsManagerService secrets = serviceCache.getExtension(SecretsManagerService.class);
    String value = secrets.loadSecretStringByArn(smtp.credentialsSecretArn());

    try {
      Map<?, ?> credentials = GsonUtil.getInstance().fromJson(value, Map.class);
      String username = getCredential(credentials, "username");
      String password = getCredential(credentials, "password");

      smtpEmailSender.send(new SmtpEmail(notification.email(), to, cc, bcc, subject, text, html, smtp,
          username, password));

    } catch (JsonParseException e) {
      throw new IOException("SMTP credentials secret must contain valid JSON", e);
    }
  }

  /**
   * Sends a test email using the site's saved notification configuration.
   *
   * @param siteId site identifier
   * @param to recipient email address
   * @throws IOException when the configuration is invalid or delivery fails
   */
  public void sendTestNotification(final String siteId, final String to) throws IOException {
    ConfigService configService = serviceCache.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get(siteId);
    String text = "This is a test notification from FormKiQ for site " + siteId + ".";
    sendEmail(config, to, null, null, "FormKiQ test notification", text, null);
  }
}
