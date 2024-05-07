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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesCompositeKey;

/**
 * Convert {@link DynamicObject} to {@link DocumentAttributeRecord}.
 */
public class DynamicObjectToDocumentAttributeRecord
    implements Function<Collection<DynamicObject>, Collection<DocumentAttributeRecord>> {

  /** Document Id. */
  private String attributeDocumentId;
  /** {@link Schema}. */
  private Schema schema;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   * @param documentSchema {@link Schema}
   */
  public DynamicObjectToDocumentAttributeRecord(final String documentId,
      final Schema documentSchema) {
    this.attributeDocumentId = documentId;
    this.schema = documentSchema;
  }

  private void addToList(final Collection<DocumentAttributeRecord> list,
      final DocumentAttributeValueType valueType, final String key, final String stringValue,
      final Boolean boolValue, final Double numberValue) {

    DocumentAttributeRecord a = new DocumentAttributeRecord();
    a.key(key);
    a.documentId(this.attributeDocumentId);
    a.stringValue(stringValue);
    a.booleanValue(boolValue);
    a.numberValue(numberValue);
    a.valueType(valueType);

    list.add(a);
  }

  @Override
  public Collection<DocumentAttributeRecord> apply(final Collection<DynamicObject> objs) {

    Collection<DocumentAttributeRecord> list = convert(objs);

    if (this.schema != null) {
      Collection<DocumentAttributeRecord> compositeKeys = createCompositeKeys(list);
      list.addAll(compositeKeys);
    }

    return list;
  }

  /**
   * Create Composite Keys from {@link Collection} {@link DocumentAttributeRecord}.
   * 
   * @param list {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  private Collection<DocumentAttributeRecord> createCompositeKeys(
      final Collection<DocumentAttributeRecord> list) {

    Map<String, List<DocumentAttributeRecord>> documentAttributeKeys =
        list.stream().collect(Collectors.groupingBy(DocumentAttributeRecord::getKey));

    Collection<DocumentAttributeRecord> compositeKeys = new ArrayList<>();

    notNull(this.schema.getAttributes().getCompositeKeys()).forEach(a -> {
      Collection<DocumentAttributeRecord> keys = createCompositeKeys(a, documentAttributeKeys);
      compositeKeys.addAll(keys);
    });

    return compositeKeys;
  }

  /**
   * Create Composite Keys from {@link SchemaAttributesCompositeKey}.
   * 
   * @param schemaAttributes {@link SchemaAttributesCompositeKey}
   * @param documentAttributeKeys {@link Map}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  @SuppressWarnings("boxing")
  private Collection<DocumentAttributeRecord> createCompositeKeys(
      final SchemaAttributesCompositeKey schemaAttributes,
      final Map<String, List<DocumentAttributeRecord>> documentAttributeKeys) {

    Collection<DocumentAttributeRecord> compositeKeys = new ArrayList<>();

    List<List<DocumentAttributeRecord>> compositeRecords = schemaAttributes.getAttributeKeys()
        .stream().filter(attributeKey -> documentAttributeKeys.containsKey(attributeKey))
        .map(key -> documentAttributeKeys.get(key)).toList();

    if (schemaAttributes.getAttributeKeys().size() == compositeRecords.size()) {

      int listSize = compositeRecords.stream().map(l -> Integer.valueOf(l.size())).reduce(1,
          (aa, bb) -> aa.intValue() * bb.intValue());

      if (listSize > 0) {

        List<List<String>> newCompositeKeys = new ArrayList<>();
        List<List<String>> newCompositeValues = new ArrayList<>();

        for (int i = 0; i < listSize; i++) {
          newCompositeKeys.add(new ArrayList<>());
          newCompositeValues.add(new ArrayList<>());
        }

        for (List<DocumentAttributeRecord> cr : compositeRecords) {

          int index = 0;
          int loop = listSize / cr.size();

          for (DocumentAttributeRecord c : cr) {
            for (int i = 0; i < loop; i++) {

              newCompositeKeys.get(index).add(c.getKey());

              switch (c.getValueType()) {
                case STRING:
                  newCompositeValues.get(index).add(c.getStringValue());
                  break;
                case NUMBER:
                  String s = Objects.formatDouble(c.getNumberValue(), Objects.DOUBLE_FORMAT);
                  newCompositeValues.get(index).add(s);
                  break;
                default:
                  throw new IllegalArgumentException("Unexpected value: " + c.getValueType());
              }

              index++;
            }
          }
        }

        compositeKeys.addAll(createCompositeKeys(newCompositeKeys, newCompositeValues));
      }
    }

    return compositeKeys;
  }

  /**
   * Create Composite Keys from Keys and Values.
   * 
   * @param compositeKeys {@link List} {@link String}
   * @param compositeValues {@link List} {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  private List<DocumentAttributeRecord> createCompositeKeys(final List<List<String>> compositeKeys,
      final List<List<String>> compositeValues) {

    List<DocumentAttributeRecord> records = new ArrayList<>();

    for (int i = 0; i < compositeKeys.size(); i++) {

      String compositeKey = compositeKeys.get(i).stream().collect(Collectors.joining("#"));
      String stringValue = compositeValues.get(i).stream().collect(Collectors.joining("#"));

      DocumentAttributeRecord r = new DocumentAttributeRecord();
      r.key(compositeKey);
      r.documentId(this.attributeDocumentId);
      r.valueType(DocumentAttributeValueType.COMPOSITE_STRING);
      r.stringValue(stringValue);
      records.add(r);
    }

    return records;
  }

  private Collection<DocumentAttributeRecord> convert(final Collection<DynamicObject> objs) {
    Collection<DocumentAttributeRecord> list = new ArrayList<>();

    for (DynamicObject o : objs) {

      String key = o.getString("key");

      List<String> stringValues = getStringValues(o);
      if (!Objects.isEmpty(stringValues)) {

        for (String value : stringValues) {
          addToList(list, DocumentAttributeValueType.STRING, key, value, null, null);
        }
      }

      List<Double> numberValues = getNumberValues(o);
      if (!Objects.isEmpty(numberValues)) {

        for (Double value : numberValues) {
          addToList(list, DocumentAttributeValueType.NUMBER, key, null, null, value);
        }
      }

      Boolean bool = (Boolean) o.getOrDefault("booleanValue", null);
      if (bool != null) {
        addToList(list, DocumentAttributeValueType.BOOLEAN, key, null, bool, null);
      }
    }
    return list;
  }

  private List<Double> getNumberValues(final DynamicObject o) {
    List<Double> list = o.getDoubleList("numberValues");
    if (Objects.isEmpty(list)) {
      list = new ArrayList<>();

      Double s = o.getDouble("numberValue");
      if (s != null) {
        list.add(s);
      }
    }

    return list;
  }

  private List<String> getStringValues(final DynamicObject o) {

    List<String> list = o.getStringList("stringValues");
    if (Objects.isEmpty(list)) {
      list = new ArrayList<>();

      String s = o.getString("stringValue");
      if (!Strings.isEmpty(s)) {
        list.add(s);
      }
    }

    return list;
  }

}
