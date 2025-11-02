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
package com.formkiq.stacks.dynamodb.attributes;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentRelationshipType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Build Relationship {@link DocumentAttributeRecord}.
 */
public class DocumentAttributeRecordBuilder {

  public Collection<DocumentAttributeRecord> apply(final String fromDocumentId,
      final String toDocumentId, final DocumentRelationshipType toRelationship,
      final DocumentRelationshipType inverseRelationship) {

    Collection<DocumentAttributeRecord> records = new ArrayList<>();

    String eq = toRelationship + "#" + toDocumentId;
    DocumentAttributeRecord dar0 = new DocumentAttributeRecord();
    dar0.setDocumentId(fromDocumentId);
    dar0.setStringValue(eq);
    records.add(dar0);

    if (inverseRelationship != null && fromDocumentId != null) {
      String eqInverse = inverseRelationship + "#" + fromDocumentId;
      DocumentAttributeRecord dar1 = new DocumentAttributeRecord();
      dar1.setDocumentId(toDocumentId);
      dar1.setStringValue(eqInverse);
      records.add(dar1);
    }

    Date now = new Date();
    String username = ApiAuthorization.getAuthorization().getUsername();
    records.forEach(r -> {
      r.setKey(AttributeKeyReserved.RELATIONSHIPS.getKey());
      r.setUserId(username);
      r.setInsertedDate(now);
      r.updateValueType();
    });

    return records;
  }
}
