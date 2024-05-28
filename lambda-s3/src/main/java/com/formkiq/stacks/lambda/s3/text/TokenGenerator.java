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
import java.util.stream.IntStream;

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

    List<Token> results = tokens;

    if (tokenGroupSize > 1) {

      List<StringBuffer> newFormattedTokens = new ArrayList<>();
      List<StringBuffer> newOriginalTokens = new ArrayList<>();

      Iterator<Token> itr = tokens.iterator();

      for (int i = 0; i < tokenGroupSize - 1; i++) {

        if (itr.hasNext()) {
          Token token = itr.next();

          StringBuffer tk = new StringBuffer(token.getFormatted().trim());
          newFormattedTokens.add(tk);

          StringBuffer or = new StringBuffer(token.getOriginal());
          newOriginalTokens.add(or);
        }
      }

      while (itr.hasNext()) {

        Token token = itr.next();

        StringBuffer tk = new StringBuffer(token.getFormatted().trim());
        newFormattedTokens.add(tk);

        StringBuffer or = new StringBuffer(token.getOriginal());
        newOriginalTokens.add(or);

        int size = newFormattedTokens.size();
        for (int i = 1; i < tokenGroupSize; i++) {
          newFormattedTokens.get(size - 1 - i).append(" ").append(token.getFormatted());
          newOriginalTokens.get(size - 1 - i).append(" ").append(token.getOriginal());
        }
      }

      results = IntStream.range(0, newFormattedTokens.size())
          .mapToObj(i -> new Token().setOriginal(newOriginalTokens.get(i).toString())
              .setFormatted(newFormattedTokens.get(i).toString()))
          .toList();
    }

    return results;
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
