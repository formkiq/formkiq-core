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
package com.formkiq.stacks.dynamodb;

import com.formkiq.strings.ObjectPath;
import java.util.List;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Parses the supported JSON path subset and resolves native DynamoDB document fields. */
final class JsonAttributeSearchPath {

  static List<Object> parse(final String path) {
    if (path == null || !path.startsWith("$") || path.length() < 2) {
      throw new IllegalArgumentException("invalid JSON search path");
    }
    try {
      List<Object> segments = ObjectPath.parse(path).segments();
      if (segments.contains("")) {
        throw new IllegalArgumentException("empty JSON search key");
      }
      return segments;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("invalid JSON search path", e);
    }
  }

  static AttributeValue resolve(final AttributeValue root, final List<Object> segments) {
    AttributeValue current = root;
    for (Object segment : segments) {
      if (current == null) {
        break;
      }
      if (segment instanceof String key) {
        current = current.hasM() ? current.m().get(key) : null;
      } else {
        int index = (Integer) segment;
        current = current.hasL() && index < current.l().size() ? current.l().get(index) : null;
      }
    }
    return current;
  }

  private JsonAttributeSearchPath() {}
}
