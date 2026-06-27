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

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.validation.ValidationBuilder;

/**
 * 
 * {@link ActionsValidator}.
 *
 */
public class ActionsValidatorImpl implements ActionsValidator {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public ActionsValidatorImpl(final DynamoDbService dbService) {
    this.db = dbService;
  }

  private Map<String, Object> getParameters(final Action action) {
    return action.parameters() != null ? action.parameters() : Collections.emptyMap();
  }

  private void validateActionParameters(final String siteId, final Action action,
      final String chatGptApiKey, final String notificationsEmail, final ValidationBuilder vb) {

    Map<String, Object> parameters = getParameters(action);
    action.type().validate(db, siteId, action, parameters, chatGptApiKey, notificationsEmail, vb);
  }

  @Override
  public void validation(final ValidationBuilder vb, final String siteId, final Action action,
      final String chatGptApiKey, final String notificationsEmail) {

    if (action == null) {

      vb.addError(null, "action is required");

    } else {

      if (action.type() == null) {

        vb.addError("type", "action 'type' is required");

      } else if (isEmpty(action.userId())) {

        vb.addError("userId", "action 'userId' is required");

      } else {

        validateActionParameters(siteId, action, chatGptApiKey, notificationsEmail, vb);
      }
    }
  }

  @Override
  public void validation(final ValidationBuilder vb, final String siteId,
      final List<Action> actions, final String chatGptApiKey, final String notificationsEmail) {
    actions.forEach(a -> validation(vb, siteId, a, chatGptApiKey, notificationsEmail));
  }
}
