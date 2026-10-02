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

import com.formkiq.aws.dynamodb.model.JsonAttributeSearchFilter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.ToNumberPolicy;
import java.lang.reflect.Type;
import java.util.Set;

/** Enforces JSON filter field types and retains exact numeric comparison values. */
public final class JsonAttributeSearchFilterDeserializer
    implements JsonDeserializer<JsonAttributeSearchFilter> {

  /** Supported comparison fields. */
  private static final Set<String> FIELDS =
      Set.of("path", "eq", "eqOr", "beginsWith", "gt", "gte", "lt", "lte");
  /** Numeric bound fields. */
  private static final Set<String> BOUNDS = Set.of("gt", "gte", "lt", "lte");
  /** Delegate without this adapter, retaining decimal comparison precision. */
  private final Gson gson =
      new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();

  @Override
  public JsonAttributeSearchFilter deserialize(final JsonElement json, final Type type,
      final JsonDeserializationContext context) throws JsonParseException {
    if (!json.isJsonObject()) {
      throw new JsonParseException("json search requires an object");
    }
    JsonObject object = json.getAsJsonObject();
    object.entrySet().forEach(entry -> validateField(entry.getKey(), entry.getValue()));
    return this.gson.fromJson(object, JsonAttributeSearchFilter.class);
  }

  private void validateField(final String key, final JsonElement value) {
    if (!FIELDS.contains(key) || value.isJsonNull()) {
      throw new JsonParseException("invalid JSON search field: " + key);
    }
    switch (key) {
      case "path", "beginsWith" -> {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
          throw new JsonParseException(key + " requires a string");
        }
      }
      case "eqOr" -> {
        if (!value.isJsonArray()) {
          throw new JsonParseException("json eqOr requires an array");
        }
        value.getAsJsonArray().forEach(this::validateScalar);
      }
      default -> {
        validateScalar(value);
        if (BOUNDS.contains(key) && !value.getAsJsonPrimitive().isNumber()) {
          throw new JsonParseException(key + " requires a number");
        }
      }
    }
  }

  private void validateScalar(final JsonElement value) {
    if (!value.isJsonPrimitive()) {
      throw new JsonParseException("JSON comparisons require a string, number, or boolean");
    }
  }
}
