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
package com.formkiq.stacks.dynamodb.numbering;

import com.formkiq.aws.dynamodb.base64.Pagination;

/** Numbering sequence service. */
public interface NumberingSequenceService {

  /**
   * Find a numbering sequence.
   *
   * @param siteId Site identifier
   * @param attributeKey Attribute key
   * @return numbering sequence, or null
   */
  NumberingSequence find(String siteId, String attributeKey);

  /**
   * Find numbering sequences.
   *
   * @param siteId Site identifier
   * @param nextToken Pagination token
   * @param limit Result limit
   * @return paginated numbering sequences
   */
  Pagination<NumberingSequence> findAll(String siteId, String nextToken, int limit);

  /**
   * Allocate the next value.
   *
   * @param siteId Site identifier
   * @param attributeKey Attribute key
   * @return generated value, or null if the sequence does not exist
   */
  NumberingSequenceValue next(String siteId, String attributeKey);

  /**
   * Parse a previously generated value.
   *
   * @param siteId Site identifier
   * @param attributeKey Attribute key
   * @param value Attribute value
   * @return parsed value, or a value with null sequence state when it does not match
   */
  NumberingSequenceValue parse(String siteId, String attributeKey, String value);

  /**
   * Save a numbering sequence definition.
   *
   * @param siteId Site identifier
   * @param record Definition
   * @return saved numbering sequence
   */
  NumberingSequence save(String siteId, NumberingSequenceRecord record);
}
