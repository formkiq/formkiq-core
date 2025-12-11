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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

/**
 * Comparator for sorting a {@link java.util.List} {@link Map} {@link AttributeValue} by a specific
 * AttributeValue key.
 *
 * If scanIndexForward == true, sorts ascending. If scanIndexForward == false, sorts descending.
 */
public class AttributeValueComparator
    implements Comparator<Map<String, AttributeValue>>, Serializable {

  /** Field Key. */
  private final String key;
  /** Scan Index Forward. */
  private final boolean scanForward;

  public AttributeValueComparator(final String fieldKey, final boolean scanIndexForward) {
    this.key = Objects.requireNonNull(fieldKey, "fieldKey cannot be null");
    this.scanForward = scanIndexForward;
  }

  @Override
  public int compare(final Map<String, AttributeValue> a, final Map<String, AttributeValue> b) {

    Comparator<Map<String, AttributeValue>> base =
        Comparator.comparing(this::getString, Comparator.nullsFirst(String::compareTo));

    return scanForward ? base.compare(a, b) : base.reversed().compare(a, b);
  }

  private String getString(final Map<String, AttributeValue> m) {
    AttributeValue v = m.get(key);
    return v == null ? null : v.s();
  }
}
