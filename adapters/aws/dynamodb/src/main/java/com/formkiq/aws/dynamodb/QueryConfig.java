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

import java.util.Map;

/**
 * 
 * DynamoDB Query Config.
 *
 */
public class QueryConfig {

  /** {@link Map}. */
  private Map<String, String> expressionAttributeNames;
  /** Index Name. */
  private String indexName;
  /** Projection Expression. */
  private String projectionExpression;
  /** True - ASC, False - DESC. */
  private Boolean scanIndexForward = Boolean.FALSE;

  /**
   * constructor.
   */
  public QueryConfig() {

  }

  /**
   * Get Expression Attribute Names.
   * 
   * @return {@link Map}
   */
  public Map<String, String> expressionAttributeNames() {
    return this.expressionAttributeNames;
  }

  /**
   * Set Expression Attribute Names.
   * 
   * @param expressionAttributes {@link Map}
   * @return {@link QueryConfig}
   */
  public QueryConfig expressionAttributeNames(final Map<String, String> expressionAttributes) {
    this.expressionAttributeNames = expressionAttributes;
    return this;
  }

  /**
   * Get Index Name.
   * 
   * @return {@link String}
   */
  public String indexName() {
    return this.indexName;
  }

  /**
   * Set Index Name.
   * 
   * @param index {@link String}
   * @return {@link QueryConfig}
   */
  public QueryConfig indexName(final String index) {
    this.indexName = index;
    return this;
  }

  /**
   * Is ScanIndexForward.
   * 
   * @return boolean
   */
  public Boolean isScanIndexForward() {
    return this.scanIndexForward;
  }

  /**
   * Get Projection Expression.
   * 
   * @return {@link String}
   */
  public String projectionExpression() {
    return this.projectionExpression;
  }

  /**
   * Set Projection Expression.
   * 
   * @param projection {@link String}
   * @return {@link QueryConfig}
   */
  public QueryConfig projectionExpression(final String projection) {
    this.projectionExpression = projection;
    return this;
  }

  /**
   * Set Is ScanIndexForward.
   * 
   * @param isScanIndexForward {@link Boolean}
   * @return {@link QueryConfig}
   */
  public QueryConfig scanIndexForward(final Boolean isScanIndexForward) {
    this.scanIndexForward = isScanIndexForward;
    return this;
  }

}
