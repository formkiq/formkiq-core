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

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing an attribute of an entity.
 */
public class EntityAttribute {
  /** Attribute Key. */
  private final String key;
  /** Attribute String Values. */
  private final List<String> stringValues;
  /** Attribute Number Values. */
  private final List<Double> numberValues;
  /** Boolean Values. */
  private final Boolean booleanValue;

  /**
   * constructor.
   * 
   * @param builder {@link Builder}
   */
  private EntityAttribute(final Builder builder) {
    this.key = builder.key;
    this.stringValues = builder.stringValues;
    this.numberValues = builder.numberValues;
    this.booleanValue = builder.booleanValue;
  }

  /**
   * Get {@link String}.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return key;
  }

  /**
   * Get {@link String} {@link List}.
   * 
   * @return {@link String} {@link List}
   */
  public List<String> getStringValues() {
    return stringValues;
  }

  /**
   * Get {@link Double} {@link List}.
   * 
   * @return {@link Double} {@link List}
   */
  public List<Double> getNumberValues() {
    return numberValues;
  }

  /**
   * Get {@link Boolean}.
   * 
   * @return {@link Boolean}
   */
  public Boolean getBooleanValue() {
    return booleanValue;
  }

  /**
   * Create {@link Builder}.
   * 
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link EntityAttribute}.
   */
  public static class Builder {
    /** Attribute Key. */
    private String key;
    /** Attribute String Values. */
    private final List<String> stringValues = new ArrayList<>();
    /** Attribute Number Values. */
    private final List<Double> numberValues = new ArrayList<>();
    /** Boolean Value. */
    private Boolean booleanValue;

    /**
     * Set the attribute attributeKey.
     * 
     * @param attributeKey {@link String}
     * @return Builder
     */
    public Builder key(final String attributeKey) {
      this.key = attributeKey;
      return this;
    }

    /**
     * Set multiple string values.
     * 
     * @param attributeStringValues {@link List} {@link String}
     * @return Builder
     */
    public Builder stringValues(final List<String> attributeStringValues) {
      if (attributeStringValues != null) {
        this.stringValues.addAll(attributeStringValues);
      }
      return this;
    }

    /**
     * Add one string value to the list.
     * 
     * @param value {@link String}
     * @return Builder
     */
    public Builder addStringValue(final String value) {
      if (value != null) {
        this.stringValues.add(value);
      }
      return this;
    }

    /**
     * Set multiple numeric values.
     * 
     * @param attributeNumberValues {@link List} {@link Double}
     * @return Builder
     */
    public Builder numberValues(final List<Double> attributeNumberValues) {
      if (attributeNumberValues != null) {
        this.numberValues.addAll(attributeNumberValues);
      }
      return this;
    }

    /**
     * Add one numeric value to the list.
     * 
     * @param value {@link Double}
     * @return Builder
     */
    public Builder addNumberValue(final Double value) {
      if (value != null) {
        this.numberValues.add(value);
      }
      return this;
    }

    /**
     * Set a boolean value.
     * 
     * @param attributeBooleanValue {@link Boolean}
     * @return Builder
     */
    public Builder booleanValue(final Boolean attributeBooleanValue) {
      this.booleanValue = attributeBooleanValue;
      return this;
    }

    /**
     * Build the {@link EntityAttribute} instance.
     * 
     * @return EntityAttribute
     */
    public EntityAttribute build() {
      return new EntityAttribute(this);
    }
  }
}
