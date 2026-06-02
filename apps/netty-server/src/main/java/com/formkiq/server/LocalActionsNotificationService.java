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
package com.formkiq.server;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.module.events.document.DocumentEventType.ACTIONS;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.aws.dynamodb.actions.ActionStatus;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.module.actions.services.ActionStatusPredicate;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.events.document.DocumentEvent;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.lambda.s3.DocumentActionsProcessor;

/**
 * Local Netty implementation of action notifications.
 */
final class LocalActionsNotificationService implements ActionsNotificationService {

  /** Max number of action events to process in one local notification. */
  private static final int MAX_LOCAL_EVENTS = 100;
  /** {@link ActionsService}. */
  private final ActionsService actionsService;
  /** {@link ExecutorService}. */
  private final ExecutorService executorService;
  /** {@link Logger}. */
  private final Logger logger;
  /** {@link DocumentActionsProcessor}. */
  private final DocumentActionsProcessor processor;

  /**
   * constructor.
   *
   * @param service {@link ActionsService}
   * @param actionsProcessor {@link DocumentActionsProcessor}
   * @param log {@link Logger}
   * @param executor {@link ExecutorService}
   */
  LocalActionsNotificationService(final ActionsService service,
      final DocumentActionsProcessor actionsProcessor, final Logger log,
      final ExecutorService executor) {
    this.actionsService = service;
    this.processor = actionsProcessor;
    this.logger = log;
    this.executorService = executor;
  }

  private void processLocalActionEvents(final DocumentEvent event) {
    DocumentArtifact document = DocumentArtifact.of(event.documentId(), event.artifactId());

    for (int i = 0; i < MAX_LOCAL_EVENTS; i++) {
      this.processor.processEvent(this.logger, event);

      List<Action> actions = this.actionsService.getActions(event.siteId(), document);
      boolean running = actions.stream().anyMatch(new ActionStatusPredicate(ActionStatus.RUNNING));
      boolean pending = actions.stream().anyMatch(
          new ActionStatusPredicate(ActionStatus.PENDING, ActionStatus.WAITING_FOR_RETRY));

      if (running || !pending) {
        break;
      }
    }
  }

  @Override
  public boolean publishNextActionEvent(final List<Action> actions, final String siteId,
      final String documentId) {

    boolean publishedEvent = false;

    Optional<Action> running =
        actions.stream().filter(new ActionStatusPredicate(ActionStatus.RUNNING)).findFirst();

    if (running.isEmpty()) {
      Optional<Action> pending =
          actions.stream().filter(new ActionStatusPredicate(ActionStatus.PENDING)).findFirst();

      if (pending.isPresent()) {
        publishedEvent = publishNextActionEvent(siteId, documentId, null);
      }
    }

    return publishedEvent;
  }

  @Override
  public boolean publishNextActionEvent(final String siteId,
      final DocumentArtifact documentArtifact) {
    return publishNextActionEvent(siteId, documentArtifact.documentId(),
        documentArtifact.artifactId());
  }

  @Override
  public boolean publishNextActionEvent(final String siteId, final String documentId,
      final String artifactId) {

    String site = !isEmpty(siteId) ? siteId : DEFAULT_SITE_ID;
    DocumentEvent event = new DocumentEvent().siteId(site).documentId(documentId)
        .artifactId(artifactId).type(ACTIONS);

    this.executorService.submit(() -> {
      try {
        processLocalActionEvents(event);
      } catch (Exception e) {
        this.logger.error(e);
      }
    });

    return true;
  }
}
