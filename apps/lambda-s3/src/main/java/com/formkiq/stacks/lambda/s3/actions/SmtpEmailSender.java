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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationSmtp;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotificationSmtpConnectionSecurity;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/** Sends email through an authenticated SMTP server. */
class SmtpEmailSender {

  /** SMTP connection timeout in milliseconds. */
  private static final int CONNECTION_TIMEOUT = 10000;

  private void addRecipients(final MimeMessage message, final RecipientType type,
      final String addresses) throws MessagingException {
    if (!isEmpty(trim(addresses))) {
      message.addRecipients(type, InternetAddress.parse(addresses));
    }
  }

  MimeMessage createMessage(final Session session, final SmtpEmail email)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(email.source()));
    addRecipients(message, RecipientType.TO, email.to());
    addRecipients(message, RecipientType.CC, email.cc());
    addRecipients(message, RecipientType.BCC, email.bcc());
    message.setSubject(email.subject(), StandardCharsets.UTF_8.name());

    if (!isEmpty(trim(email.text())) && !isEmpty(trim(email.html()))) {
      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(email.text(), StandardCharsets.UTF_8.name());
      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(email.html(), "text/html; charset=UTF-8");
      Multipart content = new MimeMultipart("alternative");
      content.addBodyPart(textPart);
      content.addBodyPart(htmlPart);
      message.setContent(content);
    } else if (!isEmpty(trim(email.html()))) {
      message.setContent(email.html(), "text/html; charset=UTF-8");
    } else {
      message.setText(email.text(), StandardCharsets.UTF_8.name());
    }

    message.saveChanges();
    return message;
  }

  Session createSession(final SmtpEmail email) {
    SiteConfigurationNotificationSmtp smtp = email.smtp();
    Properties properties = new Properties();
    properties.setProperty("mail.smtp.auth", "true");
    properties.setProperty("mail.smtp.host", smtp.host());
    properties.setProperty("mail.smtp.port", smtp.port().toString());
    properties.setProperty("mail.smtp.connectiontimeout", Integer.toString(CONNECTION_TIMEOUT));
    properties.setProperty("mail.smtp.timeout", Integer.toString(CONNECTION_TIMEOUT));
    properties.setProperty("mail.smtp.writetimeout", Integer.toString(CONNECTION_TIMEOUT));

    if (smtp.connectionSecurity() == SiteConfigurationNotificationSmtpConnectionSecurity.STARTTLS) {
      properties.setProperty("mail.smtp.starttls.enable", "true");
      properties.setProperty("mail.smtp.starttls.required", "true");
    } else {
      properties.setProperty("mail.smtp.ssl.enable", "true");
    }

    return Session.getInstance(properties);
  }

  void send(final SmtpEmail email) throws IOException {
    try {
      Session session = createSession(email);
      MimeMessage message = createMessage(session, email);
      Transport transport = session.getTransport("smtp");
      try {
        SiteConfigurationNotificationSmtp smtp = email.smtp();
        transport.connect(smtp.host(), smtp.port(), email.username(), email.password());
        transport.sendMessage(message, message.getAllRecipients());
      } finally {
        transport.close();
      }
    } catch (MessagingException e) {
      throw new IOException("Unable to send SMTP notification", e);
    }
  }
}
