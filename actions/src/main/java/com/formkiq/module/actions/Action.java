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
package com.formkiq.module.actions;

import java.util.Map;

/**
 * Action.
 */
public class Action {

  /** Action Parameters. */
  private Map<String, String> parameters;
  /** Is Action Completed. */
  private ActionStatus status;
  /** Type of Action. */
  private ActionType type;
  /** UserId. */
  private String userId;

  /**
   * constructor.
   */
  public Action() {
    this.status = ActionStatus.PENDING;
  }

  /**
   * Get Action parameters {@link Map}.
   * 
   * @return {@link Map}
   */
  public Map<String, String> parameters() {
    return this.parameters;
  }

  /**
   * Set Action parameters {@link Map}.
   * 
   * @param map {@link Map}
   * @return {@link Action}
   */
  public Action parameters(final Map<String, String> map) {
    this.parameters = map;
    return this;
  }

  /**
   * Get {@link ActionStatus}.
   * 
   * @return {@link ActionStatus}
   */
  public ActionStatus status() {
    return this.status;
  }

  /**
   * Set {@link ActionStatus}.
   * 
   * @param actionStatus {@link ActionStatus}
   * @return {@link Action}
   */
  public Action status(final ActionStatus actionStatus) {
    this.status = actionStatus;
    return this;
  }

  /**
   * Get {@link ActionType}.
   * 
   * @return {@link ActionType}
   */
  public ActionType type() {
    return this.type;
  }

  /**
   * Set {@link ActionType}.
   * 
   * @param actionType {@link ActionType}
   * @return {@link Action}
   */
  public Action type(final ActionType actionType) {
    this.type = actionType;
    return this;
  }

  /**
   * Get UserId.
   * 
   * @return {@link String}
   */
  public String userId() {
    return this.userId;
  }

  /**
   * Set UserId.
   * 
   * @param user {@link String}
   * @return {@link Action}
   */
  public Action userId(final String user) {
    this.userId = user;
    return this;
  }
}
