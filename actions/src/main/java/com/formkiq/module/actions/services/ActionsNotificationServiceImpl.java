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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.module.events.document.DocumentEventType.ACTIONS;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.util.List;
import java.util.Optional;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.document.DocumentEvent;

/**
 * 
 * Implementation of {@link ActionsNotificationService}.
 *
 */
public class ActionsNotificationServiceImpl implements ActionsNotificationService {

  /** {@link EventService}. */
  private EventService documentEventService;

  /**
   * constructor.
   * 
   * @param eventService {@link EventService}
   */
  public ActionsNotificationServiceImpl(final EventService eventService) {
    if (eventService == null) {
      throw new IllegalArgumentException("'eventService' is required");
    }
    this.documentEventService = eventService;
  }

  @Override
  public boolean publishNextActionEvent(final List<Action> actions, final String siteId,
      final String documentId) {

    boolean publishedEvent = false;

    Optional<Action> o =
        actions.stream().filter(new ActionStatusPredicate(ActionStatus.RUNNING)).findFirst();

    if (o.isEmpty()) {

      o = actions.stream().filter(new ActionStatusPredicate(ActionStatus.PENDING)).findFirst();

      if (o.isPresent()) {
        publishedEvent = publishNextActionEvent(siteId, documentId);
      }
    }

    return publishedEvent;
  }

  @Override
  public boolean publishNextActionEvent(final String siteId, final String documentId) {
    String site = !isEmpty(siteId) ? siteId : DEFAULT_SITE_ID;
    DocumentEvent event = new DocumentEvent().siteId(site).documentId(documentId).type(ACTIONS);
    this.documentEventService.publish(event);
    return true;
  }
}
