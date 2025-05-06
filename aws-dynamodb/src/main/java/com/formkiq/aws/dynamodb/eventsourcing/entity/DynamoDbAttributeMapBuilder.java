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
package com.formkiq.aws.dynamodb.eventsourcing.entity;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import com.formkiq.aws.dynamodb.eventsourcing.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Builder for assembling a DynamoDB item attribute map.
 * <p>
 * Only non-null values are added. Supports String, Number, and Boolean types.
 */
public class DynamoDbAttributeMapBuilder {

  /** Attribute {@link Map}. */
  private final Map<String, AttributeValue> attributes = new HashMap<>();

  /**
   * constructor.
   */
  private DynamoDbAttributeMapBuilder() {}

  /**
   * Static factory method to create a new builder instance.
   *
   * @return a new DynamoDbAttributeMapBuilder
   */
  public static DynamoDbAttributeMapBuilder builder() {
    return new DynamoDbAttributeMapBuilder();
  }

  /**
   * Adds a String attribute if the value is non-null.
   *
   * @param name the attribute name
   * @param value the String value
   * @return this builder
   */
  public DynamoDbAttributeMapBuilder withString(final String name, final String value) {
    if (value != null) {
      attributes.put(name, AttributeValue.builder().s(value).build());
    }
    return this;
  }

  /**
   * Adds a Number attribute if the value is non-null.
   *
   * @param name the attribute name
   * @param value the numeric value
   * @return this builder
   */
  public DynamoDbAttributeMapBuilder withNumber(final String name, final Number value) {
    if (value != null) {
      attributes.put(name, AttributeValue.builder().n(value.toString()).build());
    }
    return this;
  }

  /**
   * Adds a Boolean attribute if the value is non-null.
   *
   * @param name the attribute name
   * @param value the Boolean value
   * @return this builder
   */
  public DynamoDbAttributeMapBuilder withBoolean(final String name, final Boolean value) {
    if (value != null) {
      attributes.put(name, AttributeValue.builder().bool(value).build());
    }
    return this;
  }

  /**
   * Builds the attribute map to be used in a DynamoDB Put/Update operation.
   *
   * @return a map of attribute names to AttributeValue
   */
  public Map<String, AttributeValue> build() {
    return attributes;
  }

  /**
   * Adds a {@link Date} attribute if the value is non-null.
   *
   * @param name the attribute name
   * @param value the Boolean value
   * @return this builder
   */
  public DynamoDbAttributeMapBuilder withDate(final String name, final Date value) {
    if (value != null) {
      String val = DynamoDbTypes.fromDate(value);
      attributes.put(name, AttributeValue.builder().s(val).build());
    }

    return this;
  }
}

