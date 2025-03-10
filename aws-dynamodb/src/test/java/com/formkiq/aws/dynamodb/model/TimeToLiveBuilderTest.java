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
package com.formkiq.aws.dynamodb.model;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link TimeToLiveBuilder}.
 */
class TimeToLiveBuilderTest {

  @Test
  void testWithExpirationTime() {
    Instant expirationTime = Instant.parse("2025-01-31T23:59:59Z");
    long expectedTtl = expirationTime.getEpochSecond();

    long ttl = new TimeToLiveBuilder().withExpirationTime(expirationTime).build();

    assertEquals(expectedTtl, ttl, "TTL should match the specified expiration time.");
  }

  @Test
  void testWithDurationFromNow() {
    final long durationInSeconds = 3600;
    long now = Instant.now().getEpochSecond();

    long ttl = new TimeToLiveBuilder().withDurationFromNow(durationInSeconds).build();

    assertTrue(ttl >= now + durationInSeconds, "TTL should be at least the duration from now.");
  }

  @Test
  void testWithDaysFromNow() {
    final long days = 7;
    long now = Instant.now().getEpochSecond();
    final long expectedDurationInSeconds = days * 24 * 60 * 60;

    long ttl = new TimeToLiveBuilder().withDaysFromNow(days).build();

    assertTrue(ttl >= now + expectedDurationInSeconds,
        "TTL should be at least the number of days from now.");
  }

  @Test
  void testBuildAttributeValue() {
    final long days = 3;
    long now = Instant.now().getEpochSecond();
    final long expectedDurationInSeconds = days * 24 * 60 * 60;

    AttributeValue ttlAttribute =
        new TimeToLiveBuilder().withDaysFromNow(days).buildAttributeValue();

    long ttl = Long.parseLong(ttlAttribute.n());
    assertTrue(ttl >= now + expectedDurationInSeconds,
        "TTL AttributeValue should match the calculated TTL.");
  }

  @Test
  void testMissingExpirationThrowsException() {
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> new TimeToLiveBuilder().build());

    assertEquals("Expiration time must be set using withExpirationTime() or withDurationFromNow().",
        exception.getMessage(), "Expected an exception when expiration time is not set.");
  }

  @Test
  void testNegativeDurationThrowsException() {
    final int durationInSeconds = -10;
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new TimeToLiveBuilder().withDurationFromNow(durationInSeconds));

    assertEquals("Duration must be greater than 0 seconds", exception.getMessage(),
        "Expected an exception when duration is negative.");
  }

  @Test
  void testNegativeDaysThrowsException() {
    final int durationInSeconds = -5;
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new TimeToLiveBuilder().withDaysFromNow(durationInSeconds));

    assertEquals("Days must be greater than 0", exception.getMessage(),
        "Expected an exception when days is negative.");
  }
}
