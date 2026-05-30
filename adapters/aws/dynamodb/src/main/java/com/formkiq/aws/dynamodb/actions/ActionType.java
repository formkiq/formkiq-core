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
package com.formkiq.aws.dynamodb.actions;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.FindEntityByName;
import com.formkiq.aws.dynamodb.entity.FindEntityTypeByName;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.validation.ValidationBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

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
        final String notificationsEmail, final ValidationBuilder vb) {

    }
  },
  /** Malware Scan. */
  MALWARE_SCAN {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {

    }
  },
  /** Document Tagging. */
  DOCUMENTTAGGING {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (!parameters.containsKey("tags")) {
        vb.addError("parameters.tags", "action 'tags' parameter is required");
      }

      if (!parameters.containsKey("engine")) {
        vb.addError("parameters.engine", "action 'engine' parameter is required");
      } else if (!"chatgpt".equals(parameters.getOrDefault("engine", ""))) {
        vb.addError("parameters.engine", "invalid 'engine' parameter");
      } else if (Strings.isEmpty(chatGptApiKey)) {
        vb.addError(null, "chatgpt 'api key' is not configured");
      }
    }
  },
  /** Full Text. */
  FULLTEXT {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {

    }
  },
  /** Move document to folder. */
  MOVE {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isMissingValue(parameters, "path")) {
        vb.addError("parameters.path", "action 'path' parameter is required");
      } else {
        String path = parameters.get("path").toString().trim();
        if (!path.endsWith("/")) {
          vb.addError("parameters.path", "action 'path' parameter must end with '/'");
        }
      }
    }
  },
  /** Delete document. */
  DELETE {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isMissingValue(parameters, "deleteType")) {
        vb.addError("parameters.deleteType", "action 'deleteType' parameter is required");
      } else {
        String deleteType = parameters.get("deleteType").toString().trim().toUpperCase(Locale.ROOT);
        if (!VALID_DELETE_TYPES.contains(deleteType)) {
          vb.addError("parameters.deleteType",
              "action 'deleteType' parameter must be one of " + VALID_DELETE_TYPES);
        }
      }
    }
  },
  /** Checksum. */
  CHECKSUM {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      String checksumType = parameters != null ? (String) parameters.get("checksumType") : null;

      if (isMissingValue(parameters, "checksumType")) {
        vb.addError("parameters.checksumType", "'checksumType' parameter is required");
      } else if (!VALID_CHECKSUM_TYPES.contains(checksumType.toUpperCase(Locale.ROOT))) {
        vb.addError("parameters.checksumType",
            "'checksumType' parameter must be one of " + VALID_CHECKSUM_TYPES);
      }
    }
  },
  /** Intelligent Document Processing. */
  IDP {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isMissingValue(parameters, "mappingId")) {
        vb.addError("mappingId", "'mappingId' is required");

      } else {
        String mappingId = (String) parameters.get("mappingId");
        MappingRecord m = new MappingRecord().setDocumentId(mappingId);

        if (!db.exists(m.fromS(m.pk(siteId)), m.fromS(m.sk()))) {
          vb.addError("mappingId", "'mappingId' does not exist");
        }
      }
    }
  },
  /** Notification Action. */
  NOTIFICATION {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {

      if (isEmpty(notificationsEmail)) {
        vb.addError("parameters.notificationEmail", "notificationEmail is not configured");
      } else {

        for (String parameter : Arrays.asList(ActionParameters.PARAMETER_NOTIFICATION_TYPE,
            ActionParameters.PARAMETER_NOTIFICATION_SUBJECT)) {
          if (isMissingValue(parameters, parameter)) {
            vb.addError("parameters." + parameter,
                "action '" + parameter + "' parameter is required");
          }
        }

        if (isMissingValue(parameters, ActionParameters.PARAMETER_NOTIFICATION_TO_CC)
            && isMissingValue(parameters, ActionParameters.PARAMETER_NOTIFICATION_TO_BCC)) {
          vb.addError("parameters." + ActionParameters.PARAMETER_NOTIFICATION_TO_CC,
              "action '" + ActionParameters.PARAMETER_NOTIFICATION_TO_CC + "' or '"
                  + ActionParameters.PARAMETER_NOTIFICATION_TO_BCC + "' is required");
        }

        if (isMissingValue(parameters, ActionParameters.PARAMETER_NOTIFICATION_TEXT)
            && isMissingValue(parameters, ActionParameters.PARAMETER_NOTIFICATION_HTML)) {
          vb.addError("parameters." + ActionParameters.PARAMETER_NOTIFICATION_TEXT,
              "action '" + ActionParameters.PARAMETER_NOTIFICATION_TEXT + "' or '"
                  + ActionParameters.PARAMETER_NOTIFICATION_HTML + "' is required");
        }
      }
    }
  },
  /** OCR. */
  OCR {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      String ocrParseTypes = (String) parameters.get("ocrParseTypes");
      if (ocrParseTypes != null) {
        ocrParseTypes = ocrParseTypes.toLowerCase();
        Collection<Map<String, Object>> ocrTextractQueries =
            notNull((Collection<Map<String, Object>>) parameters.getOrDefault("ocrTextractQueries",
                Collections.emptyList()));

        if (ocrParseTypes.contains("queries") && ocrTextractQueries.isEmpty()) {
          vb.addError("parameters.ocrTextractQueries",
              "action 'ocrTextractQueries' parameter is required");
        }

        ocrTextractQueries.forEach(q -> {
          String text = (String) q.get("text");
          if (isEmpty(text)) {
            vb.addError("parameters.ocrTextractQueries.text",
                "action 'ocrTextractQueries.text' parameter is required");
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
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isEmpty(action.queueId())) {
        vb.addError("queueId", "'queueId' is required");
      } else {
        Queue q = new Queue().documentId(action.queueId());
        if (!db.exists(q.fromS(q.pk(siteId)), q.fromS(q.sk()))) {
          vb.addError("queueId", "'queueId' does not exist");
        }
      }
    }
  },
  /** WebHook. */
  WEBHOOK {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (!parameters.containsKey("url")) {
        vb.addError("parameters.url", "action 'url' parameter is required");
      }
    }
  },
  /** Publish. */
  PUBLISH {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {

    }
  },
  /** Pdf Export. */
  PDFEXPORT {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {

    }
  },
  /** Event Bridge. */
  EVENTBRIDGE {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isMissingValue(parameters, "eventBusName")) {
        vb.addError("parameters.eventBusName", "'eventBusName' parameter is required");
      }
    }
  },
  /** Metadata Extraction. */
  METADATA_EXTRACTION {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isMissingValue(parameters, "llmPromptEntityName")) {
        vb.addError("parameters.llmPromptEntityName",
            "'llmPromptEntityName' parameter is required");
      }
    }
  },
  /** LLM Prompt. */
  LLMPROMPT {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isMissingValue(parameters, "llmPromptEntityName")) {
        vb.addError("parameters.llmPromptEntityName",
            "'llmPromptEntityName' parameter is required");
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
        final String notificationsEmail, final ValidationBuilder vb) {
      String widthParameterName = "width";
      String heightParameterName = "height";

      if ("auto".equals(parameters.get(widthParameterName))
          && "auto".equals(parameters.get(heightParameterName))) {
        vb.addError("parameters." + widthParameterName, "'" + widthParameterName + "' and '"
            + heightParameterName + "' parameters cannot be both set to auto");
      }

      validateDimension(parameters, vb, widthParameterName);
      validateDimension(parameters, vb, heightParameterName);
      validateImageFormat(parameters, vb);
    }

    private void validateDimension(final Map<String, Object> parameters, final ValidationBuilder vb,
        final String dimension) {
      if (isMissingValue(parameters, dimension)) {
        vb.addError("parameters." + dimension, "'" + dimension + "' parameter is required");
      } else {
        String value = (String) parameters.get(dimension);

        if (!isGreaterThanZeroInteger(value) && !"auto".equals(value)) {
          vb.addError("parameters." + dimension,
              "'" + dimension + "' parameter must be an integer > 0 or 'auto'");
        }
      }
    }

    private void validateImageFormat(final Map<String, Object> parameters,
        final ValidationBuilder vb) {
      String outputType = (String) parameters.get("outputType");

      if (outputType != null && !VALID_IMAGE_FORMATS.contains(outputType.toLowerCase())) {
        vb.addError("parameters.outputType",
            "'outputType' parameter must be one of " + VALID_IMAGE_FORMATS);
      }
    }
  },
  /** Data Classification. */
  DATA_CLASSIFICATION {
    @Override
    public void validate(final DynamoDbService db, final String siteId, final Action action,
        final Map<String, Object> parameters, final String chatGptApiKey,
        final String notificationsEmail, final ValidationBuilder vb) {
      if (isMissingValue(parameters, "llmPromptEntityName")) {
        vb.addError("parameters.llmPromptEntityName",
            "'llmPromptEntityName' parameter is required");
      } else {

        String entityTypeId = new FindEntityTypeByName().find(db, siteId,
            new FindEntityTypeByName.EntityTypeName(EntityTypeNamespace.PRESET, "LlmPrompt"));

        String name = (String) parameters.get("llmPromptEntityName");
        EntityRecord entityRecord = new FindEntityByName().find(db, siteId,
            new FindEntityByName.EntityName(entityTypeId, name));
        if (entityRecord == null) {
          vb.addError("parameters.llmPromptEntityName",
              "action 'llmPromptEntityName' doesn't exist");
        }
      }
    }
  };

  /** Valid image formats for resize action. */
  private static final List<String> VALID_IMAGE_FORMATS =
      List.of("bmp", "gif", "jpeg", "png", "tif");
  /** Valid checksum types for checksum action. */
  private static final List<String> VALID_CHECKSUM_TYPES = List.of("SHA1", "SHA256", "SHA512");
  /** Valid delete types for delete action. */
  private static final List<String> VALID_DELETE_TYPES =
      List.of("SOFT_DELETE", "HARD_DELETE", "PURGE");

  private static boolean isMissingValue(final Map<String, Object> parameters, final String key) {
    return parameters == null || !parameters.containsKey(key)
        || isEmpty(parameters.get(key).toString().trim());
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
   * @param vb {@link ValidationBuilder}
   */
  public abstract void validate(DynamoDbService db, String siteId, Action action,
      Map<String, Object> parameters, String chatGptApiKey, String notificationsEmail,
      ValidationBuilder vb);
}
