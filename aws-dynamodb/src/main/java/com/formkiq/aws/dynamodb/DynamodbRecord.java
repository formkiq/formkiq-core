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

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * DynamoDB Keys.
 * 
 * @param <T> Type of DynamodbRecord
 *
 */
public interface DynamodbRecord<T> {

  /**
   * Convert {@link Map} {@link AttributeValue}.
   * 
   * @param attrs {@link Map} {@link AttributeValue}
   * @param key {@link String}
   * @return {@link Boolean}
   */
  default Boolean bb(final Map<String, AttributeValue> attrs, final String key) {
    AttributeValue av = attrs.get(key);
    return av != null ? av.bool() : Boolean.FALSE;
  }

  /**
   * Convert {@link String} to {@link AttributeValue}.
   * 
   * @param str {@link String}
   * @return {@link AttributeValue}
   */
  default AttributeValue fromS(final String str) {
    return AttributeValue.fromS(str);
  }

  /**
   * Get {@link AttributeValue} {@link Map}.
   * 
   * @param siteId {@link String}
   * @return {@link AttributeValue} {@link Map}
   */
  Map<String, AttributeValue> getAttributes(String siteId);

  /**
   * Transform {@link Map} {@link AttributeValue} to {@link Object}.
   * 
   * @param siteId {@link String}
   * @param attrs {@link Map} {@link AttributeValue}
   * @return {@link Object}
   */
  T getFromAttributes(String siteId, Map<String, AttributeValue> attrs);

  /**
   * Convert {@link Map} {@link AttributeValue}.
   * 
   * @param attrs {@link Map} {@link AttributeValue}
   * @param key {@link Double}
   * @return {@link String}
   */
  default Double nn(final Map<String, AttributeValue> attrs, final String key) {
    AttributeValue av = attrs.get(key);
    return av != null ? Double.valueOf(av.n()) : null;
  }

  /**
   * Get DynamoDb PK.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  String pk(String siteId);

  /**
   * GSI 1 PK.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  String pkGsi1(String siteId);

  /**
   * GSI 2 PK.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  String pkGsi2(String siteId);

  /**
   * Get DynamoDb SK.
   * 
   * @return {@link String}
   */
  String sk();

  /**
   * GSI 1 SK.
   * 
   * @return {@link String}
   */
  String skGsi1();

  /**
   * GSI 2 SK.
   * 
   * @return {@link String}
   */
  String skGsi2();

  /**
   * Convert {@link Map} {@link AttributeValue}.
   * 
   * @param attrs {@link Map} {@link AttributeValue}
   * @param key {@link String}
   * @return {@link String}
   */
  default String ss(final Map<String, AttributeValue> attrs, final String key) {
    AttributeValue av = attrs.get(key);
    return av != null ? av.s() : null;
  }
}
