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
package com.formkiq.module.lambda.lucene;

import java.util.Map;
import java.util.function.Function;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import com.formkiq.aws.dynamodb.DbKeys;

/**
 * 
 * {@link Function} for converting {@link Map} to {@link Document}.
 *
 */
public class DocumentTagMapToDocument implements Function<Map<String, Object>, Document> {

  @Override
  public Document apply(final Map<String, Object> data) {

    Document document = new Document();

    String tagKey = getValue(data.get("tagKey"));
    String tagValue = getValue(data.get("tagValue"));
    String documentId = getValue(data.get("documentId"));

    String key = "tags" + DbKeys.TAG_DELIMINATOR + tagKey;
    IndexableField field0 = new StringField(key, tagValue, Field.Store.YES);
    document.add(field0);

    IndexableField field1 = new StringField("documentId", documentId, Field.Store.YES);
    document.add(field1);

    return document;
  }

  /**
   * Get Value.
   * 
   * @param object {@link Object}
   * @return {@link String}
   */
  @SuppressWarnings("unchecked")
  private String getValue(final Object object) {

    String value = null;

    if (object instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) object;
      if (map.containsKey("S")) {
        value = map.get("S").toString();
      }
    }

    return value;
  }

}
