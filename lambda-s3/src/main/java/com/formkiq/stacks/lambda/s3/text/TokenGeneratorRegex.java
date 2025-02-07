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

import com.formkiq.strings.StringFormatter;
import com.formkiq.strings.lexer.Token;
import com.formkiq.strings.lexer.TokenGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex Token Generator.
 */
public class TokenGeneratorRegex implements TokenGenerator {

  /** Regex. */
  private final String regexString;
  /** {@link StringFormatter}. */
  private final StringFormatter stringFormatter;

  /**
   * constructor.
   * 
   * @param regex {@link String}
   * @param formatter {@link StringFormatter}
   */
  public TokenGeneratorRegex(final String regex, final StringFormatter formatter) {
    this.regexString = regex;
    this.stringFormatter = formatter;
  }

  @Override
  public List<Token> generateTokens(final String text) {

    Pattern pattern = Pattern.compile(this.regexString);
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
    return new Token().setOriginal(s).setFormatted(stringFormatter.format(s)).setStart(start)
        .setEnd(end);
  }
}
