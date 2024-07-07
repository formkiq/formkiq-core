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

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Add Document Tag.
 */
@Reflectable
public class AddDocumentTag {

  /** Document Tag Key. */
  private String key;
  /** Document Tag Value. */
  private String value;
  /** Document Tag Values. */
  private List<String> values;

  /**
   * constructor.
   */
  public AddDocumentTag() {

  }

  /**
   * Get Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Set Tag Key.
   * 
   * @param tagKey {@link String}
   */
  public void setKey(final String tagKey) {
    this.key = tagKey;
  }

  /**
   * Get Tag Value.
   * 
   * @return {@link String}
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Set Tag Value.
   * 
   * @param tagValue {@link String}
   */
  public void setValue(final String tagValue) {
    this.value = tagValue;
  }

  /**
   * Get Tag Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getValues() {
    return this.values;
  }

  /**
   * Set Tag Values.
   * 
   * @param tagValues {@link List} {@link String}
   */
  public void setValues(final List<String> tagValues) {
    this.values = tagValues;
  }
}
