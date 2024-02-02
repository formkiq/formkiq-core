package com.formkiq.aws.dynamodb;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Common requests for {@link DynamodbRecord}.
 */
public class DynamodbRecordRequests implements DbKeys {

  /**
   * Get {@link DynamodbRecord} {@link UpdateItemRequest}.
   * 
   * @param siteId {@link String}
   * @param tableName {@link String}
   * @param record {@link DynamodbRecord}
   * @return {@link UpdateItemRequest}
   */
  public UpdateItemRequest getUpdateRequest(final String siteId, final String tableName,
      final DynamodbRecord<?> record) {

    Map<String, AttributeValue> attributes = record.getAttributes(siteId);

    final Map<String, AttributeValue> key = Map.of(PK, attributes.get(PK), SK, attributes.get(SK));

    attributes.remove(PK);
    attributes.remove(SK);
    attributes.remove(GSI1_PK);
    attributes.remove(GSI1_SK);
    attributes.remove(GSI2_PK);
    attributes.remove(GSI2_SK);
    attributes.remove("inserteddate");
    attributes.remove("documentId");

    Map<String, AttributeValueUpdate> values = new HashMap<>();

    for (Map.Entry<String, AttributeValue> e : attributes.entrySet()) {
      values.put(e.getKey(), AttributeValueUpdate.builder().value(e.getValue()).build());
    }

    return !values.isEmpty()
        ? UpdateItemRequest.builder().key(key).attributeUpdates(values).tableName(tableName).build()
        : null;
  }
}
