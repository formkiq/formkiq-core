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
package com.formkiq.module.lambdaservices.logger;

/**
 * Log Level.
 */
public enum LogLevel {
  /** Trace. */
  TRACE(1),
  /** Debug. */
  DEBUG(3),
  /** Info. */
  INFO(5),
  /** Error. */
  ERROR(8);

  /**
   * Convert {@link String} to {@link LogLevel}.
   * 
   * @param logLevel {@link String}
   * @return {@link LogLevel}
   */
  public static LogLevel fromString(final String logLevel) {
    try {
      return valueOf(logLevel.toUpperCase());
    } catch (Exception e) {
      return LogLevel.INFO;
    }
  }

  /** Log Level. */
  private final int level;

  /**
   * constructor.
   * 
   * @param logLevel int
   */
  LogLevel(final int logLevel) {
    this.level = logLevel;
  }

  /**
   * Get Level.
   * 
   * @return int
   */
  public int getLevel() {
    return this.level;
  }
}
