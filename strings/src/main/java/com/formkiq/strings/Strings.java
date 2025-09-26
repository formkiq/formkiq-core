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
package com.formkiq.strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Strings Helper.
 */
public class Strings {

  /** Three. */
  private static final int THREE = 3;

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
   * Splits the given string by each occurrence of the specified delimiter character.
   *
   * @param input the string to split
   * @param delimiter the character to split on
   * @return an array of substrings obtained by splitting the input string
   */
  public static String[] splitByChar(final String input, final char delimiter) {

    String[] result = new String[0];
    if (input != null) {

      int start = 0;
      List<String> parts = new ArrayList<>();

      for (int i = 0; i < input.length(); i++) {
        if (input.charAt(i) == delimiter) {
          parts.add(input.substring(start, i));
          start = i + 1;
        }
      }

      parts.add(input.substring(start));

      result = parts.toArray(new String[0]);
    }

    return result;
  }

  /**
   * Validates whether the provided string is numeric.
   *
   * @param str the string to validate
   * @return true if the string is numeric, false otherwise
   */
  public static boolean isNumeric(final String str) {

    boolean match = false;

    if (!isEmpty(str)) {
      try {
        // Attempt to parse the string as a double.
        // This covers integers, decimals, and numbers in scientific notation.
        Double.parseDouble(str);
        match = true;
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    return match;
  }

  /**
   * Tests {@link String} is ISO 639 compliant language code.
   *
   * @param str {@link String}
   * @return boolean
   */
  private static boolean isLanguageCodeIso639(final String str) {
    return isAllLowerCase(str) && (str.length() == 2 || str.length() == THREE);
  }

  /**
   * Tests {@link String} against ISO 3166 alpha-2 country code.
   *
   * @param str {@link String}
   * @return boolean
   */
  private static boolean isCountryCodeIso3166(final String str) {
    return isAllUpperCase(str) && str.length() == 2;
  }

  /**
   * Tests {@link String} against the UN M.49 numeric area code.
   *
   * @param str {@link String}
   * @return boolean
   */
  private static boolean isNumericAreaCode(final String str) {
    return isNumeric(str) && str.length() == THREE;
  }

  /**
   * Parse {@link String} to {@link Locale}.
   * 
   * @param str {@link String}
   * @return Locale
   */
  public static Locale parseLocale(final String str) {

    String[] parts = notNull(str).split("[-_]");
    int len = parts.length;

    return switch (len) {
      case 1 -> isLanguageCodeIso639(str) ? new Locale(str) : null;
      case 2 -> isLanguageCodeIso639(parts[0]) && isCountryCodeIso(parts[1])
          ? new Locale(parts[0], parts[1])
          : null;
      case THREE ->
        isLanguageCodeIso639(parts[0]) && isCountryCodeIso(parts[1]) && !parts[2].isEmpty()
            ? new Locale(parts[0], parts[1], parts[2])
            : null;
      default -> null;
    };
  }

  /**
   * Convert Null {@link String} to "".
   * 
   * @param str {@link String}
   * @return String
   */
  public static String notNull(final String str) {
    return str != null ? str : "";
  }

  private static boolean isCountryCodeIso(final String s) {
    return isCountryCodeIso3166(s) || isNumericAreaCode(s);
  }

  /**
   * Checks if {@link String} only contains lowercase characters.
   * 
   * @param s {@link String}
   * @return boolean
   */
  public static boolean isAllLowerCase(final String s) {
    return isAllCase(s, true);
  }

  /**
   * Checks if {@link String} only contains uppercase characters.
   *
   * @param s {@link String}
   * @return boolean
   */
  public static boolean isAllUpperCase(final String s) {
    return isAllCase(s, false);
  }

  /**
   * Is {@link String} all the case.
   * 
   * @param s {@link String}
   * @param lower true for all lowercase, false for uppercase
   * @return boolean
   */
  private static boolean isAllCase(final String s, final boolean lower) {
    boolean match = false;

    if (!isEmpty(s)) {

      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if ((lower && Character.isLowerCase(c)) || (!lower && Character.isUpperCase(c))) {
          match = true;
        } else {
          match = false;
          break;
        }
      }
    }

    return match;
  }

  /**
   * Find first {@link String} that is not empty.
   * 
   * @param strs {@link String}
   * @return String
   */
  public static String notEmpty(final String... strs) {
    for (String str : strs) {
      if (!isEmpty(str)) {
        return str;
      }
    }

    return null;
  }

  /**
   * Returns a list containing the elements in list1 but not in list2.
   *
   * @param <T> Type of {@link List}
   * @param list1 the list from which to retain only elements not present in list2
   * @param list2 the list whose elements should be excluded
   * @return a list of elements that are in list2 but not in list1
   */
  public static <T> Collection<T> complement(final Collection<T> list1, final Collection<T> list2) {
    Collection<T> set1 = new HashSet<>(list2);
    return list1.stream().filter(element -> !set1.contains(element)).collect(Collectors.toList());
  }

  /**
   * Split a {@link String} using lastIndexOf.
   * 
   * @param s {@link String}
   * @param split {@link String}
   * @return {@link SplitResult}
   */
  public static SplitResult lastIndexOf(final String s, final String split) {
    int pos = s.lastIndexOf(split);
    return pos > 0 ? new SplitResult(s.substring(0, pos), s.substring(pos + 1)) : null;
  }

  public record SplitResult(String before, String after) {
  }
}
