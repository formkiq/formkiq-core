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

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit Test {@link TokenGeneratorKeywords}.
 */
public class TokenGeneratorKeywordsTest {

  @Test
  void generateTokens01() {
    // given
    TokenGenerator gen = create(List.of("map"));
    String s = "map[cognito:groups:wehroewhrwrwnvv permissionsMap:map[wehroewhrwrwnvv:[READ]]]";

    // when
    List<Token> tokens = gen.generateTokens(s);

    // then
    final int expected = 19;
    assertEquals(expected, tokens.size());

    int i = 0;
    assertTokenEquals(tokens.get(i++), TokenType.KEYWORD, "map");
    assertTokenEquals(tokens.get(i++), TokenType.LEFT_BRACKET, "[");
    assertTokenEquals(tokens.get(i++), TokenType.IDENTIFIER, "cognito");
    assertTokenEquals(tokens.get(i++), TokenType.SYMBOL, ":");
    assertTokenEquals(tokens.get(i++), TokenType.IDENTIFIER, "groups");
    assertTokenEquals(tokens.get(i++), TokenType.SYMBOL, ":");
    assertTokenEquals(tokens.get(i++), TokenType.IDENTIFIER, "wehroewhrwrwnvv");
    assertTokenEquals(tokens.get(i++), TokenType.WHITESPACE, " ");
    assertTokenEquals(tokens.get(i++), TokenType.IDENTIFIER, "permissionsMap");
    assertTokenEquals(tokens.get(i++), TokenType.SYMBOL, ":");
    assertTokenEquals(tokens.get(i++), TokenType.KEYWORD, "map");
    assertTokenEquals(tokens.get(i++), TokenType.LEFT_BRACKET, "[");
    assertTokenEquals(tokens.get(i++), TokenType.IDENTIFIER, "wehroewhrwrwnvv");
    assertTokenEquals(tokens.get(i++), TokenType.SYMBOL, ":");
    assertTokenEquals(tokens.get(i++), TokenType.LEFT_BRACKET, "[");
    assertTokenEquals(tokens.get(i++), TokenType.IDENTIFIER, "READ");
    assertTokenEquals(tokens.get(i++), TokenType.RIGHT_BRACKET, "]");
    assertTokenEquals(tokens.get(i++), TokenType.RIGHT_BRACKET, "]");
    assertTokenEquals(tokens.get(i), TokenType.RIGHT_BRACKET, "]");
  }

  private void assertTokenEquals(final Token token, final TokenType tokenType, final String text) {
    assertEquals(tokenType, token.getType());
    assertEquals(text, token.getOriginal());
  }

  private TokenGenerator create(final Collection<String> tokens) {
    return new TokenGeneratorKeywords(tokens);
  }
}
