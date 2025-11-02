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

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


/**
 * Builder for assembling a DynamoDB item attribute map.
 * <p>
 * Only non-null values are added. Supports String, Number, and Boolean types.
 */
public class DynamoDbAttributeMapBuilder {

  /**
   * Static factory method to create a new builder instance.
   *
   * @return a new DynamoDbAttributeMapBuilder
   */
  public static DynamoDbAttributeMapBuilder builder() {
    return new DynamoDbAttributeMapBuilder();
  }

  /** Attribute {@link Map}. */
  private final Map<String, AttributeValue> attributes = new HashMap<>();

  /**
   * constructor.
   */
  private DynamoDbAttributeMapBuilder() {}

  /**
   * Builds the attribute map to be used in a DynamoDB Put/Update operation.
   *
   * @return a map of attribute names to AttributeValue
   */
  public Map<String, AttributeValue> build() {
    return attributes;
  }

  /**
   * With {@link AttributeValue}.
   * 
   * @param name {@link String}
   * @param value {@link AttributeValue}
   * @return DynamoDbAttributeMapBuilder
   */
  public DynamoDbAttributeMapBuilder withAttributeValue(final String name,
      final AttributeValue value) {
    if (value != null) {
      attributes.put(name, value);
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
   * Adds a Custom attribute if the value is non-null.
   *
   * @param name the attribute name
   * @param value the {@link Object} value
   * @param custom {@link CustomDynamoDbAttributeBuilder}
   * @return this builder
   */
  public DynamoDbAttributeMapBuilder withCustom(final String name, final Object value,
      final CustomDynamoDbAttributeBuilder custom) {
    if (value != null) {
      Map<String, AttributeValue> av = custom.encode(name, value);
      attributes.putAll(av);
    }

    return this;
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

  /**
   * Adds {@link Collection} of {@link Enum}.
   *
   * @param name {@link String}
   * @param value {@link Enum}
   * @param <E> Type of Enum.
   * @return {@link DynamoDbAttributeMapBuilder}
   */
  public <E extends Enum<E>> DynamoDbAttributeMapBuilder withEnum(final String name,
      final E value) {
    if (value != null) {
      attributes.put(name, AttributeValue.builder().s(value.name()).build());
    }
    return this;
  }

  /**
   * Adds {@link Collection} of {@link Enum}.
   *
   * @param name {@link String}
   * @param values {@link Collection}
   * @param <E> Type of Enum.
   * @return {@link DynamoDbAttributeMapBuilder}
   */
  public <E extends Enum<E>> DynamoDbAttributeMapBuilder withEnumList(final String name,
      final Collection<E> values) {
    if (values != null) {
      Collection<AttributeValue> val =
          values.stream().map(v -> AttributeValue.builder().s(v.name()).build()).toList();
      attributes.put(name, AttributeValue.builder().l(val).build());
    }
    return this;
  }

  /**
   * Add Attribute {@link List}.
   * 
   * @param name {@link String}
   * @param values {@link List}
   * @return builder
   */
  public DynamoDbAttributeMapBuilder withList(final String name,
      final List<Map<String, Object>> values) {
    if (values != null) {
      AttributeValue list = new ObjectToAttributeValue().apply(values);
      attributes.put(name, list);
    }
    return this;
  }

  /**
   * Adds a String attribute if the value is non-null.
   *
   * @param name the attribute name
   * @param value the {@link Map} value
   * @return this builder
   */
  public DynamoDbAttributeMapBuilder withMap(final String name, final Map<String, Object> value) {
    if (value != null) {
      Map<String, AttributeValue> map = new MapToAttributeValue().apply(value);
      attributes.put(name, AttributeValue.builder().m(map).build());
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
   * Set a Time to Live in days.
   *
   * @param days long
   * @return DynamoDbAttributeMapBuilder
   */
  public DynamoDbAttributeMapBuilder withTimeToLiveInDays(final long days) {
    final long secondsPerDay = 86400;
    long expiresAt = Instant.now().getEpochSecond() + days * secondsPerDay;
    attributes.put("TimeToLive", AttributeValue.fromN(Long.toString(expiresAt)));
    return this;
  }

  /**
   * Set a Time to Live in hours.
   *
   * @param hours long
   * @return DynamoDbAttributeMapBuilder
   */
  public DynamoDbAttributeMapBuilder withTimeToLiveInHours(final long hours) {
    final long secondsPerHour = 3600;
    long expiresAt = Instant.now().getEpochSecond() + hours * secondsPerHour;
    attributes.put("TimeToLive", AttributeValue.fromN(Long.toString(expiresAt)));
    return this;
  }

  /**
   * Set a Time to Live in Seconds.
   *
   * @param ttlSeconds long
   * @return DynamoDbAttributeMapBuilder
   */
  public DynamoDbAttributeMapBuilder withTimeToLiveInSeconds(final long ttlSeconds) {
    long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
    attributes.put("TimeToLive", AttributeValue.fromN(Long.toString(expiresAt)));
    return this;
  }
}
