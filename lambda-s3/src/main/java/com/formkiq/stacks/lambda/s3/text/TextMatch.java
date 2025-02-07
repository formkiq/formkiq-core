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

import com.formkiq.strings.lexer.Token;

/**
 * Text Match.
 */
public class TextMatch {
  /** Token Matched. */
  private final Token token;
  /** Match Score. */
  private final int score;

  /**
   * constructor.
   * 
   * @param matchToken {@link Token}
   * @param matchScore int
   */
  public TextMatch(final Token matchToken, final int matchScore) {
    this.token = matchToken;
    this.score = matchScore;
  }

  /**
   * Get Score.
   * 
   * @return int
   */
  public int getScore() {
    return this.score;
  }

  /**
   * Get {@link Token}.
   * 
   * @return {@link String}
   */
  public Token getToken() {
    return this.token;
  }
}
