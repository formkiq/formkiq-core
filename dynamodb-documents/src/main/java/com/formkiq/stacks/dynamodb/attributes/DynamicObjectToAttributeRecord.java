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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;

/**
 * Convert {@link DynamicObject} to {@link DocumentAttributeRecord}.
 */
public class DynamicObjectToAttributeRecord
    implements Function<Collection<DynamicObject>, Collection<DocumentAttributeRecord>> {

  /** Document Id. */
  private String attributeDocumentId;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   */
  public DynamicObjectToAttributeRecord(final String documentId) {
    this.attributeDocumentId = documentId;
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
