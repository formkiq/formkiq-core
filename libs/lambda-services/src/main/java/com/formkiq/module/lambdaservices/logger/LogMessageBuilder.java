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
package com.formkiq.module.lambdaservices.logger;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;

/**
 * Utility for building structured log messages with a title and key/value properties.
 *
 * Example output:
 *
 * UploadDocument | documentId=123 | userId=abc | size=2048
 */
public final class LogMessageBuilder {

  /** Strict JSON parser for log properties. */
  private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
  /** Replacement for redacted values and invalid JSON. */
  private static final String REDACTED = "[REDACTED]";

  private static void redact(final JsonElement value, final Set<String> fields) {
    if (value.isJsonObject()) {
      value.getAsJsonObject().entrySet().forEach(entry -> {
        if (fields.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
          entry.setValue(new JsonPrimitive(REDACTED));
        } else {
          redact(entry.getValue(), fields);
        }
      });
    } else if (value.isJsonArray()) {
      value.getAsJsonArray().forEach(element -> redact(element, fields));
    }
  }

  /**
   * Start building a log message with a title.
   * 
   * @param title {@link String}
   * @return {@link LogMessageBuilder}
   */
  public static LogMessageBuilder title(final String title) {
    return new LogMessageBuilder(title);
  }

  /** Title. */
  private final String title;

  /** Properties. */
  private final Map<String, Object> properties = new LinkedHashMap<>();

  private LogMessageBuilder(final String logTitle) {
    this.title = Objects.requireNonNull(logTitle, "title must not be null");
  }

  /**
   * Build the final log message string.
   * 
   * @return {@link String}
   */
  public String build() {
    StringJoiner joiner = new StringJoiner(" | ");

    joiner.add(title);

    properties.forEach((k, v) -> joiner.add(k + "=" + v));

    return joiner.toString();
  }

  /**
   * Add multiple properties at once.
   * 
   * @param values {@link Map}
   * @return {@link LogMessageBuilder}
   */
  public LogMessageBuilder properties(final Map<String, ?> values) {
    if (values != null) {
      values.forEach(this::property);
    }
    return this;
  }

  /**
   * Add a key/value property (ignored if value is null).
   * 
   * @param key {@link String}
   * @param value {@link Object}
   * @return {@link LogMessageBuilder}
   */
  public LogMessageBuilder property(final String key, final Object value) {
    if (key != null && value != null) {
      properties.put(key, value);
    }
    return this;
  }

  /**
   * Add a JSON property, compacted onto one line without redaction. Null keys or values are
   * ignored. Invalid or blank JSON is replaced with a placeholder.
   *
   * @param key property key
   * @param json JSON value
   * @return this builder
   */
  public LogMessageBuilder propertyJson(final String key, final String json) {
    return propertyJson(key, json, List.of());
  }

  /**
   * Add a JSON property with matching fields replaced by a placeholder at every nesting level,
   * including inside arrays. Field names are matched case-insensitively. Unlisted fields remain
   * visible. Null keys or JSON values are ignored; invalid or blank JSON is never logged verbatim.
   *
   * @param key property key
   * @param json JSON value
   * @param redactFields field names to redact; must not be null
   * @return this builder
   */
  public LogMessageBuilder propertyJson(final String key, final String json,
      final Collection<String> redactFields) {
    if (key == null || json == null) {
      return this;
    }
    Set<String> fields = redactFields.stream().filter(Objects::nonNull)
        .map(field -> field.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    try {
      JsonElement value = GSON.fromJson(json, JsonElement.class);
      if (value == null) {
        return property(key, REDACTED);
      }
      redact(value, fields);
      return property(key, value.toString());
    } catch (JsonParseException e) {
      return property(key, REDACTED);
    }
  }
}
