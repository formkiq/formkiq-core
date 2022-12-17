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

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * 
 * Document Metadata.
 *
 */
@Reflectable
public class DocumentMetadata {

  /** {@link String} Key. */
  @Reflectable
  private String key;
  /** {@link String} Value. */
  @Reflectable
  private String value;
  /** {@link List} {@link String} Values. */
  @Reflectable
  private List<String> values;

  /**
   * constructor.
   */
  public DocumentMetadata() {
    // empty
  }

  /**
   * constructor.
   * 
   * @param metadataKey {@link String}
   * @param metadataValue {@link List} {@link String}
   */
  public DocumentMetadata(final String metadataKey, final List<String> metadataValue) {
    setKey(metadataKey);
    setValues(metadataValue);
  }

  /**
   * constructor.
   * 
   * @param metadataKey {@link String}
   * @param metadataValue {@link String}
   */
  public DocumentMetadata(final String metadataKey, final String metadataValue) {
    setKey(metadataKey);
    setValue(metadataValue);
  }

  /**
   * Get Metadata Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Get Metadata Value.
   * 
   * @return {@link String}
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Get Metadata Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getValues() {
    return this.values;
  }

  /**
   * Set Metadata Key.
   * 
   * @param metadataKey {@link String}
   */
  public void setKey(final String metadataKey) {
    this.key = metadataKey;
  }

  /**
   * Set Metadata Value.
   * 
   * @param metadataValue {@link String}
   */
  public void setValue(final String metadataValue) {
    this.value = metadataValue;
  }

  /**
   * Set Metadata Values.
   * 
   * @param metadataValues {@link List} {@link String}
   */
  public void setValues(final List<String> metadataValues) {
    this.values = metadataValues;
  }
}
