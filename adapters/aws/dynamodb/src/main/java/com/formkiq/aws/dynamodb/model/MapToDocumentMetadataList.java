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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_METADATA;

/**
 * Convert {@link Map} with Document Metadata keys, IE: starts with "md#" then convert to a
 * {@link List} of {@link Map} with "key", "value" or "values" as keys.
 */
public class MapToDocumentMetadataList
    implements Function<Map<String, Object>, List<Map<String, Object>>> {
  @Override
  public List<Map<String, Object>> apply(final Map<String, Object> map) {

    List<Map<String, Object>> result = new ArrayList<>();

    map.entrySet().stream().filter(e -> e.getKey().startsWith(PREFIX_DOCUMENT_METADATA))
        .forEach((e -> {
          String key = e.getKey().substring(PREFIX_DOCUMENT_METADATA.length());
          if (e.getValue() instanceof Collection<?> c) {
            result.add(Map.of("key", key, "values", c));
          } else {
            result.add(Map.of("key", key, "value", e.getValue()));
          }
        }));

    return result;
  }
}
