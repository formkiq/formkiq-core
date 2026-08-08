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

import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_HTML;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_SUBJECT;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TEXT;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_BCC;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_CC;
import static com.formkiq.aws.dynamodb.actions.ActionParameters.PARAMETER_NOTIFICATION_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.aws.dynamodb.actions.ActionBuilder;
import com.formkiq.aws.dynamodb.actions.ActionStatus;
import com.formkiq.aws.dynamodb.actions.ActionType;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.secretsmanager.SecretsManagerService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.lambdaservices.logger.LoggerRecorder;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotification;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationProvider;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationSmtp;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationSmtpConnectionSecurity;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

/** Tests for {@link NotificationAction}. */
class NotificationActionTest {

  private static final class CapturingSmtpEmailSender extends SmtpEmailSender {
    /** Captured SMTP email. */
    private SmtpEmail email;

    @Override
    void send(final SmtpEmail value) {
      this.email = value;
    }
  }

  private static final class TestConfigService implements ConfigService {
    /** Site configuration returned by the test service. */
    private final SiteConfiguration configuration;

    private TestConfigService(final SiteConfiguration config) {
      this.configuration = config;
    }

    @Override
    public void delete(final String siteId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SiteConfiguration get(final String siteId) {
      return this.configuration;
    }

    @Override
    public long getIncrement(final String siteId, final String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Long> getIncrements(final String siteId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long increment(final String siteId, final String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean save(final String siteId, final SiteConfiguration config) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestSecretsManagerService implements SecretsManagerService {
    /** Secret value returned by the test service. */
    private final String secret;

    private TestSecretsManagerService(final String value) {
      this.secret = value;
    }

    @Override
    public String createSecret(final String name, final String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String createSecret(final String name, final byte[] value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(final String arn) {
      throw new UnsupportedOperationException();
    }

    @Override
    public byte[] loadSecretBytesByArn(final String arn) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String loadSecretStringByArn(final String arn) {
      return this.secret;
    }
  }

  private Action emailAction() {
    DocumentArtifact document = DocumentArtifact.of("documentId", null);
    return new ActionBuilder().document(document).indexUlid().type(ActionType.NOTIFICATION)
        .parameters(Map.of(PARAMETER_NOTIFICATION_TYPE, "EMAIL", PARAMETER_NOTIFICATION_TO_CC,
            "cc@example.com", PARAMETER_NOTIFICATION_TO_BCC, "bcc@example.com",
            PARAMETER_NOTIFICATION_SUBJECT, "Contract expires", PARAMETER_NOTIFICATION_TEXT,
            "Plain text", PARAMETER_NOTIFICATION_HTML, "<p>HTML text</p>"))
        .userId("System").build((String) null);
  }

  private SmtpEmail smtpEmail(final SiteConfigurationNotificationSmtpConnectionSecurity security) {
    SiteConfigurationNotificationSmtp smtp =
        new SiteConfigurationNotificationSmtp("smtp.example.com", 587, security, "secretArn");
    return new SmtpEmail("from@example.com", "to@example.com", "cc@example.com", "bcc@example.com",
        "Subject", "Plain text", "<p>HTML text</p>", smtp, "username", "password");
  }

  private AwsServiceCache smtpServiceCache(final String secret,
      final SiteConfigurationNotificationSmtpConnectionSecurity security) {
    SiteConfigurationNotificationSmtp smtp =
        new SiteConfigurationNotificationSmtp("smtp.example.com", 587, security, "secretArn");
    SiteConfigurationNotification notification = new SiteConfigurationNotification(
        "from@example.com", SiteConfigurationNotificationProvider.SMTP, smtp);
    SiteConfiguration config =
        SiteConfiguration.builder().notification(notification).build((String) null);

    AwsServiceCache cache = new AwsServiceCache();
    cache.register(ConfigService.class, new ClassServiceExtension<>(new TestConfigService(config)));
    cache.register(SecretsManagerService.class,
        new ClassServiceExtension<>(new TestSecretsManagerService(secret)));
    return cache;
  }

  /** IN_APP notifications complete without loading email services. */
  @Test
  void testInAppNotification() throws IOException {
    DocumentArtifact document = DocumentArtifact.of("documentId", null);
    Action action = new ActionBuilder().document(document).indexUlid().type(ActionType.NOTIFICATION)
        .parameters(Map.of(PARAMETER_NOTIFICATION_TYPE, "IN_APP")).userId("System")
        .build((String) null);

    var result = new NotificationAction(null, new AwsServiceCache()).run(new LoggerRecorder(), null,
        document, List.of(), action);

    assertEquals(ActionStatus.COMPLETE, result.actionStatus());
  }

  /** SMTP credentials are not exposed by the email value's string representation. */
  @Test
  void testSmtpEmailToStringRedactsCredentials() {
    assertEquals("SmtpEmail[redacted]",
        smtpEmail(SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS).toString());
  }

  /** Implicit TLS encrypts the SMTP connection from the start. */
  @Test
  void testSmtpImplicitTlsProperties() {
    Session session = new SmtpEmailSender()
        .createSession(smtpEmail(SiteConfigurationNotificationSmtpConnectionSecurity.IMPLICIT_TLS));

    assertEquals("true", session.getProperty("mail.smtp.ssl.enable"));
    assertNull(session.getProperty("mail.smtp.starttls.enable"));
  }

  /** SMTP messages preserve all recipients and text/HTML alternatives. */
  @Test
  void testSmtpMessage() throws Exception {
    SmtpEmail email = smtpEmail(SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS);
    SmtpEmailSender sender = new SmtpEmailSender();
    MimeMessage message = sender.createMessage(sender.createSession(email), email);

    assertEquals("from@example.com", message.getFrom()[0].toString());
    assertEquals("to@example.com",
        message.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
    assertEquals("cc@example.com",
        message.getRecipients(MimeMessage.RecipientType.CC)[0].toString());
    assertEquals("bcc@example.com",
        message.getRecipients(MimeMessage.RecipientType.BCC)[0].toString());
    assertEquals("Subject", message.getSubject());
    Multipart content = assertInstanceOf(Multipart.class, message.getContent());
    assertEquals(2, content.getCount());
    assertEquals("Plain text", content.getBodyPart(0).getContent());
    assertEquals("<p>HTML text</p>", content.getBodyPart(1).getContent());
  }

  /** SMTP notifications load credentials and send the action contents. */
  @Test
  void testSmtpNotification() throws IOException {
    CapturingSmtpEmailSender sender = new CapturingSmtpEmailSender();
    AwsServiceCache cache =
        smtpServiceCache("{\"username\":\"smtp-user\",\"password\":\"smtp-password\"}",
            SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS);
    Action action = emailAction();
    DocumentArtifact document = DocumentArtifact.of("documentId", null);

    var result = new NotificationAction(null, cache, sender).run(new LoggerRecorder(), null,
        document, List.of(), action);

    assertEquals(ActionStatus.COMPLETE, result.actionStatus());
    assertEquals("from@example.com", sender.email.source());
    assertEquals("cc@example.com", sender.email.cc());
    assertEquals("bcc@example.com", sender.email.bcc());
    assertEquals("Contract expires", sender.email.subject());
    assertEquals("Plain text", sender.email.text());
    assertEquals("<p>HTML text</p>", sender.email.html());
    assertEquals("smtp-user", sender.email.username());
    assertEquals("smtp-password", sender.email.password());
  }

  /** SMTP credentials must be valid JSON. */
  @Test
  void testSmtpNotificationMalformedCredentials() {
    CapturingSmtpEmailSender sender = new CapturingSmtpEmailSender();
    AwsServiceCache cache =
        smtpServiceCache("not-json", SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS);
    Action action = emailAction();
    DocumentArtifact document = DocumentArtifact.of("documentId", null);

    IOException exception =
        assertThrows(IOException.class, () -> new NotificationAction(null, cache, sender)
            .run(new LoggerRecorder(), null, document, List.of(), action));

    assertEquals("SMTP credentials secret must contain valid JSON", exception.getMessage());
    assertNull(sender.email);
  }

  /** SMTP credential Secrets require both username and password fields. */
  @Test
  void testSmtpNotificationMissingPassword() {
    CapturingSmtpEmailSender sender = new CapturingSmtpEmailSender();
    AwsServiceCache cache = smtpServiceCache("{\"username\":\"smtp-user\"}",
        SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS);
    Action action = emailAction();
    DocumentArtifact document = DocumentArtifact.of("documentId", null);

    IOException exception =
        assertThrows(IOException.class, () -> new NotificationAction(null, cache, sender)
            .run(new LoggerRecorder(), null, document, List.of(), action));

    assertEquals("SMTP credentials secret must contain a non-empty 'password'",
        exception.getMessage());
    assertNull(sender.email);
  }

  /** STARTTLS requires the SMTP connection to be upgraded to TLS. */
  @Test
  void testSmtpStartTlsProperties() {
    Session session = new SmtpEmailSender()
        .createSession(smtpEmail(SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS));

    assertEquals("true", session.getProperty("mail.smtp.auth"));
    assertEquals("true", session.getProperty("mail.smtp.starttls.enable"));
    assertEquals("true", session.getProperty("mail.smtp.starttls.required"));
    assertNull(session.getProperty("mail.smtp.ssl.enable"));
  }

  /** Test notifications use a To recipient and fixed diagnostic content. */
  @Test
  void testSmtpTestNotification() throws IOException {
    CapturingSmtpEmailSender sender = new CapturingSmtpEmailSender();
    AwsServiceCache cache =
        smtpServiceCache("{\"username\":\"smtp-user\",\"password\":\"smtp-password\"}",
            SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS);

    new NotificationAction(null, cache, sender).sendTestNotification("site123",
        "recipient@example.com");

    assertEquals("recipient@example.com", sender.email.to());
    assertNull(sender.email.cc());
    assertNull(sender.email.bcc());
    assertEquals("FormKiQ test notification", sender.email.subject());
    assertEquals("This is a test notification from FormKiQ for site site123.", sender.email.text());
    assertNull(sender.email.html());
  }
}
