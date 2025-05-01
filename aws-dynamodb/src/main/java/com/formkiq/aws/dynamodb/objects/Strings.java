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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 
 * {@link String} Helper.
 *
 */
public class Strings {
  /** Scheme Authority. */
  private static final String SCHEME_AUTHORITY = "://";

  /**
   * Return text or elseText if the text is empty.
   * 
   * @param text {@link String}
   * @param elseText {@link String}
   * @return {@link String}
   */
  public static String isNotNullOrEmptyElse(final String text, final String elseText) {
    return !isEmpty(text) ? text : elseText;
  }

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
          }).toList();

      o = matches.size() == 1 ? Optional.of(String.join("/", matches.get(0))) : Optional.empty();
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
        strs.stream().filter(Objects::nonNull).map(r -> r.split("/")).collect(Collectors.toList());

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
    return pos > -1 ? filename.substring(pos + 1) : "";
  }

  /**
   * Get Filename from Path.
   * 
   * @param path {@link String}
   * @return {@link String}
   */
  public static String getFilename(final String path) {

    String name = path;

    if (isUrl(name)) {

      int protocolPos = name.indexOf(SCHEME_AUTHORITY);
      String protocol = name.substring(0, protocolPos);

      int pos = protocolPos + SCHEME_AUTHORITY.length();
      name = name.substring(pos);

      pos = name.lastIndexOf("/");
      name = pos > -1 ? name.substring(pos + 1) : "s3".equalsIgnoreCase(protocol) ? name : "";

    } else {

      int pos = name.lastIndexOf("/");
      name = pos > -1 ? name.substring(pos + 1) : name;
    }

    return name;
  }

  public static boolean isUrl(final String s) {
    String regex = "^[a-zA-Z][a-zA-Z0-9]+" + SCHEME_AUTHORITY + "[a-zA-Z0-9]+.*$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(s);
    return matcher.matches();
  }

  private static String getUri(final String path) {

    String name;

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
    return cs == null || cs.isEmpty();
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
    return s.replaceAll("[!.,?]$", "");
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

  /**
   * Convert {@link Exception} to {@link String}.
   *
   * @param e {@link Exception}
   * @return {@link String}
   */
  public static String toString(final Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * XOR {@link String} {@link List}.
   * 
   * @param strs {@link Collection} {@link String}
   * @return boolean
   */
  public static boolean isEmptyOrHasValues(final String... strs) {

    int len = strs.length;
    long countEmpty = Arrays.stream(strs).filter(s -> isEmpty(s)).count();
    return len == countEmpty || countEmpty == 0;
  }

  /**
   * Truncates a string to a specified maximum number of characters. If the string is shorter than
   * or equal to the maximum length, it is returned unchanged.
   *
   * @param input the string to be truncated
   * @param maxLength the maximum number of characters to retain
   * @return the truncated string
   */
  public static String truncate(final String input, final int maxLength) {
    if (input == null || maxLength < 0) {
      throw new IllegalArgumentException(
          "Input cannot be null and maxLength must be non-negative.");
    }

    return input.length() <= maxLength ? input : input.substring(0, maxLength);
  }
}
