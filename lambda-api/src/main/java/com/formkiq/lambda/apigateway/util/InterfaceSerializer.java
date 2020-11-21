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
package com.formkiq.lambda.apigateway.util;

import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * {@link Gson} {@link JsonSerializer} for converting Interface to Object.
 * 
 *
 * @param <T> Type of {@link Class}
 */
public class InterfaceSerializer<T> implements JsonSerializer<T>, JsonDeserializer<T> {
  /** Interface {@link Class}. */
  private final Class<T> implementationClass;

  /**
   * constructor.
   * 
   * @param clazz {@link Class}
   */
  public InterfaceSerializer(final Class<T> clazz) {
    this.implementationClass = clazz;
  }

  static <T> InterfaceSerializer<T> interfaceSerializer(final Class<T> implementationClass) {
    return new InterfaceSerializer<>(implementationClass);
  }

  @Override
  public JsonElement serialize(final T value, final Type type,
      final JsonSerializationContext context) {
    final Type targetType = value != null ? value.getClass() : type;
    return context.serialize(value, targetType);
  }

  @Override
  public T deserialize(final JsonElement jsonElement, final Type typeOfT,
      final JsonDeserializationContext context) {
    return context.deserialize(jsonElement, this.implementationClass);
  }
}
