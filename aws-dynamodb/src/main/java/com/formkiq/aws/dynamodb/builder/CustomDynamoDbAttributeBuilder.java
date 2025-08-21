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
package com.formkiq.aws.dynamodb.builder;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

/**
 * {@link DynamoDbAttributeMapBuilder} for saving Custom Objects.
 */
public interface CustomDynamoDbAttributeBuilder {

  /**
   * Builds {@link AttributeValue} for DynamoDb.
   * 
   * @param name {@link String}
   * @param value {@link Object}
   * @return Map {@link AttributeValue}
   */
  Map<String, AttributeValue> encode(String name, Object value);

  /**
   * Decode {@link Map} {@link AttributeValue} to {@link Object}.
   * 
   * @param attrs {@link Map} {@link AttributeValue}
   * @return Object
   * @param <T> Type of object
   */
  <T> T decode(Map<String, AttributeValue> attrs);
}
