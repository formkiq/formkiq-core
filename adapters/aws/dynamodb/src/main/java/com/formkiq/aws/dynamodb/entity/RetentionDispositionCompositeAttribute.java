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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeEntityKeyValue;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.StoredDerivedAttribute;
import com.formkiq.aws.dynamodb.objects.DateUtil;

import java.util.List;

import static com.formkiq.aws.dynamodb.DbKeys.COMPOSITE_KEY_DELIM;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_POLICY;

/**
 * DispositionDate derived attribute.
 */
public class RetentionDispositionCompositeAttribute extends DispositionDateAttribute
    implements StoredDerivedAttribute {

  @Override
  public String getAttributeKey() {
    return "";
  }

  @Override
  public DocumentAttributeRecord getDocumentAttributeRecord(final EntityRecord entityRecord,
      final DocumentRecord document) {

    var sourceType = getSourceType(entityRecord);
    var date = getDispositionField(entityRecord, document);
    var dateString = DateUtil.getYyyyMmDdFormatter().format(date);
    var sv =
        new DocumentAttributeEntityKeyValue(entityRecord.entityTypeId(), entityRecord.documentId())
            .getStringValue();

    String stringValue = String.join(COMPOSITE_KEY_DELIM, List.of(sv, dateString));
    String attributeKey =
        String.join(COMPOSITE_KEY_DELIM, List.of(RETENTION_POLICY.getKey(), sourceType));

    return new DocumentAttributeRecord()
        .setDocument(DocumentArtifact.of(document.documentId(), document.artifactId()))
        .setUserId("System").setKey(attributeKey).setStringValue(stringValue)
        .setValueType(DocumentAttributeValueType.COMPOSITE_STRING);
  }
}
