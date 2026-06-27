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
package com.formkiq.aws.ses;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Destination.Builder;
import software.amazon.awssdk.services.ses.model.IdentityType;
import software.amazon.awssdk.services.ses.model.ListIdentitiesRequest;
import software.amazon.awssdk.services.ses.model.ListIdentitiesResponse;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

/**
 * 
 * SMTP Services.
 *
 */
public class SesService {

  /** List Max Identiify. */
  private static final int LIST_MAX = 1000;
  /** {@link SesClient}. */
  private SesClient client;

  /**
   * constructor.
   * 
   * @param smtpConnection {@link SesConnectionBuilder}
   */
  public SesService(final SesConnectionBuilder smtpConnection) {
    this.client = smtpConnection.build();
  }

  /**
   * Get SES Email Addresses.
   * 
   * @return {@link ListIdentitiesResponse}
   */
  public ListIdentitiesResponse getEmailAddresses() {
    return this.client.listIdentities(ListIdentitiesRequest.builder()
        .identityType(IdentityType.EMAIL_ADDRESS).maxItems(Integer.valueOf(LIST_MAX)).build());
  }

  /**
   * Send SES Message.
   * 
   * @param source {@link String}
   * @param cc {@link String}
   * @param bcc {@link String}
   * @param message {@link Message}
   * @return {@link SendEmailResponse}
   */
  public SendEmailResponse sendEmail(final String source, final String cc, final String bcc,
      final Message message) {
    Builder destination = Destination.builder();

    if (cc != null) {
      destination = destination.ccAddresses(cc.split(","));
    }

    if (bcc != null) {
      destination = destination.bccAddresses(bcc.split(","));
    }

    return this.client.sendEmail(SendEmailRequest.builder().source(source)
        .destination(destination.build()).message(message).build());
  }
}
