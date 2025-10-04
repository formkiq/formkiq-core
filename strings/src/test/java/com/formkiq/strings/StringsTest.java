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

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link Strings}.
 */
public class StringsTest {
  private void assertLocale(final Locale locale, final String country, final String language,
      final String variant) {
    assertNotNull(locale);
    assertEquals(country, locale.getCountry());
    assertEquals(language, locale.getLanguage());
    if (variant != null) {
      assertEquals(variant, locale.getVariant());
    }
  }

  @Test
  void isAllLowerCase() {
    assertTrue(Strings.isAllLowerCase("adad"));
    assertFalse(Strings.isAllLowerCase("aDad"));
    assertFalse(Strings.isAllLowerCase("adad2"));
    assertFalse(Strings.isAllLowerCase(""));
    assertFalse(Strings.isAllLowerCase(null));
  }

  @Test
  void isAllUpperCase() {
    assertTrue(Strings.isAllUpperCase("ADAD"));
    assertFalse(Strings.isAllUpperCase("aDad"));
    assertFalse(Strings.isAllUpperCase("ADSD2"));
    assertFalse(Strings.isAllUpperCase(""));
    assertFalse(Strings.isAllUpperCase(null));
  }

  @Test
  void isLastSplit() {
    Strings.SplitResult r = Strings.lastIndexOf("mystring/something", "/");
    assertNotNull(r);
    assertEquals("mystring", r.before());
    assertEquals("something", r.after());

    r = Strings.lastIndexOf("mystringsomething", "/");
    assertNull(r);
  }

  @Test
  void isNumeric() {
    assertFalse(Strings.isNumeric(null));
    assertFalse(Strings.isNumeric(""));
    assertTrue(Strings.isNumeric("1"));
    assertTrue(Strings.isNumeric("1.234"));
  }

  @Test
  void notNull() {
    assertNull(Strings.notEmpty(null, null, ""));
    assertEquals("a", Strings.notEmpty(null, "", "a"));
    assertEquals("b", Strings.notEmpty(null, "b", "a"));
  }

  /**
   * Is Empty.
   */
  @Test
  void testIsEmpty() {
    assertTrue(Strings.isEmpty(null));
    assertTrue(Strings.isEmpty(""));
    assertFalse(Strings.isEmpty(" "));
    assertFalse(Strings.isEmpty("a"));
  }

  @Test
  void testParseLocale() {
    assertLocale(Strings.parseLocale("en"), "", "en", null);
    assertLocale(Strings.parseLocale("fr"), "", "fr", null);
    assertLocale(Strings.parseLocale("fr-124"), "124", "fr", null);
    assertLocale(Strings.parseLocale("en-US"), "US", "en", null);
    assertLocale(Strings.parseLocale("fr_FR"), "FR", "fr", null);
    assertLocale(Strings.parseLocale("en-US-POSIX"), "US", "en", "POSIX");
    assertLocale(Strings.parseLocale("eng"), "", "eng", null);
    assertNull(Strings.parseLocale("boourns"));
    assertNull(Strings.parseLocale("a"));
    assertNull(Strings.parseLocale(""));
    assertNull(Strings.parseLocale(null));
  }

  @Test
  void testSplitByChar() {
    assertEquals("a d sa add", String.join(" ", Strings.splitByChar("a:d:sa:add", ':')));
    assertEquals("adsa add", String.join(" ", Strings.splitByChar("adsa:add", ':')));
    assertEquals("adsa:add", String.join(" ", Strings.splitByChar("adsa:add", '!')));
    assertEquals("", String.join(" ", Strings.splitByChar(null, '!')));
  }
}
