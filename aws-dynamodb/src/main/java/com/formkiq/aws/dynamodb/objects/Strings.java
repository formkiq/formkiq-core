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
package com.formkiq.aws.dynamodb.objects;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 
 * {@link String} Helper.
 *
 */
public class Strings {
  /**
   * Generate Random String.
   * 
   * @param len int
   * @return {@link String}
   */
  public static String generateRandomString(final int len) {
    final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      int randomIndex = random.nextInt(chars.length());
      sb.append(chars.charAt(randomIndex));
    }

    return sb.toString();
  }

  /**
   * Get Filename from Path.
   * 
   * @param filename {@link String}
   * @return {@link String}
   */
  public static String getExtension(final String filename) {
    int pos = filename.lastIndexOf(".");
    String ext = pos > -1 ? filename.substring(pos + 1) : "";
    return ext;
  }

  /**
   * Get Filename from Path.
   * 
   * @param path {@link String}
   * @return {@link String}
   */
  public static String getFilename(final String path) {
    int pos = path.lastIndexOf("/");
    String name = pos > -1 ? path.substring(pos + 1) : path;
    return name;
  }

  /**
   * Is {@link String} empty.
   * 
   * @param cs {@link CharSequence}
   * @return boolean
   */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  /**
   * Is {@link String} a {@link UUID}.
   *
   * @param s {@link String}
   * @return boolean
   */
  public static boolean isUuid(final String s) {
    try {
      UUID.fromString(s);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Remove '/' from start/end of {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String removeBackSlashes(final String s) {
    return s.replaceAll("^/|/$", "");
  }

  /**
   * Remove single/double quotes from {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String removeEndingPunctuation(final String s) {
    return s.replaceAll("[!\\.,?]$", "");
  }
  
  /**
   * Remove single/double quotes from {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  public static String removeQuotes(final String s) {
    return s.replaceAll("^['\"]|['\"]$", "");
  }
}
