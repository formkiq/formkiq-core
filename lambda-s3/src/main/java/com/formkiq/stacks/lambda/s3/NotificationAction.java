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
package com.formkiq.stacks.lambda.s3;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_HTML;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_SUBJECT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TEXT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_BCC;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_CC;
import static com.formkiq.stacks.dynamodb.ConfigService.NOTIFICATION_EMAIL;
import java.io.IOException;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.ses.SesService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.Message.Builder;

/**
 * {@link DocumentAction} for Notification {@link ActionType}.
 */
public class NotificationAction implements DocumentAction {

  /** {@link SesService}. */
  private SesService ses;
  /** {@link ConfigService}. */
  private String source;

  /**
   * constructor.
   * 
   * @param siteId {@link String}
   * @param serviceCache {@link AwsServiceCache}
   */
  public NotificationAction(final String siteId, final AwsServiceCache serviceCache) {
    this.ses = serviceCache.getExtension(SesService.class);

    ConfigService configService = serviceCache.getExtension(ConfigService.class);
    DynamicObject config = configService.get(siteId);
    this.source = config.getString(NOTIFICATION_EMAIL);
  }

  @Override
  public void run(final LambdaLogger logger, final String siteId, final String documentId,
      final Action action) throws IOException {

    String cc = action.parameters().get(PARAMETER_NOTIFICATION_TO_CC);
    String bcc = action.parameters().get(PARAMETER_NOTIFICATION_TO_BCC);
    String subject = action.parameters().get(PARAMETER_NOTIFICATION_SUBJECT);
    String text = action.parameters().get(PARAMETER_NOTIFICATION_TEXT);
    String html = action.parameters().get(PARAMETER_NOTIFICATION_HTML);

    Builder msg = Message.builder().subject(Content.builder().data(subject).build());

    if (!isEmpty(text)) {
      msg = msg.body(Body.builder().text(Content.builder().data(text).build()).build());
    }

    if (!isEmpty(html)) {
      msg = msg.body(Body.builder().html(Content.builder().data(html).build()).build());
    }

    this.ses.sendEmail(this.source, cc, bcc, msg.build());
  }
}
