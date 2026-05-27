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
package com.formkiq.aws.dynamodb.builder;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link DynamoDbAttributeMapBuilder}.
 */
public class DynamoDbAttributeMapBuilderTest {

  private static void assertBetween(final DynamoDbAttributeMapBuilder builder, final Instant now,
      final long offsetInSeconds) {
    long value = Long.parseLong(builder.build().get("TimeToLive").n());
    long minInclusive = now.getEpochSecond() + offsetInSeconds;
    long maxInclusive = minInclusive + 1;
    assertTrue(value >= minInclusive && value <= maxInclusive,
        () -> "Expected " + value + " to be between [" + minInclusive + ", " + maxInclusive + "]");
  }

  @Test
  void timeToLiveInDays() {
    // given
    Instant now = Instant.now();
    final long days = 3;
    final int secondsPerDays = 259200;

    // when
    DynamoDbAttributeMapBuilder builder =
        DynamoDbAttributeMapBuilder.builder().withTimeToLiveInDays(days);

    // then
    assertBetween(builder, now, secondsPerDays);
  }

  @Test
  void timeToLiveInHours() {
    // given
    Instant now = Instant.now();
    final long hours = 2L;
    final int secondsPerHours = 7200;

    // when
    DynamoDbAttributeMapBuilder builder =
        DynamoDbAttributeMapBuilder.builder().withTimeToLiveInHours(hours);

    // then
    assertBetween(builder, now, secondsPerHours);
  }

  @Test
  void timeToLiveInSeconds() {
    // given
    Instant now = Instant.now();
    final long ttlSeconds = 90L;

    // when
    DynamoDbAttributeMapBuilder builder =
        DynamoDbAttributeMapBuilder.builder().withTimeToLiveInSeconds(ttlSeconds);

    // then
    assertBetween(builder, now, ttlSeconds);
  }
}
