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
package com.formkiq.aws.services.lambda;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum of reserved Site IDs.
 */
public enum ReservedSiteId {

  /** Global Site. */
  GLOBAL("GLOBAL"),
  /** API Key. */
  API_KEY("API_KEY");

  /** Reserveed Site id. */
  private final String siteId;

  ReservedSiteId(final String reservedSiteId) {
    this.siteId = reservedSiteId;
  }

  public String getSiteId() {
    return this.siteId;
  }

  /**
   * Convert a string to a {@link ReservedSiteId}.
   *
   * @param value site id string
   * @return Optional of ReservedSiteId if it matches, otherwise empty
   */
  public static Optional<ReservedSiteId> fromString(final String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(v -> v.getSiteId().equalsIgnoreCase(value)).findFirst();
  }
}
