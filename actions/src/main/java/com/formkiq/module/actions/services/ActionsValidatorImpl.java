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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.Queue;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;

/**
 * 
 * {@link ActionsValidator}.
 *
 */
public class ActionsValidatorImpl implements ActionsValidator {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /** Valid image formats for resize action. */
  private static final List<String> VALID_IMAGE_FORMATS =
      List.of("bmp", "gif", "jpeg", "png", "tif");

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public ActionsValidatorImpl(final DynamoDbService dbService) {
    this.db = dbService;
  }

  private Map<String, String> getParameters(final Action action) {
    return action.parameters() != null ? action.parameters() : Collections.emptyMap();
  }

  private boolean hasValue(final Map<String, String> parameters, final String key) {
    return parameters != null && parameters.containsKey(key)
        && !isEmpty(parameters.get(key).trim());
  }

  /**
   * Validate Document Tagging.
   * 
   * @param chatGptApiKey {@link String}
   * @param parameters {@link Map}
   * @param errors {@link Collections} {@link ValidationError}
   */
  private void validateDocumentTagging(final String chatGptApiKey,
      final Map<String, String> parameters, final Collection<ValidationError> errors) {

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
   * @param notificationEmail {@link String}
   * @param action {@link Action}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateNotificationEmail(final String notificationEmail, final Action action,
      final Collection<ValidationError> errors) {

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

  private void validateQueue(final String siteId, final Action action,
      final Collection<ValidationError> errors) {

    if (isEmpty(action.queueId())) {
      errors.add(new ValidationErrorImpl().key("queueId").error("'queueId' is required"));
    } else {
      Queue q = new Queue().documentId(action.queueId());
      if (!this.db.exists(q.fromS(q.pk(siteId)), q.fromS(q.sk()))) {
        errors.add(new ValidationErrorImpl().key("queueId").error("'queueId' does not exist"));
      }
    }
  }

  private void validateIdp(final String siteId, final Action action,
      final Collection<ValidationError> errors) {

    Map<String, String> parameters = getParameters(action);

    if (!hasValue(parameters, "mappingId")) {
      errors.add(new ValidationErrorImpl().key("mappingId").error("'mappingId' is required"));

    } else {
      String mappingId = parameters.get("mappingId");
      MappingRecord m = new MappingRecord().setDocumentId(mappingId);

      if (!this.db.exists(m.fromS(m.pk(siteId)), m.fromS(m.sk()))) {
        errors.add(new ValidationErrorImpl().key("mappingId").error("'mappingId' does not exist"));
      }
    }
  }

  private void validateEventBridge(final Action action, final Collection<ValidationError> errors) {

    Map<String, String> parameters = getParameters(action);

    if (!hasValue(parameters, "eventBusName")) {
      errors.add(new ValidationErrorImpl().key("parameters.eventBusName")
          .error("'eventBusName' parameter is required"));
    }
  }

  private void validateResize(final Action action, final Collection<ValidationError> errors) {
    Map<String, String> parameters = getParameters(action);

    String widthParameterName = "width";
    String heightParameterName = "height";

    if ("auto".equals(parameters.get(widthParameterName))
        && "auto".equals(parameters.get(heightParameterName))) {
      errors.add(new ValidationErrorImpl().key("parameters." + widthParameterName)
          .error("'" + widthParameterName + "' and '" + heightParameterName
              + "' parameters cannot be both set to auto"));
    }

    validateDimension(parameters, errors, widthParameterName);
    validateDimension(parameters, errors, heightParameterName);
    validateImageFormat(parameters, errors);
  }

  private void validateDimension(final Map<String, String> parameters,
      final Collection<ValidationError> errors, final String dimension) {
    if (!hasValue(parameters, dimension)) {
      errors.add(new ValidationErrorImpl().key("parameters." + dimension)
          .error("'" + dimension + "' parameter is required"));
    } else {
      String value = parameters.get(dimension);

      if (!isGreaterThanZeroInteger(value) && !"auto".equals(value)) {
        errors.add(new ValidationErrorImpl().key("parameters." + dimension)
            .error("'" + dimension + "' parameter must be an integer > 0 or 'auto'"));
      }
    }
  }

  private static boolean isGreaterThanZeroInteger(final String value) {
    try {
      return Integer.parseInt(value) > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private void validateImageFormat(final Map<String, String> parameters,
      final Collection<ValidationError> errors) {
    String outputType = parameters.get("outputType");

    if (outputType != null && !VALID_IMAGE_FORMATS.contains(outputType)) {
      errors.add(new ValidationErrorImpl().key("parameters.outputType")
          .error("'outputType' parameter must be one of " + VALID_IMAGE_FORMATS));
    }
  }

  @Override
  public Collection<ValidationError> validation(final String siteId, final Action action,
      final String chatGptApiKey, final String notificationsEmail) {
    Collection<ValidationError> errors = new ArrayList<>();

    if (action == null) {

      errors.add(new ValidationErrorImpl().error("action is required"));

    } else {

      if (action.type() == null) {

        errors.add(new ValidationErrorImpl().key("type").error("action 'type' is required"));

      } else if (isEmpty(action.userId())) {

        errors.add(new ValidationErrorImpl().key("userId").error("action 'userId' is required"));

      } else {

        validateActionParameters(siteId, action, chatGptApiKey, notificationsEmail, errors);
      }
    }

    return errors;
  }

  @Override
  public List<Collection<ValidationError>> validation(final String siteId,
      final List<Action> actions, final String chatGptApiKey, final String notificationsEmail) {

    List<Collection<ValidationError>> errors = new ArrayList<>();
    actions.forEach(a -> errors.add(validation(siteId, a, chatGptApiKey, notificationsEmail)));
    return errors;
  }

  private void validateActionParameters(final String siteId, final Action action,
      final String chatGptApiKey, final String notificationsEmail,
      final Collection<ValidationError> errors) {

    Map<String, String> parameters = getParameters(action);
    if (ActionType.WEBHOOK.equals(action.type()) && !parameters.containsKey("url")) {
      errors.add(new ValidationErrorImpl().key("parameters.url")
          .error("action 'url' parameter is required"));
    } else if (ActionType.DOCUMENTTAGGING.equals(action.type())) {
      validateDocumentTagging(chatGptApiKey, parameters, errors);
    } else if (ActionType.NOTIFICATION.equals(action.type())) {
      validateNotificationEmail(notificationsEmail, action, errors);
    } else if (ActionType.QUEUE.equals(action.type())) {
      validateQueue(siteId, action, errors);
    } else if (ActionType.IDP.equals(action.type())) {
      validateIdp(siteId, action, errors);
    } else if (ActionType.EVENTBRIDGE.equals(action.type())) {
      validateEventBridge(action, errors);
    } else if (ActionType.RESIZE.equals(action.type())) {
      validateResize(action, errors);
    }
  }
}
