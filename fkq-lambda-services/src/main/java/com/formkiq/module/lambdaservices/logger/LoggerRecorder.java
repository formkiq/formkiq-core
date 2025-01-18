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

import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link Logger} recorder.
 */
public class LoggerRecorder implements Logger {

  /** {@link String} messages. */
  private List<String> messages = new ArrayList<>();

  /**
   * constructor.
   */
  public LoggerRecorder() {}

  public void log(final LogLevel level, final String message) {
    if (isLogged(level)) {
      messages.add(message);
      System.out.print(message);
    }
  }

  @Override
  public void log(final LogLevel level, final Exception ex) {
    log(level, ex.getMessage());
  }

  /**
   * Get Messages.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getMessages() {
    return this.messages;
  }

  @Override
  public LogLevel getCurrentLogLevel() {
    return LogLevel.TRACE;
  }

  /**
   * Contains {@link String}.
   * 
   * @param s {@link String}
   * @return boolean
   */
  public boolean containsString(final String s) {
    return this.messages.stream().anyMatch(m -> m.contains(s));
  }
}
