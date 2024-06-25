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
package com.formkiq.stacks.api.handler;

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;

/**
 * Add Attribute Class.
 */
@Reflectable
public class AddAttribute {

  /** {@link AttributeDataType}. */
  private AttributeDataType dataType;
  /** Key of Attribute. */
  private String key;
  /** Type of Attribute. */
  private AttributeType type;

  /**
   * constructor.
   */
  public AddAttribute() {

  }

  /**
   * Get {@link AttributeDataType}.
   * 
   * @return {@link AttributeDataType}
   */
  public AttributeDataType getDataType() {
    return this.dataType;
  }

  /**
   * Set Data Type.
   * 
   * @param attributeDataType {@link AttributeDataType}
   * @return AddAttribute
   */
  public AddAttribute setDataType(final AttributeDataType attributeDataType) {
    this.dataType = attributeDataType;
    return this;
  }

  /**
   * Get Key {@link String}.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Set Key.
   * 
   * @param attributeKey {@link String}
   * @return AddAttribute
   */
  public AddAttribute setKey(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Get {@link AttributeType}.
   * 
   * @return {@link AttributeType}
   */
  public AttributeType getType() {
    return this.type;
  }

  /**
   * Set Attribute Type.
   * 
   * @param attributeType {@link AttributeType}
   * @return AddAttribute
   */
  public AddAttribute setType(final AttributeType attributeType) {
    this.type = attributeType;
    return this;
  }
}
