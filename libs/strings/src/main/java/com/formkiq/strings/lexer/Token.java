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
package com.formkiq.strings.lexer;

/**
 * {@link String} Token.
 */
public class Token {

  /** {@link TokenType}. */
  private TokenType type;
  /** Formatted Text. */
  private String formatted;
  /** Original Text. */
  private String original;
  /** Start Position. */
  private int start;
  /** End Position. */
  private int end;

  /**
   * constructor.
   */
  public Token() {

  }

  /**
   * Get End Position.
   * 
   * @return int
   */
  public int getEnd() {
    return this.end;
  }

  /**
   * Get Formatted Text.
   * 
   * @return {@link String}
   */
  public String getFormatted() {
    return this.formatted;
  }

  /**
   * Get Original Text.
   * 
   * @return String
   */
  public String getOriginal() {
    return this.original;
  }

  /**
   * Get Start Position.
   * 
   * @return int
   */
  public int getStart() {
    return this.start;
  }

  /**
   * Get {@link TokenType}.
   * 
   * @return {@link TokenType}
   */
  public TokenType getType() {
    return this.type;
  }

  /**
   * Set End Position.
   * 
   * @param endPosition int
   * @return Token
   */
  public Token setEnd(final int endPosition) {
    this.end = endPosition;
    return this;
  }

  /**
   * Set Formatted Text.
   * 
   * @param text {@link String}
   * @return Token
   */
  public Token setFormatted(final String text) {
    this.formatted = text;
    return this;
  }

  /**
   * Set Original Text.
   * 
   * @param text {@link String}
   * @return Token
   */
  public Token setOriginal(final String text) {
    this.original = text;
    return this;
  }

  public Token setStart(final int startPosition) {
    this.start = startPosition;
    return this;
  }

  /**
   * Set {@link TokenType}.
   * 
   * @param tokenType {@link TokenType}
   * @return {@link Token}
   */
  public Token setType(final TokenType tokenType) {
    this.type = tokenType;
    return this;
  }
}
