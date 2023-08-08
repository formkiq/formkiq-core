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

  /**
   * Load File as Byte Array.
   * 
   * @param caller {@link Object}
   * @param filename {@link String}
   * @return byte[]
   * @throws IOException IOException
   */
  public static byte[] loadFileAsByteArray(final Object caller, final String filename)
      throws IOException {
    try (InputStream in = caller.getClass().getResourceAsStream(filename)) {
      if (in == null) {
        throw new FileNotFoundException(filename);
      }
      return in.readAllBytes();
    }
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
   * private constructor.
   */
  private FileUtils() {

  }
}
