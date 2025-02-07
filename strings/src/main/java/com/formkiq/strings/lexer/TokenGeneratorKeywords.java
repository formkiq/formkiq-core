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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * {@link TokenGenerator} Keywords.
 */
public class TokenGeneratorKeywords implements TokenGenerator {

  /** {@link Collection} Keywords. */
  private final Collection<String> keywords;

  public TokenGeneratorKeywords(final Collection<String> tokenKeywords) {
    this.keywords = new HashSet<>(tokenKeywords);
  }

  private int processWhitespace(final List<Token> tokens, final char[] chars, final int start,
      final int length) {

    int pos = start;
    int whitespacePos = -1;
    while (pos < length && Character.isWhitespace(chars[pos])) {
      if (whitespacePos == -1) {
        whitespacePos = pos;
      }
      pos++;
    }

    if (whitespacePos > -1) {
      tokens
          .add(new Token().setOriginal(" ").setType(TokenType.WHITESPACE).setStart(whitespacePos));
    }

    return pos;
  }

  @Override
  public List<Token> generateTokens(final String input) {

    List<Token> tokens = new ArrayList<>();
    char[] chars = input.toCharArray();
    final int length = chars.length;
    int pos = 0;

    while (pos < length) {
      pos = processWhitespace(tokens, chars, pos, length);
      if (pos >= length) {
        break;
      }

      int start = pos;
      char currentChar = chars[pos];

      // Determine which type of token we are about to read:
      if (isIdentifierStart(currentChar)) {
        // Read an identifier (or potential keyword).
        do {
          pos++;
        } while (pos < length && isIdentifierPart(chars[pos]));

      } else if (Character.isDigit(currentChar)) {
        // Read a number (integer only in this example).
        do {
          pos++;
        } while (pos < length && Character.isDigit(chars[pos]));

      } else {
        // Any other character (like punctuation or operators) is a token by itself.
        pos++;
      }

      String tokenText = new String(chars, start, pos - start);
      TokenType type = determineTokenType(tokenText);
      tokens.add(new Token().setOriginal(tokenText).setType(type).setStart(start));
    }

    return tokens;
  }

  private boolean isIdentifierStart(final char ch) {
    return Character.isLetter(ch) || ch == '_';
  }

  // Returns true if the character is valid in the rest of an identifier.
  private boolean isIdentifierPart(final char ch) {
    return Character.isLetterOrDigit(ch) || ch == '_';
  }

  // Determines the token type based on its content.
  private TokenType determineTokenType(final String tokenText) {

    TokenType type;
    if (keywords.contains(tokenText)) {
      type = TokenType.KEYWORD;
    } else if (Character.isDigit(tokenText.charAt(0))) {
      type = TokenType.NUMBER;
    } else if (isIdentifierStart(tokenText.charAt(0))) {
      type = TokenType.IDENTIFIER;
    } else {

      type = switch (tokenText.charAt(0)) {
        case '[' -> TokenType.LEFT_BRACKET;
        case ']' -> TokenType.RIGHT_BRACKET;
        case '(' -> TokenType.LEFT_PAREN;
        case ')' -> TokenType.RIGHT_PAREN;
        default -> TokenType.SYMBOL;
      };
    }

    return type;
  }
}
