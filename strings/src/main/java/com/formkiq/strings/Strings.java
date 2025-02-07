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
import java.util.List;

/**
 * Strings Helper.
 */
public class Strings {

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
}
