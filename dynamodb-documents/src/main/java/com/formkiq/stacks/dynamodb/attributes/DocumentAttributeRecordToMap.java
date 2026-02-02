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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamodbRecordToMap;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeEntityKeyValue;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.entity.AttributeValueToEntityTransformer;
import com.formkiq.aws.dynamodb.entity.EntityAttributeTransformer;
import com.formkiq.aws.dynamodb.entity.EntityDocumentAttributeEntityKeyValueGet;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * {@link Function} to merge {@link DocumentAttributeRecord} with the same keys together.
 */
public class DocumentAttributeRecordToMap implements
    BiFunction<String, Collection<DocumentAttributeRecord>, Collection<Map<String, Object>>> {

  /** Whether Keys should be unique. */
  private final boolean uniqueKeys;

  /** Whether to load entities. */
  private final boolean loadEntities;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** Table Name. */
  private final String tabeName;

  /**
   * constructor.
   * 
   * @param uniqueAttributeKeys boolean
   */
  public DocumentAttributeRecordToMap(final boolean uniqueAttributeKeys) {
    this(uniqueAttributeKeys, false, null, null);
  }

  /**
   * constructor.
   *
   * @param uniqueAttributeKeys boolean
   * @param loadEntityValues boolean
   * @param dbService {@link DynamoDbService}
   * @param dynamoDbTableName {@link String}
   */
  public DocumentAttributeRecordToMap(final boolean uniqueAttributeKeys,
      final boolean loadEntityValues, final DynamoDbService dbService,
      final String dynamoDbTableName) {
    this.uniqueKeys = uniqueAttributeKeys;
    this.loadEntities = loadEntityValues;
    this.db = dbService;
    this.tabeName = dynamoDbTableName;
  }

  private void addEntityValue(final DocumentAttributeRecord a,
      final Map<String, Map<String, AttributeValue>> entityMap,
      final Map<String, Object> lastValues) {

    if (DocumentAttributeValueType.ENTITY.equals(a.getValueType())
        && !isEmpty(a.getStringValue())) {
      String key = a.getStringValue();
      if (entityMap.containsKey(key)) {

        Map<String, Object> values =
            new AttributeValueToEntityTransformer().apply(entityMap.get(key));
        new EntityAttributeTransformer().apply(values);

        lastValues.put("entity", values);
      }
    }
  }

  private void addNumberValues(final Map<String, Object> lastValues,
      final DocumentAttributeRecord a) {
    if (lastValues.containsKey("numberValue")) {

      List<Double> s = new ArrayList<>();
      s.add((Double) lastValues.get("numberValue"));
      s.add(a.getNumberValue());

      lastValues.remove("numberValue");
      lastValues.put("numberValues", s);

    } else if (lastValues.containsKey("numberValues")) {
      ((List<Double>) lastValues.get("numberValues")).add(a.getNumberValue());
    }
  }

  private void addStringValues(final Map<String, Object> lastValues,
      final DocumentAttributeRecord a) {

    if (lastValues.containsKey("stringValue")) {

      List<String> s = new ArrayList<>();
      s.add((String) lastValues.get("stringValue"));
      s.add(a.getStringValue());

      lastValues.remove("stringValue");
      lastValues.put("stringValues", s);

    } else if (lastValues.containsKey("stringValues")) {
      ((List<String>) lastValues.get("stringValues")).add(a.getStringValue());
    }
  }

  @Override
  public Collection<Map<String, Object>> apply(final String siteId,
      final Collection<DocumentAttributeRecord> attributes) {

    DocumentAttributeRecord last = null;
    Map<String, Object> lastValues = null;
    Collection<Map<String, Object>> c = new ArrayList<>();

    var entityMap = loadEntities(siteId, attributes);

    for (DocumentAttributeRecord a : attributes) {

      if (uniqueKeys && lastValues != null && last.getKey().equals(a.getKey())) {

        if (!isEmpty(a.getStringValue())) {

          addStringValues(lastValues, a);

        } else if (a.getNumberValue() != null) {

          addNumberValues(lastValues, a);
        }

      } else {

        lastValues = new DynamodbRecordToMap().apply(a);
        lastValues.remove("documentId");

        if (lastValues.containsKey("inserteddate")) {
          lastValues.put("insertedDate", lastValues.get("inserteddate"));
          lastValues.remove("inserteddate");
        }

        addEntityValue(a, entityMap, lastValues);

        c.add(lastValues);

        last = a;
      }
    }

    return c;
  }

  private Collection<DocumentAttributeEntityKeyValue> findEntityKeyValues(
      final Collection<DocumentAttributeRecord> attributes) {
    return attributes.stream()
        .filter(a -> DocumentAttributeValueType.ENTITY.equals(a.getValueType()))
        .map(a -> DocumentAttributeEntityKeyValue.fromString(a.getStringValue()))
        .collect(Collectors.toSet());
  }

  private Map<String, Map<String, AttributeValue>> loadEntities(final String siteId,
      final Collection<DocumentAttributeRecord> attributes) {
    if (loadEntities) {
      var entityKeyValues = findEntityKeyValues(attributes);
      Collection<Map<String, AttributeValue>> attrs =
          new EntityDocumentAttributeEntityKeyValueGet(entityKeyValues).find(db, tabeName, siteId);

      return attrs.stream().collect(Collectors.toMap(
          r -> new DocumentAttributeEntityKeyValue(DynamoDbTypes.toString(r.get("entityTypeId")),
              DynamoDbTypes.toString(r.get("documentId"))).getStringValue(),
          Function.identity()));
    }

    return Collections.emptyMap();
  }

}
