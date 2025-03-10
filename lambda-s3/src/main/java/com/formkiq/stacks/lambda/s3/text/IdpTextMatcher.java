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
import com.formkiq.strings.StringFormatter;
import com.formkiq.strings.lexer.Token;
import com.formkiq.strings.lexer.TokenGenerator;

import java.util.ArrayList;
import java.util.Iterator;
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
      final TokenGenerator tokenGenerator, final TextMatchAlgorithm matchAlgorithm,
      final String splitRegex, final StringFormatter formatter) {

    TextMatch bestMatch = null;
    List<Token> tokens = tokenGenerator.generateTokens(text);

    for (String m : matches) {

      String match = formatter.format(m);
      int groupSize = splitRegex != null ? match.split(splitRegex).length : 1;

      List<Token> groupTokens = groupTokens(tokens, groupSize);

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

  /**
   * Group Tokens to a certain size.
   *
   * @param tokens {@link List} {@link Token}
   * @param tokenGroupSize int
   * @return {@link List} {@link Token}
   */
  private List<Token> groupTokens(final List<Token> tokens, final int tokenGroupSize) {

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
