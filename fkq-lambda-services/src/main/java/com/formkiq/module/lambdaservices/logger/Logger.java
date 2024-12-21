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
 * Logger interface.
 */
public interface Logger {

  /**
   * Trace Log.
   * 
   * @param message {@link String}
   */
  default void trace(final String message) {
    log(LogLevel.TRACE, message);
  }

  /**
   * Debug Log.
   * 
   * @param message {@link String}
   */
  default void debug(final String message) {
    log(LogLevel.DEBUG, message);
  }

  /**
   * Info Log.
   * 
   * @param message {@link String}
   */
  default void info(final String message) {
    log(LogLevel.INFO, message);
  }

  /**
   * Error Log.
   * 
   * @param message {@link String}
   */
  default void error(final String message) {
    log(LogLevel.ERROR, message);
  }

  /**
   * Log.
   * 
   * @param level {@link LogLevel}
   * @param message {@link String}
   */
  void log(LogLevel level, String message);

  /**
   * Is Current {@link LogLevel} going to be logged.
   * 
   * @param level {@link LogLevel}
   * @return boolean
   */
  default boolean isLogged(final LogLevel level) {
    return level.ordinal() >= getCurrentLogLevel().ordinal();
  }

  /**
   * Get Current {@link LogLevel}.
   * 
   * @return {@link LogLevel}
   */
  LogLevel getCurrentLogLevel();
}
