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

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_HTML;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_SUBJECT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TEXT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_BCC;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_CC;
import java.io.IOException;
import java.util.List;

import com.formkiq.aws.ses.SesService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.Message.Builder;

/**
 * {@link DocumentAction} for Notification {@link ActionType}.
 */
public class NotificationAction implements DocumentAction {

  /** {@link SesService}. */
  private final SesService ses;
  /** {@link ConfigService}. */
  private final String source;

  /**
   * constructor.
   * 
   * @param siteId {@link String}
   * @param serviceCache {@link AwsServiceCache}
   */
  public NotificationAction(final String siteId, final AwsServiceCache serviceCache) {
    this.ses = serviceCache.getExtension(SesService.class);

    ConfigService configService = serviceCache.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get(siteId);
    this.source = config.notificationEmail();
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException {

    String cc = (String) action.parameters().get(PARAMETER_NOTIFICATION_TO_CC);
    String bcc = (String) action.parameters().get(PARAMETER_NOTIFICATION_TO_BCC);
    String subject = (String) action.parameters().get(PARAMETER_NOTIFICATION_SUBJECT);
    String text = (String) action.parameters().get(PARAMETER_NOTIFICATION_TEXT);
    String html = (String) action.parameters().get(PARAMETER_NOTIFICATION_HTML);

    Builder msg = Message.builder().subject(Content.builder().data(subject).build());

    if (!isEmpty(text)) {
      msg = msg.body(Body.builder().text(Content.builder().data(text).build()).build());
    }

    if (!isEmpty(html)) {
      msg = msg.body(Body.builder().html(Content.builder().data(html).build()).build());
    }

    this.ses.sendEmail(this.source, cc, bcc, msg.build());
    return new ProcessActionStatus(ActionStatus.COMPLETE);
  }
}
