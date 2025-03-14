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
package com.formkiq.stacks.api.transformers;

import com.formkiq.stacks.api.handler.DocumentAttribute;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordBuilder;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Convert {@link DocumentAttribute} to {@link DocumentAttributeRecord}.
 */
public class DocumentAttributeToDocumentAttributeRecord
    implements Function<DocumentAttribute, Collection<DocumentAttributeRecord>> {

  /** User Identifier. */
  private final String user;
  /** Document Id. */
  private final String docId;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   * @param userId {@link String}
   */
  public DocumentAttributeToDocumentAttributeRecord(final String documentId, final String userId) {
    this.docId = documentId;
    this.user = userId;
  }

  @Override
  public Collection<DocumentAttributeRecord> apply(final DocumentAttribute a) {
    return buildAttributeRecords(a);
  }

  /**
   * Build {@link Collection} {@link DocumentAttributeRecord} from {@link DocumentAttribute}.
   *
   * @param a {@link DocumentAttribute}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  private Collection<DocumentAttributeRecord> buildAttributeRecords(final DocumentAttribute a) {
    Collection<DocumentAttributeRecord> c = new ArrayList<>();

    if (a != null) {
      boolean used = false;
      String key = a.getKey();

      if (isRelationship(a)) {
        addRelationship(a, c);
      } else if (isClassification(a)) {
        addClassification(a, c);
      } else {

        if (!isEmpty(a.getStringValue())) {
          used = true;
          addToList(c, DocumentAttributeValueType.STRING, key, a.getStringValue(), null, null);
        }

        if (a.getNumberValue() != null) {
          used = true;
          addToList(c, DocumentAttributeValueType.NUMBER, key, null, null, a.getNumberValue());
        }

        if (a.getBooleanValue() != null) {
          used = true;
          addToList(c, DocumentAttributeValueType.BOOLEAN, key, null, a.getBooleanValue(), null);
        }

        for (String stringValue : notNull(a.getStringValues())) {
          used = true;
          addToList(c, DocumentAttributeValueType.STRING, key, stringValue, null, null);
        }

        for (Double numberValue : notNull(a.getNumberValues())) {
          used = true;
          addToList(c, DocumentAttributeValueType.NUMBER, key, null, null, numberValue);
        }

        if (!used) {
          addToList(c, DocumentAttributeValueType.KEY_ONLY, key, null, null, null);
        }
      }
    }

    return c;
  }

  private void addClassification(final DocumentAttribute a,
      final Collection<DocumentAttributeRecord> c) {
    String k = AttributeKeyReserved.CLASSIFICATION.getKey();

    if (!isEmpty(a.getClassificationId())) {
      addToList(c, DocumentAttributeValueType.CLASSIFICATION, k, a.getClassificationId(), null,
          null);
    }

    if (!isEmpty(a.getStringValue())) {
      addToList(c, DocumentAttributeValueType.CLASSIFICATION, k, a.getStringValue(), null, null);
    }

    notNull(a.getStringValues())
        .forEach(v -> addToList(c, DocumentAttributeValueType.CLASSIFICATION, k, v, null, null));
  }

  private static boolean isRelationship(final DocumentAttribute a) {
    return !isEmpty(a.getDocumentId()) && a.getRelationship() != null;
  }

  private void addRelationship(final DocumentAttribute a,
      final Collection<DocumentAttributeRecord> c) {

    Collection<DocumentAttributeRecord> records = new DocumentAttributeRecordBuilder().apply(
        this.docId, a.getDocumentId(), a.getRelationship(), a.getInverseRelationship(), this.user);
    c.addAll(records);
  }

  private void addToList(final Collection<DocumentAttributeRecord> list,
      final DocumentAttributeValueType valueType, final String key, final String stringValue,
      final Boolean boolValue, final Double numberValue) {
    addToList(list, this.docId, valueType, key, stringValue, boolValue, numberValue);
  }

  private void addToList(final Collection<DocumentAttributeRecord> list, final String documentId,
      final DocumentAttributeValueType valueType, final String key, final String stringValue,
      final Boolean boolValue, final Double numberValue) {

    DocumentAttributeRecord a = new DocumentAttributeRecord();
    a.setKey(key);
    a.setDocumentId(documentId);
    a.setStringValue(stringValue);
    a.setBooleanValue(boolValue);
    a.setNumberValue(numberValue);
    a.setValueType(valueType);
    a.setUserId(this.user);

    list.add(a);
  }

  private boolean isClassification(final DocumentAttribute a) {
    String k = AttributeKeyReserved.CLASSIFICATION.getKey();
    return k.equalsIgnoreCase(a.getKey()) || !isEmpty(a.getClassificationId());
  }
}
