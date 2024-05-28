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
package com.formkiq.stacks.lambda.s3.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.IoUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Unit Test for {@link TokenGeneratorDefault}.
 */
public class TokenGeneratorTest {

  /** {@link IdpTextMatcher}. */
  private final IdpTextMatcher idpTextMatcher = new IdpTextMatcher();

  /**
   * Text Matcher with Fuzzy Logic.
   * 
   * @throws IOException IOException
   */
  @Test
  void testIdpTextMatcher01() throws IOException {
    // given
    String text = IoUtils.toUtf8String(new FileInputStream("src/test/resources/text/text01.txt"));
    List<String> textLabels = Arrays.asList("P.O. Number", "PO Number", "Purchase Order Number");

    // when
    final TextMatch match =
        idpTextMatcher.findMatch(text, textLabels, new TokenGeneratorDefault(), new FuzzyMatcher());
    final String matchValue0 = idpTextMatcher.findMatchValue(text, match, "");
    final String matchValue1 = idpTextMatcher.findMatchValue(text, match, "[0-9]+");

    // then
    assertEquals("P.O. NO.:", match.getToken().getOriginal());
    assertEquals("po no", match.getToken().getFormatted());
    assertEquals("6200041751", matchValue0);
    assertEquals("6200041751", matchValue1);
  }

  /**
   * Text Matcher with Exact Logic.
   * 
   * @throws IOException IOException
   */
  @Test
  void testIdpTextMatcher02() throws IOException {
    // given
    String text = IoUtils.toUtf8String(new FileInputStream("src/test/resources/text/text01.txt"));
    List<String> textLabels = List.of("P.O. No");

    // when
    final TextMatch match =
        idpTextMatcher.findMatch(text, textLabels, new TokenGeneratorDefault(), new ExactMatcher());
    final String matchValue0 = idpTextMatcher.findMatchValue(text, match, "");
    final String matchValue1 = idpTextMatcher.findMatchValue(text, match, "[0-9]+");

    // then
    assertEquals("P.O. NO.:", match.getToken().getOriginal());
    assertEquals("po no", match.getToken().getFormatted());
    assertEquals("6200041751", matchValue0);
    assertEquals("6200041751", matchValue1);
  }

  /**
   * Text Matcher with Exact Mismatch.
   *
   * @throws IOException IOException
   */
  @Test
  void testIdpTextMatcher03() throws IOException {
    // given
    String text = IoUtils.toUtf8String(new FileInputStream("src/test/resources/text/text01.txt"));
    List<String> textLabels = List.of("P.O. Number");

    // when
    final TextMatch match =
        idpTextMatcher.findMatch(text, textLabels, new TokenGeneratorDefault(), new ExactMatcher());
    final String matchValue0 = idpTextMatcher.findMatchValue(text, match, "");
    final String matchValue1 = idpTextMatcher.findMatchValue(text, match, "[0-9]+");

    // then
    assertNull(match);
    assertNull(matchValue0);
    assertNull(matchValue1);
  }
}
