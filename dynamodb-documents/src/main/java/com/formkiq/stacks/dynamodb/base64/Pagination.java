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
package com.formkiq.stacks.dynamodb.base64;

import com.formkiq.aws.dynamodb.base64.MapToBase64;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pagination Results for a DynamoDB Query.
 *
 * @param <T> Type of Results.
 */
public class Pagination<T> {
  /** {@link List}. */
  private final List<T> results;
  /** Next Token. */
  private final String nextToken;

  /**
   * constructor.
   *
   * @param list {@link List}
   * @param lastEvaluatedKey {@link Map}
   */
  public Pagination(final List<T> list, final Map<String, AttributeValue> lastEvaluatedKey) {
    this.results = list;

    Map<String, String> map = lastEvaluatedKey.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s()));
    this.nextToken = new MapToBase64().apply(map);
  }

  /**
   * Get Results.
   * 
   * @return List
   */
  public List<T> getResults() {
    return this.results;
  }

  /**
   * Get Next Token.
   * 
   * @return String
   */
  public String getNextToken() {
    return this.nextToken;
  }
}
