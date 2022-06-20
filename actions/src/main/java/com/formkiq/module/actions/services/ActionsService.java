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
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;

/**
 * 
 * FormKiQ Actions Service.
 *
 */
public interface ActionsService {

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
   * Save {@link List} {@link Action}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actions {@link List} {@link Action}
   */
  void saveActions(String siteId, String documentId, List<Action> actions);

  /**
   * Update {@link Action} {@link ActionStatus}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @param index int
   */
  void updateActionStatus(String siteId, String documentId, Action action, int index);

  /**
   * Updates {@link ActionStatus}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param type {@link ActionType}
   * @param status {@link ActionStatus}
   * @return {@link List} {@link Action}
   */
  List<Action> updateActionStatus(String siteId, String documentId, ActionType type,
      ActionStatus status);
}
