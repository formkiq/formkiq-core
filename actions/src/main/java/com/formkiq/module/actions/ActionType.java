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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.FindEntityByName;
import com.formkiq.aws.dynamodb.entity.FindEntityTypeByName;
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_HTML;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_SUBJECT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TEXT;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_BCC;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TO_CC;
import static com.formkiq.module.actions.ActionParameters.PARAMETER_NOTIFICATION_TYPE;

/**
 * 
 * Supported Type of Actions.
 *
 */
@Reflectable
public enum ActionType {
  /** AntiVirus. */
  ANTIVIRUS {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {

    }
  },
  /** Malware Scan. */
  MALWARE_SCAN {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {

    }
  },
  /** Document Tagging. */
  DOCUMENTTAGGING {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
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
  },
  /** Full Text. */
  FULLTEXT {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {

    }
  },
  /** Intelligent Document Processing. */
  IDP {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
      if (!hasValue(parameters, "mappingId")) {
        errors.add(new ValidationErrorImpl().key("mappingId").error("'mappingId' is required"));

      } else {
        String mappingId = (String) parameters.get("mappingId");
        MappingRecord m = new MappingRecord().setDocumentId(mappingId);

        if (!db.exists(m.fromS(m.pk(siteId)), m.fromS(m.sk()))) {
          errors
              .add(new ValidationErrorImpl().key("mappingId").error("'mappingId' does not exist"));
        }
      }
    }
  },
  /** Notification Action. */
  NOTIFICATION {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {

      if (isEmpty(notificationsEmail)) {
        errors.add(new ValidationErrorImpl().key("parameters.notificationEmail")
            .error("notificationEmail is not configured"));
      } else {

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
              .error("action '" + PARAMETER_NOTIFICATION_TEXT + "' or '"
                  + PARAMETER_NOTIFICATION_HTML + "' is required"));
        }
      }
    }
  },
  /** OCR. */
  OCR {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
      String ocrParseTypes = (String) parameters.get("ocrParseTypes");
      if (ocrParseTypes != null) {
        ocrParseTypes = ocrParseTypes.toLowerCase();
        Collection<Map<String, Object>> ocrTextractQueries =
            notNull((Collection<Map<String, Object>>) parameters.getOrDefault("ocrTextractQueries",
                Collections.emptyList()));

        if (ocrParseTypes.contains("queries") && ocrTextractQueries.isEmpty()) {
          errors.add(new ValidationErrorImpl().key("parameters.ocrTextractQueries")
              .error("action 'ocrTextractQueries' parameter is required"));
        }

        ocrTextractQueries.forEach(q -> {
          String text = (String) q.get("text");
          if (isEmpty(text)) {
            errors.add(new ValidationErrorImpl().key("parameters.ocrTextractQueries.text")
                .error("action 'ocrTextractQueries.text' parameter is required"));
          }
        });
      }
    }
  },
  /** Queue. */
  QUEUE {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
      if (isEmpty(action.queueId())) {
        errors.add(new ValidationErrorImpl().key("queueId").error("'queueId' is required"));
      } else {
        Queue q = new Queue().documentId(action.queueId());
        if (!db.exists(q.fromS(q.pk(siteId)), q.fromS(q.sk()))) {
          errors.add(new ValidationErrorImpl().key("queueId").error("'queueId' does not exist"));
        }
      }
    }
  },
  /** WebHook. */
  WEBHOOK {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
      if (!parameters.containsKey("url")) {
        errors.add(new ValidationErrorImpl().key("parameters.url")
            .error("action 'url' parameter is required"));
      }
    }
  },
  /** Publish. */
  PUBLISH {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {

    }
  },
  /** Pdf Export. */
  PDFEXPORT {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {

    }
  },
  /** Event Bridge. */
  EVENTBRIDGE {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
      if (!hasValue(parameters, "eventBusName")) {
        errors.add(new ValidationErrorImpl().key("parameters.eventBusName")
            .error("'eventBusName' parameter is required"));
      }
    }
  },
  /** Metadata Extraction. */
  METADATA_EXTRACTION {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
      if (!hasValue(parameters, "llmPromptEntityName")) {
        errors.add(new ValidationErrorImpl().key("parameters.llmPromptEntityName")
            .error("'llmPromptEntityName' parameter is required"));
      }
    }
  },
  /** Resize. */
  RESIZE {
    private boolean isGreaterThanZeroInteger(final String value) {
      try {
        return Integer.parseInt(value) > 0;
      } catch (NumberFormatException e) {
        return false;
      }
    }

    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
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

    private void validateDimension(final Map<String, Object> parameters,
        final Collection<ValidationError> errors, final String dimension) {
      if (!hasValue(parameters, dimension)) {
        errors.add(new ValidationErrorImpl().key("parameters." + dimension)
            .error("'" + dimension + "' parameter is required"));
      } else {
        String value = (String) parameters.get(dimension);

        if (!isGreaterThanZeroInteger(value) && !"auto".equals(value)) {
          errors.add(new ValidationErrorImpl().key("parameters." + dimension)
              .error("'" + dimension + "' parameter must be an integer > 0 or 'auto'"));
        }
      }
    }

    private void validateImageFormat(final Map<String, Object> parameters,
        final Collection<ValidationError> errors) {
      String outputType = (String) parameters.get("outputType");

      if (outputType != null && !VALID_IMAGE_FORMATS.contains(outputType.toLowerCase())) {
        errors.add(new ValidationErrorImpl().key("parameters.outputType")
            .error("'outputType' parameter must be one of " + VALID_IMAGE_FORMATS));
      }
    }
  },
  /** Data Classification. */
  DATA_CLASSIFICATION {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final Collection<ValidationError> errors) {
      if (!hasValue(parameters, "llmPromptEntityName")) {
        errors.add(new ValidationErrorImpl().key("parameters.llmPromptEntityName")
            .error("'llmPromptEntityName' parameter is required"));
      } else {

        String tableName = db.getTableName();
        String entityTypeId = new FindEntityTypeByName().find(db, tableName, siteId,
            new FindEntityTypeByName.EntityTypeName(EntityTypeNamespace.PRESET,
                PresetEntity.LLM_PROMPT.getName()));

        String name = (String) parameters.get("llmPromptEntityName");
        EntityRecord entityRecord = new FindEntityByName().find(db, tableName, siteId,
            new FindEntityByName.EntityName(entityTypeId, name));
        if (entityRecord == null) {
          errors.add(new ValidationErrorImpl().key("parameters.llmPromptEntityName")
              .error("action 'llmPromptEntityName' doesn't exist"));
        }
      }
    }
  };

  /** Valid image formats for resize action. */
  private static final List<String> VALID_IMAGE_FORMATS =
      List.of("bmp", "gif", "jpeg", "png", "tif");

  private static boolean hasValue(final Map<String, Object> parameters, final String key) {
    return parameters != null && parameters.containsKey(key)
        && !isEmpty(parameters.get(key).toString().trim());
  }

  /**
   * Action Type Parameter Validation.
   * 
   * @param db {@link DynamoDbService}
   * @param siteId {@link String}
   * @param action {@link Action}
   * @param parameters {@link Map}
   * @param chatGptApiKey {@link String}
   * @param notificationsEmail {@link String}
   * @param errors {@link List} {@link com.formkiq.validation.ValidationBuilder}
   */
  public abstract void validate(DynamoDbService db, String siteId, Action action,
      Map<String, Object> parameters, String chatGptApiKey, String notificationsEmail,
      Collection<ValidationError> errors);
}
