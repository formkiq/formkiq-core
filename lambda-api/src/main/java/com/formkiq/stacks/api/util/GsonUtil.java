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
package com.formkiq.stacks.api.util;

import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** {@link Gson} utils. */
public final class GsonUtil {

  /** Date Format. */
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  /** {@link Gson}. */
  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().setDateFormat(DATE_FORMAT).registerTypeAdapter(
          DocumentItem.class, new InterfaceSerializer<>(DocumentItemDynamoDb.class)).create();

  /** private constructor. */
  private GsonUtil() {}

  /**
   * Get Instance of {@link Gson}.
   *
   * @return {@link Gson}
   */
  public static Gson getInstance() {
    return GSON;
  }
}
