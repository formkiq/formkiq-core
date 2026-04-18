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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionBuilder;
import com.formkiq.module.actions.ActionStatus;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 *
 * FormKiQ Actions Service.
 *
 */
public interface ActionsService {

  /**
   * Delete {@link Action}.
   *
   * @param actions {@link Collection} {@link Action}
   */
  void deleteActions(Collection<Action> actions);

  /**
   * Delete Document Actions.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   */
  void deleteActions(String siteId, DocumentArtifact document);

  /**
   * Find Document in Queue.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param queueId {@link String}
   * @return {@link Action}
   */
  Action findActionInQueue(String siteId, DocumentArtifact document, String queueId);

  /**
   * Find Documents in Queue.
   *
   * @param siteId {@link String}
   * @param queueId {@link String}
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link Pagination} {@link Action}
   */
  Pagination<Action> findDocumentsInQueue(String siteId, String queueId, String nextToken,
      int limit);

  /**
   * Find Documents with FAILED status.
   *
   * @param siteId {@link String}
   * @param status {@link ActionStatus}
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link Pagination}
   */
  Pagination<DocumentArtifact> findDocumentsWithStatus(String siteId, ActionStatus status,
      String nextToken, int limit);

  /**
   * Get List of {@link Action} by {@link ActionStatus}.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param status {@link ActionStatus}
   * @return {@link List} {@link ActionStatus}
   */
  List<Action> getAction(String siteId, DocumentArtifact document, ActionStatus status);

  /**
   * Get {@link List} {@link Action} for a document.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @return {@link List} {@link Action}
   */
  List<Action> getActions(String siteId, DocumentArtifact document);

  /**
   * Get {@link List} {@link Action} for a document.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link Pagination} {@link Action}
   */
  Pagination<Action> getActions(String siteId, DocumentArtifact document, String nextToken,
      int limit);

  /**
   * Whether SiteId / DocumentId combination has any actions.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @return boolean
   */
  boolean hasActions(String siteId, DocumentArtifact document);

  /**
   * Insert {@link Action}.
   *
   * @param siteId {@link String}
   * @param currentAction {@link Action}
   * @param insertedAction {@link ActionBuilder}
   */
  void insertBeforeAction(String siteId, Action currentAction, ActionBuilder insertedAction);

  /**
   * Get Previous Index.
   *
   * @param index {@link String}
   * @return {@link String}
   */
  String previousIndex(String index);

  /**
   * Save {@link List} {@link Action}.
   *
   * @param actions {@link List} {@link Action}
   * @return {@link List} {@link Map}
   */
  List<Map<String, AttributeValue>> saveNewActions(List<Action> actions);

  /**
   * Update {@link Action}.
   *
   * @param action {@link Action}
   */
  void updateAction(Action action);

  /**
   * Update {@link Collection} {@link Action}.
   *
   * @param actions {@link Collection} {@link Action}
   */
  void updateActions(Collection<Action> actions);
}
