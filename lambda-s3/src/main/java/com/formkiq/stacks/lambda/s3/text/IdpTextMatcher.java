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

import com.formkiq.aws.dynamodb.objects.Strings;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Document Processor {@link TextMatcher}.
 */
public class IdpTextMatcher implements TextMatcher {

  @Override
  public TextMatch findMatch(final String text, final List<String> matches,
      final TokenGenerator tokenGenerator, final TextMatchAlgorithm matchAlgorithm) {

    TextMatch bestMatch = null;
    List<Token> tokens = tokenGenerator.generateTokens(text);

    for (String m : matches) {

      String match = tokenGenerator.formatText(m);
      int groupSize = match.split(tokenGenerator.getSplitRegex()).length;

      List<Token> groupTokens = tokenGenerator.groupTokens(tokens, groupSize);

      List<TextMatch> textMatches = matchAlgorithm.findMatches(groupTokens, match);
      Optional<TextMatch> o = textMatches.stream().max(new TextMatchScoreComparator());

      if (o.isPresent()) {
        if (bestMatch == null || bestMatch.getScore() < o.get().getScore()) {
          bestMatch = o.get();
        }
      }
    }

    return bestMatch;
  }

  @Override
  public String findMatchValue(final String text, final TextMatch textMatch,
      final String validationRegex) {

    String value = null;

    if (textMatch != null) {
      int pos = textMatch.getToken().getEnd();

      if (pos > -1) {

        String subText = text.substring(pos).trim();

        if (!Strings.isEmpty(validationRegex)) {
          value = extractText(subText, validationRegex);
        } else {
          int nextSpace = subText.indexOf(" ");
          if (nextSpace > 0) {
            value = subText.substring(0, nextSpace);
          } else {
            value = subText;
          }
        }
      }
    }

    return value;
  }

  /**
   * Extracts text from the input string based on the provided regex pattern.
   *
   * @param input the input string to search within
   * @param regex the regular expression pattern to match
   * @return a list of extracted text that matches the regex
   */
  private String extractText(final String input, final String regex) {

    String text = null;
    Pattern pattern = Pattern.compile(regex);

    Matcher matcher = pattern.matcher(input);

    if (matcher.find()) {
      text = matcher.group();
    }

    return text;
  }
}
