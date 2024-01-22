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

import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * FormKiQ Actions Service.
 *
 */
public interface ActionsService {

  /**
   * Delete Document Actions.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void deleteActions(String siteId, String documentId);

  /**
   * Find Document in Queue.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param queueId {@link String}
   * @return {@link Action}
   */
  Action findActionInQueue(String siteId, String documentId, String queueId);

  /**
   * Find Documents in Queue.
   * 
   * @param siteId {@link String}
   * @param queueId {@link String}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link PaginationResults} {@link Action}
   */
  PaginationResults<Action> findDocumentsInQueue(String siteId, String queueId,
      Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Find Documents with FAILED status.
   * 
   * @param siteId {@link String}
   * @param status {@link ActionStatus}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link PaginationResults}
   */
  PaginationResults<String> findDocumentsWithStatus(String siteId, ActionStatus status,
      Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Get {@link List} {@link Action} for a document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link List} {@link Action}
   */
  List<Action> getActions(String siteId, String documentId);

  /**
   * Whether SiteId / DocumentId combination has any actions.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return boolean
   */
  boolean hasActions(String siteId, String documentId);

  /**
   * Insert {@link Action}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actions {@link List} {@link Action}
   * @param currentAction {@link Action}
   * @param insertedAction {@link Action}
   */
  void insertBeforeAction(String siteId, String documentId, List<Action> actions,
      Action currentAction, Action insertedAction);

  /**
   * Save {@link Action}.
   * 
   * @param siteId {@link String}
   * @param action {@link Action}
   */
  void saveAction(String siteId, Action action);

  /**
   * Save {@link Action}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @param index int
   */
  void saveAction(String siteId, String documentId, Action action, int index);

  /**
   * Save {@link Action}.
   * 
   * @param siteId {@link String}
   * @param actions {@link List} {@link Action}
   */
  void saveActions(String siteId, List<Action> actions);

  /**
   * Save {@link List} {@link Action}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actions {@link List} {@link Action}
   * @return {@link List} {@link Map}
   */
  List<Map<String, AttributeValue>> saveNewActions(String siteId, String documentId,
      List<Action> actions);

  /**
   * Update {@link Action} {@link ActionStatus}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   */
  void updateActionStatus(String siteId, String documentId, Action action);

  /**
   * Update Document Workflow Status.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   */
  void updateDocumentWorkflowStatus(String siteId, String documentId, Action action);
}
