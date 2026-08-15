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

import java.util.Objects;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;

/** Fluent builder for {@link NumberingSequenceRecord}. */
public class NumberingSequenceRecordBuilder
    implements DynamoDbEntityBuilder<NumberingSequenceRecord> {

  /** Attribute key. */
  private String attributeKey;
  /** Number format pattern. */
  private String pattern;
  /** Initial sequence number. */
  private Long startAt;
  /** Sequence padding. */
  private Integer padding;
  /** Reset frequency. */
  private NumberingSequenceReset reset;
  /** Period timezone. */
  private String timezone;

  /** Constructor. */
  public NumberingSequenceRecordBuilder() {}

  /**
   * Sets the attribute key.
   *
   * @param value Attribute key
   * @return this builder
   */
  public NumberingSequenceRecordBuilder attributeKey(final String value) {
    this.attributeKey = value;
    return this;
  }

  @Override
  public NumberingSequenceRecord build(final DynamoDbKey key) {
    return new NumberingSequenceRecord(key, this.attributeKey, this.pattern, this.startAt,
        this.padding, this.reset, this.timezone);
  }

  @Override
  public NumberingSequenceRecord build(final String siteId) {
    return build(buildKey(siteId));
  }

  @Override
  public DynamoDbKey buildKey(final String siteId) {
    Objects.requireNonNull(this.attributeKey, "attributeKey must not be null");
    return DynamoDbKey.builder().pk(siteId, NumberingSequenceRecord.PK_PREFIX)
        .sk(NumberingSequenceRecord.SK_PREFIX + this.attributeKey).build();
  }

  /**
   * Sets the sequence padding.
   *
   * @param value Padding
   * @return this builder
   */
  public NumberingSequenceRecordBuilder padding(final Integer value) {
    this.padding = value;
    return this;
  }

  /**
   * Sets the number format pattern.
   *
   * @param value Pattern
   * @return this builder
   */
  public NumberingSequenceRecordBuilder pattern(final String value) {
    this.pattern = value;
    return this;
  }

  /**
   * Sets the reset frequency.
   *
   * @param value Reset frequency
   * @return this builder
   */
  public NumberingSequenceRecordBuilder reset(final NumberingSequenceReset value) {
    this.reset = value;
    return this;
  }

  /**
   * Sets the initial sequence number.
   *
   * @param value Initial sequence number
   * @return this builder
   */
  public NumberingSequenceRecordBuilder startAt(final Long value) {
    this.startAt = value;
    return this;
  }

  /**
   * Sets the timezone.
   *
   * @param value IANA timezone
   * @return this builder
   */
  public NumberingSequenceRecordBuilder timezone(final String value) {
    this.timezone = value;
    return this;
  }
}
