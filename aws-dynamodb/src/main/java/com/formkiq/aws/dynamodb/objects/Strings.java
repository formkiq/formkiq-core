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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 
 * {@link String} Helper.
 *
 */
public class Strings {
  private static String findMatch(final Collection<String> resourceUrls,
      final List<String[]> resourceSplits, final String path) {

    Optional<String> o = resourceUrls.stream().filter(r -> r != null && r.equals(path)).findAny();

    if (o.isEmpty()) {

      String[] s = path.split("/");
      List<String[]> matches =
          resourceSplits.stream().filter(r -> r.length == s.length).filter(r -> {

            boolean match = false;

            for (int i = 0; i < r.length; i++) {
              match = r[i].equals(s[i]) || (r[i].startsWith("{") && r[i].endsWith("}"));
              if (!match) {
                break;
              }
            }

            return match;
          }).collect(Collectors.toList());

      o = matches.size() == 1
          ? Optional.of(Arrays.asList(matches.get(0)).stream().collect(Collectors.joining("/")))
          : Optional.empty();
    }

    return o.orElse(null);
  }

  /**
   * Find Best Match of {@link String}.
   * 
   * @param strs {@link Collection} {@link String}
   * @param s {@link String}
   * @return {@link String}
   */
  public static String findUrlMatch(final Collection<String> strs, final String s) {
    List<String[]> resourceSplits =
        strs.stream().filter(r -> r != null).map(r -> r.split("/")).collect(Collectors.toList());

    return findMatch(strs, resourceSplits, s);
  }

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

    String name = getUri(path);
    int pos = name.lastIndexOf("/");
    name = pos > -1 ? name.substring(pos + 1) : name;
    return name;
  }

  private static String getUri(final String path) {

    String name = null;

    try {
      URI u = new URI(path);
      name = u.getPath();
    } catch (URISyntaxException e) {
      name = path;
    }

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
