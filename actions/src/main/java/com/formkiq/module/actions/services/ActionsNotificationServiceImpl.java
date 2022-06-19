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
package com.formkiq.module.actions.services;

import static com.formkiq.module.documentevents.DocumentEventType.ACTIONS;
import java.util.List;
import java.util.Optional;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.module.actions.Action;
import com.formkiq.module.documentevents.DocumentEvent;
import com.formkiq.module.documentevents.DocumentEventService;
import com.formkiq.module.documentevents.DocumentEventServiceSns;

/**
 * 
 * Implementation of {@link ActionsNotificationService}.
 *
 */
public class ActionsNotificationServiceImpl implements ActionsNotificationService {

  /** Document Event Actions Topic. */
  private String topicArn;
  /** {@link DocumentEventService}. */
  private DocumentEventService documentEventService;

  /**
   * constructor.
   * 
   * @param documentActionsTopicArn {@link String}
   * @param snsBuilder {@link SnsConnectionBuilder}
   */
  public ActionsNotificationServiceImpl(final String documentActionsTopicArn,
      final SnsConnectionBuilder snsBuilder) {
    this.topicArn = documentActionsTopicArn;
    this.documentEventService = new DocumentEventServiceSns(snsBuilder);
  }

  @Override
  public void publishNextActionEvent(final List<Action> actions, final String siteId,
      final String documentId) {

    Optional<Action> o = actions.stream().filter(new NextActionPredicate()).findFirst();

    if (o.isPresent()) {
      DocumentEvent event = new DocumentEvent().siteId(siteId).documentId(documentId).type(ACTIONS);
      this.documentEventService.publish(this.topicArn, event);
    }
  }
}
