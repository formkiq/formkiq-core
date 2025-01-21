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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Builds Dynamodb Time to live value.
 */
public class TimeToLiveBuilder {

  /** Current Ttl Time. */
  private Instant time;

  /**
   * constructor.
   */
  public TimeToLiveBuilder() {}

  /**
   * Sets a specific expiration expiryTime.
   *
   * @param expiryTime Instant representing the expiration expiryTime.
   * @return TimeToLiveBuilder instance.
   */
  public TimeToLiveBuilder withExpirationTime(final Instant expiryTime) {
    this.time = Objects.requireNonNull(expiryTime, "Expiration expiryTime cannot be null.");
    return this;
  }


  /**
   * Sets the TTL based on a duration from now.
   *
   * @param durationInSeconds Number of seconds from now until expiration.
   * @return TimeToLiveBuilder instance.
   */
  public TimeToLiveBuilder withDurationFromNow(final long durationInSeconds) {
    if (durationInSeconds <= 0) {
      throw new IllegalArgumentException("Duration must be greater than 0 seconds");
    }
    this.time = Instant.now().plusSeconds(durationInSeconds);
    return this;
  }

  /**
   * Sets the TTL based on a number of days from now.
   *
   * @param days Number of days from now until expiration.
   * @return TimeToLiveBuilder instance.
   */
  public TimeToLiveBuilder withDaysFromNow(final long days) {
    if (days <= 0) {
      throw new IllegalArgumentException("Days must be greater than 0");
    }
    this.time = Instant.now().plus(days, ChronoUnit.DAYS);
    return this;
  }

  /**
   * Builds the TTL value as a Unix timestamp (in seconds).
   *
   * @return The Unix epoch timestamp for TTL.
   */
  public long build() {
    if (time == null) {
      throw new IllegalStateException(
          "Expiration time must be set using withExpirationTime() or withDurationFromNow().");
    }
    return time.getEpochSecond();
  }

  /**
   * Builds the TTL as a DynamoDB AttributeValue.
   *
   * @return AttributeValue representing the TTL.
   */
  public AttributeValue buildAttributeValue() {
    long ttl = build();
    return AttributeValue.builder().n(String.valueOf(ttl)) // DynamoDB stores numbers as strings
        .build();
  }
}
