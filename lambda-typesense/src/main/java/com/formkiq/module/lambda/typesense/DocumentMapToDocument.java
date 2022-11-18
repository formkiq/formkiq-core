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
package com.formkiq.module.lambda.typesense;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_METADATA;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link Function} for converting {@link Map} to {@link Map}.
 *
 */
public class DocumentMapToDocument implements Function<Map<String, Object>, Map<String, Object>> {

  /** Fields to Process. */
  private static final List<String> FIELDS = Arrays.asList("documentId", "path", "userId");

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> apply(final Map<String, Object> data) {

    Map<String, Object> document = new HashMap<>();

    for (String field : FIELDS) {

      if (data.containsKey(field)) {

        Map<String, Object> values = (Map<String, Object>) data.get(field);
        String value = values.get("S").toString();

        if (!StringUtils.isEmpty(value)) {
          // document.add(new StringField(field, value, Field.Store.YES));
          document.put(field, value);
        }
      }
    }

    List<String> metadata =
        data.entrySet().stream().filter(e -> e.getKey().startsWith(PREFIX_DOCUMENT_METADATA))
            .map(e -> e.getKey()).collect(Collectors.toList());

    metadata.forEach(m -> {
      Map<String, Object> values = (Map<String, Object>) data.get(m);
      String value = values.get("S").toString();
      // document.add(new StringField(m, value, Field.Store.YES));
      document.put(m, value);
    });

    return document;
  }

}
