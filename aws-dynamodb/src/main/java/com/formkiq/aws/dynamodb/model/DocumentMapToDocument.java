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
package com.formkiq.aws.dynamodb.model;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_METADATA;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * {@link Function} for converting {@link Map} to {@link Map}.
 *
 */
public class DocumentMapToDocument
    implements Function<Map<String, ? extends Object>, Map<String, Object>> {

  /** Fields to Process. */
  private static final List<String> FIELDS =
      Arrays.asList("documentId", "path", "content", "contentType", "deepLinkPath");

  @SuppressWarnings("unchecked")
  private String getValue(final Object obj) {
    String value = null;

    if (obj != null) {

      if (obj instanceof AttributeValue) {

        AttributeValue av = (AttributeValue) obj;
        value = av.s();

      } else if (obj instanceof Map) {

        Map<String, Object> values = (Map<String, Object>) obj;
        if (values.containsKey("S")) {
          value = values.get("S").toString();
        } else if (values.containsKey("s")) {
          value = values.get("s").toString();
        }

      } else if (obj instanceof String) {
        value = obj.toString();
      }
    }

    return value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> apply(final Map<String, ? extends Object> data) {

    Map<String, Object> document = new HashMap<>();

    for (String field : FIELDS) {

      Object ob = data.get(field);
      String value = getValue(ob);

      if (value != null) {
        document.put(field, value);
      }
    }

    List<String> metadata =
        data.entrySet().stream().filter(e -> e.getKey().startsWith(PREFIX_DOCUMENT_METADATA))
            .map(e -> e.getKey()).collect(Collectors.toList());

    metadata.forEach(m -> {

      Map<String, Object> obj = (Map<String, Object>) data.get(m);
      String value = getValue(obj);
      String key = m.replaceAll("md#", "metadata#");
      document.put(key, value);
    });

    if (data.containsKey("metadata")) {
      List<Map<String, String>> list = (List<Map<String, String>>) data.get("metadata");
      for (Map<String, String> map : list) {
        String key = map.get("key");
        String value = map.get("value");
        document.put("metadata#" + key, value != null ? value : "");
      }
    }

    document.put("metadata#", "");

    return document;
  }
}
