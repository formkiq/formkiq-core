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
package com.formkiq.module.ocr;

import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * {@link Comparator} That Naturally sorts Strings that end in a number.
 * 
 * IE: test/2 should sort after test/10
 *
 */
public class S3KeysNaturalComparator implements Comparator<String>, Serializable {

  /** Number Pattern. */
  private static final Pattern PATTERN = Pattern.compile("(.*)/(\\D*)(\\d+)$");
  /** Serial Version UID. */
  private static final long serialVersionUID = -8307896084449315528L;

  @Override
  public int compare(final String s1, final String s2) {

    int result = s1.compareTo(s2);

    Matcher m1 = PATTERN.matcher(s1);
    Matcher m2 = PATTERN.matcher(s2);

    if (m1.find() && m2.find()) {

      if (m1.group(1).equals(m2.group(1)) && m1.group(2).equals(m2.group(2))) {
        int i = 2 + 1;
        String p1 = m1.group(i);
        String p2 = m2.group(i);
        result = Integer.valueOf(p1).compareTo(Integer.valueOf(p2));

      }
    }

    return result;
  }
}
