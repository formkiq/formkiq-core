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
package com.formkiq.aws.dynamodb;

import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.base64.MapAttributeValueToString;
import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Represents the result of a DynamoDB query, including the list of items and the key to start the
 * next page of results.
 */
public record QueryResult(List<Map<String, AttributeValue>> items,
    Map<String, AttributeValue> lastEvaluatedKey) {

  /**
   * Convert Last Evelauate Key to {@link String}.
   * 
   * @return {@link String}
   */
  public String toNextToken() {
    return lastEvaluatedKey != null ? new MapAttributeValueToString().apply(lastEvaluatedKey)
        : null;
  }

  /**
   * Builder class for {@link QueryResult}.
   * <p>
   * Provides a fluid API for constructing immutable {@link QueryResult} instances.
   */
  public static class Builder {

    /** {@link List}. */
    private List<Map<String, AttributeValue>> items;
    /** {@link Map}. */
    private Map<String, AttributeValue> lastEvaluatedKey;
    /** Is Query Results truncated. */
    private boolean truncated;

    /** Creates a new, empty builder. */
    public Builder() {}

    /**
     * Set the list of results returned by the query.
     *
     * @param results the results to include in the result
     * @return this builder instance
     */
    public Builder items(final List<Map<String, AttributeValue>> results) {
      return items(results, -1);
    }

    /**
     * Set the list of results returned by the query.
     *
     * @param results the results to include in the result
     * @param limit limit the results.
     * @return this builder instance
     */
    public Builder items(final List<Map<String, AttributeValue>> results, final int limit) {
      this.truncated = results.size() >= limit;
      this.items = limit > 0 && results.size() > limit ? results.subList(0, limit) : results;
      return this;
    }

    /**
     * Set the DynamoDB last evaluated key used for pagination.
     *
     * @param queryLastEvaluatedKey the pagination key map, or {@code null} if no pagination is
     *        required
     * @return this builder instance
     */
    public Builder lastEvaluatedKey(final Map<String, AttributeValue> queryLastEvaluatedKey) {
      this.lastEvaluatedKey = queryLastEvaluatedKey;
      return this;
    }

    /**
     * Build a new {@link QueryResult} instance using the configured builder state.
     *
     * @return a new {@link QueryResult}
     */
    public QueryResult build() {
      return new QueryResult(items, truncated ? lastEvaluatedKey : null);
    }

    /**
     * Set Last Evaluated Key to last item in results.
     * 
     * @param indexName Name of index.
     * @param isShardKey boolean
     */
    public void lastEvaluatedKeyLastItem(final String indexName, final boolean isShardKey) {
      lastEvaluatedKey =
          new DynamodbLastEvaluatedKeyBuilder(Objects.last(items), indexName).build(isShardKey);
    }
  }

  /**
   * Create a new {@link Builder} instance for constructing {@link QueryResult} objects.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }
}
