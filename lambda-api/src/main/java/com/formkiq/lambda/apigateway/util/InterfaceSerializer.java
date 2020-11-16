/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
