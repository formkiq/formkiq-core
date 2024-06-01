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
import java.util.function.Function;

/**
 * {@link Function} that implements Fuzzy {@link String} matching.
 */
public class FuzzyMatcher implements TextMatchAlgorithm {

  /**
   * constructor.
   */
  public FuzzyMatcher() {}

  @Override
  public List<TextMatch> findMatches(final List<Token> tokens, final String match) {

    List<TextMatch> matches = new ArrayList<>();

    for (Token token : tokens) {
      int score = fuzzyScore(token.getFormatted(), match);
      if (score > 0) {
        matches.add(new TextMatch(token, score));
      }
    }

    return matches;
  }

  private Integer fuzzyScore(final CharSequence term, final CharSequence query) {
    if (term == null || query == null) {
      throw new IllegalArgumentException("CharSequences must not be null");
    }

    // the resulting score
    int score = 0;
    int beginningBonus = 1;

    // the position in the term which will be scanned next for potential
    // query character matches
    int termIndex = 0;

    // index of the previously matched character in the term
    int previousMatchingCharacterIndex = Integer.MIN_VALUE;

    for (int queryIndex = 0; queryIndex < query.length(); queryIndex++) {
      final char queryChar = query.charAt(queryIndex);

      boolean termCharacterMatchFound = false;
      for (; termIndex < term.length() && !termCharacterMatchFound; termIndex++) {
        final char termChar = term.charAt(termIndex);

        if (queryChar == termChar) {
          // simple character matches result in one point
          score++;

          if (queryIndex == 0) {
            score += beginningBonus;
          }

          // subsequent character matches further improve
          // the score.
          if (previousMatchingCharacterIndex + 1 == termIndex) {
            score += (2 + beginningBonus);
          }

          previousMatchingCharacterIndex = termIndex;

          // we can leave the nested loop. Every character in the
          // query can match at most one character in the term.
          termCharacterMatchFound = true;

        } else {
          beginningBonus = 0;
        }

      }
    }

    // exact term match get a bonus
    if (term.equals(query)) {
      score++;
    }

    return score;
  }
}
