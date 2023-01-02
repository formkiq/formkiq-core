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
package com.formkiq.stacks.dynamodb;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.model.DocumentSync;
import com.formkiq.aws.dynamodb.model.DocumentSyncMap;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link DocumentTag}.
 *
 */
public class AttributeValueToDocumentSync
    implements Function<Map<String, AttributeValue>, DocumentSync> {

  /** {@link AttributeValueToDate}. */
  private AttributeValueToDate toDate = new AttributeValueToDate("syncDate");

  @Override
  public DocumentSync apply(final Map<String, AttributeValue> map) {

    DocumentSync sync = new DocumentSyncMap(new HashMap<>());

    sync.setDocumentId(map.get("documentId").s());
    sync.setUserId(map.get("userId").s());
    sync.setService(DocumentSyncServiceType.valueOf(map.get("service").s().toUpperCase()));
    sync.setStatus(DocumentSyncStatus.valueOf(map.get("status").s().toUpperCase()));
    sync.setType(DocumentSyncType.valueOf(map.get("type").s().toUpperCase()));
    sync.setMessage(map.get("message").s());
    sync.setSyncDate(this.toDate.apply(map));

    return sync;
  }
}
