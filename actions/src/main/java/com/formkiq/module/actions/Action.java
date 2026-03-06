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

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Record representing an Action, with its DynamoDB key structure and metadata.
 */
@Reflectable
public record Action(DynamoDbKey key, String documentId, String index, ActionType type,
    ActionStatus status, String userId, String message, String queueId, String workflowId,
    String workflowLastStep, String workflowStepId, Map<String, String> metadata,
    Map<String, Object> parameters, Integer retryCount, Integer maxRetries, Date insertedDate,
    Date startDate, Date completedDate) {

  /** DynamoDB attribute name for document id. */
  private static final String ATTR_DOCUMENT_ID = "documentId";
  /** DynamoDB attribute name for type. */
  private static final String ATTR_TYPE = "type";
  /** DynamoDB attribute name for status. */
  private static final String ATTR_STATUS = "status";
  /** DynamoDB attribute name for user id. */
  private static final String ATTR_USER_ID = "userId";
  /** DynamoDB attribute name for message. */
  private static final String ATTR_MESSAGE = "message";
  /** DynamoDB attribute name for queue id. */
  private static final String ATTR_QUEUE_ID = "queueId";
  /** DynamoDB attribute name for workflow id. */
  private static final String ATTR_WORKFLOW_ID = "workflowId";
  /** DynamoDB attribute name for workflow last step. */
  private static final String ATTR_WORKFLOW_LAST_STEP = "workflowLastStep";
  /** DynamoDB attribute name for workflow step id. */
  private static final String ATTR_WORKFLOW_STEP_ID = "workflowStepId";
  /** DynamoDB attribute name for metadata. */
  private static final String ATTR_METADATA = "metadata";
  /** DynamoDB attribute name for parameters. */
  private static final String ATTR_PARAMETERS = "parameters";
  /** DynamoDB attribute name for retry count. */
  private static final String ATTR_RETRY_COUNT = "retryCount";
  /** DynamoDB attribute name for max retries. */
  private static final String ATTR_MAX_RETRIES = "maxRetries";
  /** DynamoDB attribute name for inserted date. */
  private static final String ATTR_INSERTED_DATE = "inserteddate";
  /** DynamoDB attribute name for completed date. */
  private static final String ATTR_COMPLETED_DATE = "completedDate";
  /** DynamoDB attribute name for start date. */
  private static final String ATTR_START_DATE = "startDate";

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of dates.
   */
  public Action {
    if (insertedDate != null) {
      insertedDate = new Date(insertedDate.getTime());
    }
    if (startDate != null) {
      startDate = new Date(startDate.getTime());
    }
    if (completedDate != null) {
      completedDate = new Date(completedDate.getTime());
    }
  }

  /**
   * Constructs an {@code ActionRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code ActionRecord} instance, or {@code null} if {@code attributes} is null or
   *         empty
   */
  public static Action fromAttributeMap(final Map<String, AttributeValue> attributes) {

    if (attributes != null && !attributes.isEmpty()) {
      DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);

      String documentId = DynamoDbTypes.toString(attributes.get(ATTR_DOCUMENT_ID));
      String userId = DynamoDbTypes.toString(attributes.get(ATTR_USER_ID));
      String message = DynamoDbTypes.toString(attributes.get(ATTR_MESSAGE));
      String queueId = DynamoDbTypes.toString(attributes.get(ATTR_QUEUE_ID));
      String workflowId = DynamoDbTypes.toString(attributes.get(ATTR_WORKFLOW_ID));
      String workflowLastStep = DynamoDbTypes.toString(attributes.get(ATTR_WORKFLOW_LAST_STEP));
      String workflowStepId = DynamoDbTypes.toString(attributes.get(ATTR_WORKFLOW_STEP_ID));

      ActionStatus status = attributes.containsKey(ATTR_STATUS)
          ? ActionStatus.valueOf(DynamoDbTypes.toString(attributes.get(ATTR_STATUS)))
          : ActionStatus.PENDING;

      ActionType type = attributes.containsKey(ATTR_TYPE)
          ? ActionType.valueOf(DynamoDbTypes.toString(attributes.get(ATTR_TYPE)))
          : null;

      Integer retryCount = DynamoDbTypes.toInteger(attributes.get(ATTR_RETRY_COUNT));
      Integer maxRetries = DynamoDbTypes.toInteger(attributes.get(ATTR_MAX_RETRIES));

      Date insertedDate = DynamoDbTypes.toDate(attributes.get(ATTR_INSERTED_DATE));
      Date completedDate = DynamoDbTypes.toDate(attributes.get(ATTR_COMPLETED_DATE));
      Date startDate = DynamoDbTypes.toDate(attributes.get(ATTR_START_DATE));

      Map<String, Object> parameters = null;
      if (attributes.containsKey(ATTR_PARAMETERS) && attributes.get(ATTR_PARAMETERS).hasM()) {
        parameters = new AttributeValueToMap().apply(attributes.get(ATTR_PARAMETERS).m());
      }

      Map<String, String> metadata = null;
      if (attributes.containsKey(ATTR_METADATA) && attributes.get(ATTR_METADATA).hasM()) {
        metadata = attributes.get(ATTR_METADATA).m().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s()));
      }

      String index = calculateIndex(attributes);

      return new Action(key, documentId, index, type, status, userId, message, queueId, workflowId,
          workflowLastStep, workflowStepId, metadata, parameters, retryCount, maxRetries,
          insertedDate, startDate, completedDate);
    }

    return null;
  }

  /**
   * index is encoded in SK as: action|index|type.
   * 
   * @param attributes {@link Map}
   * @return {@link String}
   */
  private static String calculateIndex(final Map<String, AttributeValue> attributes) {
    String index = null;
    if (attributes.containsKey(DbKeys.SK)) {
      String[] parts =
          DynamoDbTypes.toString(attributes.get(DbKeys.SK)).split(DbKeys.TAG_DELIMINATOR);
      if (parts.length > 1) {
        index = parts[1];
      }
    }
    return index;
  }

  /**
   * Builds the DynamoDB item attribute map for this action, starting from the key attributes and
   * adding metadata fields.
   *
   * @return a map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    var b = key.getAttributesBuilder().withString(ATTR_DOCUMENT_ID, documentId)
        .withString(ATTR_USER_ID, userId).withString(ATTR_TYPE, type != null ? type.name() : null)
        .withString(ATTR_STATUS, status.name()).withString(ATTR_MESSAGE, message)
        .withString(ATTR_QUEUE_ID, queueId).withString(ATTR_WORKFLOW_ID, workflowId)
        .withString(ATTR_WORKFLOW_LAST_STEP, workflowLastStep)
        .withString(ATTR_WORKFLOW_STEP_ID, workflowStepId).withInteger(ATTR_RETRY_COUNT, retryCount)
        .withInteger(ATTR_MAX_RETRIES, maxRetries);

    if (metadata != null) {
      b = b.withMapObject(ATTR_METADATA, new HashMap<>(metadata));
    }

    if (parameters != null) {
      b = b.withMapObject(ATTR_PARAMETERS, parameters);
    }

    if (insertedDate != null) {
      b = b.withDate(ATTR_INSERTED_DATE, insertedDate);
    }

    if (startDate != null) {
      b = b.withDate(ATTR_START_DATE, startDate);
    }

    if (completedDate != null) {
      b = b.withDate(ATTR_COMPLETED_DATE, completedDate);
    }

    return b.build();
  }
}
