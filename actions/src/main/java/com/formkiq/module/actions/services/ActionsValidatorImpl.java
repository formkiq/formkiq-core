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
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_HTML;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_SUBJECT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TEXT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_BCC;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_CC;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TYPE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;

/**
 * 
 * {@link ActionsValidator}.
 *
 */
public class ActionsValidatorImpl implements ActionsValidator {

  /** ChatGpt Api Key. */
  private static final String CHATGPT_API_KEY = "ChatGptApiKey";
  /** Notification Email. */
  private static final String NOTIFICATION_EMAIL = "NotificationEmail";

  private Map<String, String> getParameters(final Action action) {
    Map<String, String> parameters =
        action.parameters() != null ? action.parameters() : Collections.emptyMap();
    return parameters;
  }

  private boolean hasValue(final Map<String, String> parameters, final String key) {
    return parameters != null && parameters.containsKey(key)
        && !isEmpty(parameters.get(key).trim());
  }

  /**
   * Validate Document Tagging.
   * 
   * @param configs {@link DynamicObject}
   * @param parameters {@link Map}
   * @param errors {@link Collections} {@link ValidationError}
   */
  private void validateDocumentTagging(final DynamicObject configs,
      final Map<String, String> parameters, final Collection<ValidationError> errors) {

    String chatGptApiKey = configs.getString(CHATGPT_API_KEY);
    if (!parameters.containsKey("tags")) {
      errors.add(new ValidationErrorImpl().key("parameters.tags")
          .error("action 'tags' parameter is required"));
    }

    if (!parameters.containsKey("engine")) {
      errors.add(new ValidationErrorImpl().key("parameters.engine")
          .error("action 'engine' parameter is required"));
    } else if (!"chatgpt".equals(parameters.getOrDefault("engine", ""))) {
      errors.add(
          new ValidationErrorImpl().key("parameters.engine").error("invalid 'engine' parameter"));
    } else if (Strings.isEmpty(chatGptApiKey)) {
      errors.add(new ValidationErrorImpl().error("chatgpt 'api key' is not configured"));
    }
  }

  /**
   * Validate Notification Email.
   * 
   * @param configs {@link DynamicObject}
   * @param action {@link Action}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateNotificationEmail(final DynamicObject configs, final Action action,
      final Collection<ValidationError> errors) {

    String notificationEmail = configs.getString(NOTIFICATION_EMAIL);

    if (isEmpty(notificationEmail)) {
      errors.add(new ValidationErrorImpl().key("parameters.notificationEmail")
          .error("notificationEmail is not configured"));
    } else {

      Map<String, String> parameters = getParameters(action);

      for (String parameter : Arrays.asList(PARAMETER_NOTIFICATION_TYPE,
          PARAMETER_NOTIFICATION_SUBJECT)) {
        if (!hasValue(parameters, parameter)) {
          errors.add(new ValidationErrorImpl().key("parameters." + parameter)
              .error("action '" + parameter + "' parameter is required"));
        }
      }

      if (!hasValue(parameters, PARAMETER_NOTIFICATION_TO_CC)
          && !hasValue(parameters, PARAMETER_NOTIFICATION_TO_BCC)) {
        errors.add(new ValidationErrorImpl().key("parameters." + PARAMETER_NOTIFICATION_TO_CC)
            .error("action '" + PARAMETER_NOTIFICATION_TO_CC + "' or '"
                + PARAMETER_NOTIFICATION_TO_BCC + "' is required"));
      }

      if (!hasValue(parameters, PARAMETER_NOTIFICATION_TEXT)
          && !hasValue(parameters, PARAMETER_NOTIFICATION_HTML)) {
        errors.add(new ValidationErrorImpl().key("parameters." + PARAMETER_NOTIFICATION_TEXT)
            .error("action '" + PARAMETER_NOTIFICATION_TEXT + "' or '" + PARAMETER_NOTIFICATION_HTML
                + "' is required"));
      }
    }
  }

  private void validateWait(final Action action, final Collection<ValidationError> errors) {

    if (isEmpty(action.queueId())) {
      errors.add(new ValidationErrorImpl().key("queueId").error("'queueId' is required"));
    }
  }

  @Override
  public Collection<ValidationError> validation(final Action action, final DynamicObject configs) {
    Collection<ValidationError> errors = new ArrayList<>();

    if (action == null) {

      errors.add(new ValidationErrorImpl().error("action is required"));

    } else {

      if (action.type() == null) {

        errors.add(new ValidationErrorImpl().key("type").error("action 'type' is required"));

      } else if (isEmpty(action.userId())) {

        errors.add(new ValidationErrorImpl().key("userId").error("action 'userId' is required"));

      } else {

        Map<String, String> parameters = getParameters(action);
        if (ActionType.WEBHOOK.equals(action.type()) && !parameters.containsKey("url")) {
          errors.add(new ValidationErrorImpl().key("parameters.url")
              .error("action 'url' parameter is required"));
        } else if (ActionType.DOCUMENTTAGGING.equals(action.type())) {
          validateDocumentTagging(configs, parameters, errors);
        } else if (ActionType.NOTIFICATION.equals(action.type())) {
          validateNotificationEmail(configs, action, errors);
        } else if (ActionType.QUEUE.equals(action.type())) {
          validateWait(action, errors);
        }
      }
    }

    return errors;
  }

  @Override
  public List<Collection<ValidationError>> validation(final List<Action> actions,
      final DynamicObject configs) {
    List<Collection<ValidationError>> errors = new ArrayList<>();
    actions.forEach(a -> errors.add(validation(a, configs)));
    return errors;
  }

}
