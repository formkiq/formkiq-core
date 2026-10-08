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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Parses the supported JSON path subset and resolves native DynamoDB document fields. */
final class JsonAttributeSearchPath {

  /** One property name or zero-based array position. */
  private static final Pattern SEGMENT =
      Pattern.compile("\\G(?:\\.([A-Za-z_][A-Za-z0-9_]*)" + "|\\[(0|[1-9][0-9]*)\\]"
          + "|\\['((?:[^'\\\\]|\\\\.)+)'\\]" + "|\\[\"((?:[^\"\\\\]|\\\\.)+)\"\\])");
  /** Capture group for single-quoted property names. */
  private static final int SINGLE_QUOTED = 3;
  /** Capture group for double-quoted property names. */
  private static final int DOUBLE_QUOTED = 4;

  static List<Object> parse(final String path) {
    if (path == null || !path.startsWith("$") || path.length() < 2) {
      throw new IllegalArgumentException("invalid JSON search path");
    }
    Matcher matcher = SEGMENT.matcher(path);
    matcher.region(1, path.length());
    List<Object> segments = new ArrayList<>();
    int end = 1;
    while (matcher.find()) {
      segments.add(segment(matcher));
      end = matcher.end();
    }
    if (end != path.length()) {
      throw new IllegalArgumentException("invalid JSON search path");
    }
    return List.copyOf(segments);
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

  private static Object segment(final Matcher matcher) {
    if (matcher.group(1) != null) {
      return matcher.group(1);
    }
    if (matcher.group(2) != null) {
      try {
        return Integer.valueOf(matcher.group(2));
      } catch (NumberFormatException e) {
        return Integer.MAX_VALUE;
      }
    }
    String quoted = matcher.group(SINGLE_QUOTED) != null ? matcher.group(SINGLE_QUOTED)
        : matcher.group(DOUBLE_QUOTED);
    StringBuilder name = new StringBuilder();
    for (int i = 0; i < quoted.length(); i++) {
      char character = quoted.charAt(i);
      if (character == '\\') {
        character = quoted.charAt(++i);
      }
      name.append(character);
    }
    return name.toString();
  }

  private JsonAttributeSearchPath() {}
}
