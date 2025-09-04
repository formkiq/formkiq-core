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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.aws.dynamodb.entity.EntityTypeNameToIdQuery;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordBuilder;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;

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
   * @param userId {@link String}
   */
  public DocumentAttributeToDocumentAttributeRecord(final AwsServiceCache serviceCache,
      final String siteId, final String documentId, final String userId) {
    this.docId = documentId;
    this.user = userId;
    this.site = siteId;
    this.db = serviceCache.getExtension(DynamoDbService.class);
    this.tableName = serviceCache.environment("DOCUMENTS_TABLE");
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
  private Collection<DocumentAttributeRecord> buildAttributeRecords(final DocumentAttribute a)
      throws ValidationException {
    Collection<DocumentAttributeRecord> c = new ArrayList<>();

    if (a != null) {
      boolean used = false;
      String key = a.getKey();

      if (isEntity(a)) {
        addEntity(a, c);
      } else if (isRelationship(a)) {
        addRelationship(a, c);
      } else if (isClassification(a)) {
        addClassification(a, c);
      } else {
        addDefault(a, c, used, key);
      }
    }

    return c;
  }

  private void addDefault(final DocumentAttribute a, final Collection<DocumentAttributeRecord> c,
      final boolean isUsed, final String key) {
    boolean used = isUsed;
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

  private static boolean isEntity(final DocumentAttribute a) {
    return !isEmpty(a.getEntityTypeId()) || !isEmpty(a.getEntityId());
  }

  private void addEntity(final DocumentAttribute a, final Collection<DocumentAttributeRecord> c)
      throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("entityId", a.getEntityId());
    vb.isRequired("entityTypeId", a.getEntityTypeId());
    vb.check();

    EntityTypeNamespace namespace = EntityTypeNamespace.fromString(a.getNamespace());
    String entityTypeId = new EntityTypeNameToIdQuery().find(db, tableName, site, EntityTypeRecord
        .builder().namespace(namespace).documentId(a.getEntityTypeId()).nameEmpty().build(site));

    String stringValue = entityTypeId + "#" + a.getEntityId();

    addToList(c, DocumentAttributeValueType.ENTITY, a.getKey(), stringValue, null, null);
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
