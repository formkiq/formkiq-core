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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token Generator.
 */
public class TokenGeneratorDefault implements TokenGenerator {

  /** Alpha Numeric. */
  private static final String ALPHANUMERIC = "[^a-zA-Z0-9]";

  /**
   * constructor.
   */
  public TokenGeneratorDefault() {}

  @Override
  public List<Token> generateTokens(final String text) {

    Pattern pattern = Pattern.compile(getSplitRegex());
    Matcher matcher = pattern.matcher(text);

    List<Token> tokens = new ArrayList<>();

    int tokenIndex = 0;

    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end() - 1; // end is exclusive in matcher, we need inclusive
      tokens.add(createToken(text, tokenIndex, start));
      tokenIndex = end + 1;
    }

    tokens.add(createToken(text, tokenIndex, text.length()));

    return tokens;
  }

  private Token createToken(final String text, final int start, final int end) {
    String s = text.substring(start, end);
    return new Token().setOriginal(s).setFormatted(formatText(s)).setStart(start).setEnd(end);
  }

  @Override
  public String formatText(final String text) {
    return text.toLowerCase().replaceAll("\\p{Punct}", "").replaceAll(ALPHANUMERIC, " ");
  }

  @Override
  public String getSplitRegex() {
    return "\\s+";
  }
}
