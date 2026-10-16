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
package com.formkiq.strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and resolves property names and zero-based list indices over maps and lists. Paths may be
 * rooted, such as {@code $.items[0].name}, or relative, such as
 * {@code attributes.Contract[0].customer.name}. Quoted bracket keys select literal names, for
 * example {@code auth.claims["custom:department"]} or {@code attributes['Invoice.Number'][0]}. A
 * backslash escapes the next character within a quoted key. Empty quoted keys are supported.
 *
 * <p>
 * Parsed paths are immutable and reusable. Resolution retains native values without conversion or
 * formatting, and returns null for missing keys, null values, out-of-range indices, or an
 * incompatible container type. Indices larger than a Java list can hold always resolve to null. The
 * root path {@code $} selects the supplied root itself. Wildcards, slices, negative indices,
 * recursive descent, and expression evaluation are not supported.
 *
 * <p>
 * To resolve template expressions, compose with {@link TemplateSubstitutor}:
 * 
 * <pre>{@code
 * new TemplateSubstitutor("${", "}").substitute(template,
 *     expression -> ObjectPath.parse(expression).resolve(context));
 * }</pre>
 * 
 * The caller supplies the context, data loading, and final rendering policies.
 */
public final class ObjectPath {

  /** One property name or zero-based list index. */
  private static final Pattern SEGMENT =
      Pattern.compile("\\G(?:\\.([A-Za-z_][A-Za-z0-9_]*)" + "|\\[(0|[1-9][0-9]*)\\]"
          + "|\\['((?:[^'\\\\]|\\\\.)*)'\\]" + "|\\[\"((?:[^\"\\\\]|\\\\.)*)\"\\])");
  /** Capture group for single-quoted property names. */
  private static final int SINGLE_QUOTED = 3;
  /** Capture group for double-quoted property names. */
  private static final int DOUBLE_QUOTED = 4;

  /**
   * Parse a rooted or relative path without evaluating it.
   *
   * @param path the path to parse
   * @return an immutable, reusable path
   * @throws IllegalArgumentException if the path is null, empty, or malformed
   */
  public static ObjectPath parse(final String path) {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("invalid object path");
    }
    String rooted = path.startsWith("$") ? path : path.startsWith("[") ? "$" + path : "$." + path;
    Matcher matcher = SEGMENT.matcher(rooted);
    matcher.region(1, rooted.length());
    List<Object> parsed = new ArrayList<>();
    int end = 1;
    while (matcher.find()) {
      parsed.add(segment(matcher));
      end = matcher.end();
    }
    if (end != rooted.length()) {
      throw new IllegalArgumentException("invalid object path");
    }
    return new ObjectPath(parsed);
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

  /** Parsed string property names and integer indices. */
  private final List<Object> segments;

  private ObjectPath(final List<Object> pathSegments) {
    this.segments = List.copyOf(pathSegments);
  }

  /**
   * Resolve this path over a map/list structure. Map keys must be strings and list positions are
   * zero-based. No reflective access to Java object fields or methods is performed.
   *
   * @param root the root value, which may be null
   * @return the selected value in its original type, or null when it cannot be resolved
   */
  public Object resolve(final Object root) {
    Object current = root;
    for (Object segment : this.segments) {
      if (current == null) {
        break;
      }
      if (segment instanceof String key && current instanceof Map<?, ?> map) {
        current = map.get(key);
      } else if (segment instanceof Integer index && current instanceof List<?> list
          && index < list.size()) {
        current = list.get(index);
      } else {
        current = null;
      }
    }
    return current;
  }

  /**
   * Get parsed steps for adapters that traverse other container representations.
   *
   * @return an immutable list containing string keys and nonnegative integer indices; an index
   *         exceeding the integer range is represented by {@link Integer#MAX_VALUE}
   */
  public List<Object> segments() {
    return this.segments;
  }
}
