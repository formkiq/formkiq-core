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
 * Convert {@link DynamicObject} to {@link AttributeSearchRecord}.
 */
public class DynamicObjectToAttributeRecord
    implements Function<Collection<DynamicObject>, Collection<AttributeSearchRecord>> {

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

  @Override
  public Collection<AttributeSearchRecord> apply(final Collection<DynamicObject> objs) {

    Collection<AttributeSearchRecord> list = new ArrayList<>();

    for (DynamicObject o : objs) {

      String key = o.getString("key");

      List<String> stringValues = getStringValues(o);
      if (!Objects.isEmpty(stringValues)) {

        for (String value : stringValues) {

          AttributeSearchRecord a = new AttributeSearchRecord();
          a.key(key);
          a.documentId(this.attributeDocumentId);
          a.stringValue(value);
          a.type(AttributeSearchType.STRING);

          list.add(a);
        }
      }
    }
    // a.booleanValue(null);
    // a.numberValue(null);

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
