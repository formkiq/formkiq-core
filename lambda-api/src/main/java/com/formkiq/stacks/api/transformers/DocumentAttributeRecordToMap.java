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

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.DynamodbRecordToMap;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;

/**
 * {@link Function} to merge {@link DocumentAttributeRecord} with the same keys together.
 */
public class DocumentAttributeRecordToMap
    implements Function<Collection<DocumentAttributeRecord>, Collection<Map<String, Object>>> {

  /** Whether Keys should be unique. */
  private final boolean uniqueKeys;

  /**
   * constructor.
   * 
   * @param uniqueAttributeKeys boolean
   */
  public DocumentAttributeRecordToMap(final boolean uniqueAttributeKeys) {
    this.uniqueKeys = uniqueAttributeKeys;
  }

  @Override
  public Collection<Map<String, Object>> apply(
      final Collection<DocumentAttributeRecord> attributes) {

    DocumentAttributeRecord last = null;
    Map<String, Object> lastValues = null;
    Collection<Map<String, Object>> c = new ArrayList<>();

    for (DocumentAttributeRecord a : attributes) {

      if (uniqueKeys && lastValues != null && last != null && last.getKey().equals(a.getKey())) {

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

        c.add(lastValues);

        last = a;
      }
    }

    return c;
  }

  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
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

}
