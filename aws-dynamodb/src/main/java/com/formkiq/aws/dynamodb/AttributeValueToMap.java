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
package com.formkiq.aws.dynamodb;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link Map}.
 *
 */
public class AttributeValueToMap
    implements Function<Map<String, AttributeValue>, Map<String, Object>> {

  /** {@link AttributeValueToMapConfig}. */
  private final AttributeValueToMapConfig config;

  /**
   * Convert {@link AttributeValue} to {@link Map}.
   */
  public AttributeValueToMap() {
    this(null);
  }

  /**
   * Convert {@link AttributeValue} to {@link Map}.
   * 
   * @param attributeValueToMapConfig {@link AttributeValueToMapConfig}
   */
  public AttributeValueToMap(final AttributeValueToMapConfig attributeValueToMapConfig) {
    this.config = attributeValueToMapConfig;
  }

  @Override
  public Map<String, Object> apply(final Map<String, AttributeValue> map) {

    Map<String, Object> result = new HashMap<>();

    if (map != null) {
      for (Map.Entry<String, AttributeValue> e : notNull(map.entrySet())) {

        String key = getKey(e.getKey());

        Object obj = convert(e.getValue());
        result.put(key, obj);
      }

      removeKeys(result);
      renameKeys(result);
      deleteKeys(result);
    }

    return result;
  }

  private void deleteKeys(final Map<String, Object> result) {
    if (this.config != null) {
      notNull(this.config.getDeleteKeys()).forEach(result::remove);
    }
  }

  private void renameKeys(final Map<String, Object> result) {
    if (this.config != null) {
      this.config.getRenameKeys().forEach((k, v) -> result.put(v, result.get(k)));
      this.config.getRenameKeys().forEach((k, v) -> result.remove(k));
    }
  }

  private void removeKeys(final Map<String, Object> result) {
    if (config != null && config.isRemoveDbKeys()) {
      result.remove(DbKeys.PK);
      result.remove(DbKeys.SK);
      result.remove(DbKeys.GSI1_PK);
      result.remove(DbKeys.GSI1_SK);
      result.remove(DbKeys.GSI2_PK);
      result.remove(DbKeys.GSI2_SK);
    }
  }

  private Object convert(final AttributeValue val) {
    Object obj;

    switch (val.type()) {
      case S -> obj = val.s();
      case N -> obj = Double.valueOf(val.n());
      case BOOL -> obj = val.bool();
      case L -> obj = val.l().stream().map(this::convert).toList();
      case NUL -> obj = null;
      case M -> obj = val.m().entrySet().stream()
          .filter(e -> !AttributeValue.Type.NUL.equals(e.getValue().type()))
          .collect(Collectors.toMap(Map.Entry::getKey, e -> convert(e.getValue())));
      default -> throw new IllegalArgumentException(
          "Unsupported attribute value map conversion " + val.type());
    }

    return obj;
  }

  private String getKey(final String key) {

    String k = key;
    if ("inserteddate".equals(key)) {
      k = "insertedDate";
    } else if (k.startsWith("fk#")) {
      final int pos = 3;
      k = k.substring(pos);
    }

    return k;
  }
}
