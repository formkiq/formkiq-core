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
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeEntityKeyValue;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.aws.dynamodb.entity.FindEntityTypeByName;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordBuilder;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Convert {@link AddDocumentAttribute} to {@link DocumentAttributeRecord}.
 */
public class AddDocumentAttributeToDocumentAttributeRecord
    implements Function<AddDocumentAttribute, Collection<DocumentAttributeRecord>> {

  /** Document Id. */
  private final String docId;
  /** Site Id. */
  private final String site;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** Documents Table Name. */
  private final String tableName;

  /**
   * constructor.
   *
   * @param serviceCache {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  public AddDocumentAttributeToDocumentAttributeRecord(final AwsServiceCache serviceCache,
      final String siteId, final String documentId) {
    this.docId = documentId;
    this.site = siteId;
    this.db = serviceCache.getExtension(DynamoDbService.class);
    this.tableName = serviceCache.environment("DOCUMENTS_TABLE");
  }

  @Override
  public Collection<DocumentAttributeRecord> apply(final AddDocumentAttribute a) {
    return buildAttributeRecords(a);
  }

  /**
   * Build {@link Collection} {@link DocumentAttributeRecord} from {@link AddDocumentAttribute}.
   *
   * @param a {@link AddDocumentAttribute}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  private Collection<DocumentAttributeRecord> buildAttributeRecords(final AddDocumentAttribute a)
      throws ValidationException {
    Collection<DocumentAttributeRecord> c = new ArrayList<>();

    if (a != null) {
      boolean used = false;

      if (a instanceof AddDocumentAttributeEntity e) {
        addEntity(e, c);
      } else if (a instanceof AddDocumentAttributeRelationship e) {
        addRelationship(e, c);
      } else if (a instanceof AddDocumentAttributeClassification e) {
        addClassification(e, c);
      } else if (a instanceof AddDocumentAttributeStandard e) {

        if (AttributeKeyReserved.CLASSIFICATION.getKey().equals(e.key())) {
          if (!isEmpty(e.stringValue())) {
            addToList(c, DocumentAttributeValueType.CLASSIFICATION, e.key(), e.stringValue(), null,
                null);
          }

          notNull(e.stringValues()).forEach(
              v -> addToList(c, DocumentAttributeValueType.CLASSIFICATION, e.key(), v, null, null));
        } else {
          addDocumentAttributeStandard(e, c, used);
        }
      }
    }

    return c;
  }

  private void addDocumentAttributeStandard(final AddDocumentAttributeStandard a,
      final Collection<DocumentAttributeRecord> c, final boolean isUsed) {
    boolean used = isUsed;
    String key = a.key();
    if (!isEmpty(a.stringValue())) {
      used = true;
      addToList(c, DocumentAttributeValueType.STRING, key, a.stringValue(), null, null);
    }

    if (a.numberValue() != null) {
      used = true;
      addToList(c, DocumentAttributeValueType.NUMBER, key, null, null, a.numberValue());
    }

    if (a.booleanValue() != null) {
      used = true;
      addToList(c, DocumentAttributeValueType.BOOLEAN, key, null, a.booleanValue(), null);
    }

    for (String stringValue : notNull(a.stringValues())) {
      used = true;
      addToList(c, DocumentAttributeValueType.STRING, key, stringValue, null, null);
    }

    for (Double numberValue : notNull(a.numberValues())) {
      used = true;
      addToList(c, DocumentAttributeValueType.NUMBER, key, null, null, numberValue);
    }

    if (!used) {
      addToList(c, DocumentAttributeValueType.KEY_ONLY, key, null, null, null);
    }

  }

  private void addClassification(final AddDocumentAttributeClassification a,
      final Collection<DocumentAttributeRecord> c) {
    String k = AttributeKeyReserved.CLASSIFICATION.getKey();

    if (!isEmpty(a.classificationId())) {
      addToList(c, DocumentAttributeValueType.CLASSIFICATION, k, a.classificationId(), null, null);
    }
  }

  private void addEntity(final AddDocumentAttributeEntity a,
      final Collection<DocumentAttributeRecord> c) throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("entityId", a.entityId());
    vb.isRequired("entityTypeId", a.entityTypeId());
    vb.check();

    EntityTypeNamespace namespace = a.namespace();
    String entityTypeId = new FindEntityTypeByName().find(db, tableName, site,
        new FindEntityTypeByName.EntityTypeName(namespace, a.entityTypeId()));

    DocumentAttributeEntityKeyValue val =
        new DocumentAttributeEntityKeyValue(entityTypeId, a.entityId());

    addToList(c, DocumentAttributeValueType.ENTITY, a.key(), val.getStringValue(), null, null);
  }

  private void addRelationship(final AddDocumentAttributeRelationship a,
      final Collection<DocumentAttributeRecord> c) {

    Collection<DocumentAttributeRecord> records = new DocumentAttributeRecordBuilder()
        .apply(this.docId, a.documentId(), a.relationship(), a.inverseRelationship());
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

    String username = ApiAuthorization.getAuthorization().getUsername();
    DocumentAttributeRecord a = new DocumentAttributeRecord();
    a.setKey(key);
    a.setDocumentId(documentId);
    a.setStringValue(stringValue);
    a.setBooleanValue(boolValue);
    a.setNumberValue(numberValue);
    a.setValueType(valueType);
    a.setUserId(username);

    list.add(a);
  }
}
