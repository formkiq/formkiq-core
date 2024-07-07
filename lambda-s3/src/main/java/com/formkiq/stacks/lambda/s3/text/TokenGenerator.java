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
import java.util.Iterator;
import java.util.List;

/**
 * Token Generator.
 */
public interface TokenGenerator {

  /**
   * Generate Tokens.
   * 
   * @param text {@link String}
   * @return {@link List} {@link Token}
   */
  List<Token> generateTokens(String text);

  /**
   * Group Tokens to a certain size.
   *
   * @param tokens {@link List} {@link Token}
   * @param tokenGroupSize int
   * @return {@link List} {@link Token}
   */
  default List<Token> groupTokens(final List<Token> tokens, final int tokenGroupSize) {

    List<Token> groupedTokens = tokenGroupSize > 1 ? new ArrayList<>() : new ArrayList<>(tokens);

    if (tokenGroupSize > 1) {

      Iterator<Token> itr = tokens.iterator();

      for (int i = 0; i < tokenGroupSize - 1; i++) {

        if (itr.hasNext()) {
          Token token = itr.next();
          groupedTokens.add(createToken(token));
        }
      }

      while (itr.hasNext()) {

        Token token = itr.next();
        groupedTokens.add(createToken(token));

        int size = groupedTokens.size();

        for (int i = 1; i < tokenGroupSize; i++) {
          Token groupedToken = groupedTokens.get(size - 1 - i);
          groupedToken.setOriginal(groupedToken.getOriginal() + " " + token.getOriginal());
          groupedToken.setFormatted(groupedToken.getFormatted() + " " + token.getFormatted());
          groupedToken.setEnd(token.getEnd());
        }
      }
    }

    return groupedTokens;
  }

  private Token createToken(final Token token) {
    return new Token().setOriginal(token.getOriginal()).setFormatted(token.getFormatted())
        .setStart(token.getStart()).setEnd(token.getEnd());
  }

  /**
   * Get Split Regex.
   * 
   * @return String
   */
  String getSplitRegex();

  /**
   * Formats Text before tokens are generated.
   * 
   * @param text {@link String}
   * @return {@link String}
   */
  String formatText(String text);
}
