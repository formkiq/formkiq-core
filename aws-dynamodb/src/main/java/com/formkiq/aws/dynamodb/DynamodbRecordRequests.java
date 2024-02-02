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

    Map<String, AttributeValue> attributes = record.getDataAttributes();

    AttributeValue pk = AttributeValue.fromS(record.pk(siteId));
    AttributeValue sk = AttributeValue.fromS(record.sk());

    final Map<String, AttributeValue> key = Map.of(PK, pk, SK, sk);

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
