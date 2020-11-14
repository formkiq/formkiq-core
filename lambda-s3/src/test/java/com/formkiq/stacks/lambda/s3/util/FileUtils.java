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
package com.formkiq.stacks.lambda.s3.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.utils.IoUtils;

/**
 * FileUtils.
 *
 */
public final class FileUtils {

  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().create();

  /**
   * private constructor.
   */
  private FileUtils() {

  }

  /**
   * Loads File into a {@link Map}.
   * 
   * @param caller {@link Object}.
   * @param filename {@link String}
   * @return {@link Map}
   * @throws IOException IOException
   */
  public static Map<String, Object> loadFileAsMap(final Object caller, final String filename)
      throws IOException {
    return loadFileAsMap(caller, filename, new String[] {});
  }

  /**
   * Loads File into a {@link Map}.
   * 
   * @param caller {@link Object}.
   * @param filename {@link String}
   * @param regexs {@link String}
   * 
   * @return {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> loadFileAsMap(final Object caller, final String filename,
      final String... regexs) throws IOException {

    String json = loadFile(caller, filename);

    if (regexs.length > 0) {
      if (regexs.length % 2 != 0) {
        throw new IllegalArgumentException("Regex must have regex & replacement");
      }

      for (int i = 0; i < regexs.length; i += 2) {
        String regex = regexs[i];
        String replacement = regexs[i + 1];
        json = json.replaceAll(regex, replacement);
      }
    }

    final Map<String, Object> map = GSON.fromJson(json, Map.class);

    return map;
  }

  /**
   * Loads File into a {@link Map}.
   * 
   * @param caller {@link Object}.
   * @param filename {@link String}
   * 
   * @return {@link String}
   * @throws IOException IOException
   */
  public static String loadFile(final Object caller, final String filename) throws IOException {

    try (InputStream in = caller.getClass().getResourceAsStream(filename)) {

      if (in == null) {
        throw new FileNotFoundException(filename);
      }

      return IoUtils.toUtf8String(in);
    }
  }
}
