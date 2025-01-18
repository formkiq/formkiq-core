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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Default {@link Logger} implementation.
 */
public class LoggerImpl implements Logger {

  /** Current Log Level. */
  private final LogLevel currentLogLevel;
  /** {@link LogType}. */
  private final LogType currentLogType;

  /**
   * constructor.
   *
   * @param logLevel {@link LogLevel}
   * @param logType {@link LogType}
   */
  public LoggerImpl(final LogLevel logLevel, final LogType logType) {
    this.currentLogLevel = logLevel;
    this.currentLogType = logType;
  }

  public void log(final LogLevel level, final Exception e) {
    if (isLogged(level)) {
      if (LogType.JSON.equals(this.currentLogType)) {
        System.out.printf("%s%n", exceptionToJson(level, e));
      } else {
        System.out.printf("%s%n", exceptionToString(e));
      }
    }
  }

  public void log(final LogLevel level, final String message) {
    if (isLogged(level)) {
      if (LogType.JSON.equals(this.currentLogType)) {
        System.out.printf("%s%n", "{\"level\":\"" + level.name() + "\",\"message\":\""
            + escapeDoubleQuotes(message) + "\"}");
      } else {
        System.out.printf("%s%n", message);
      }
    }
  }

  /**
   * Convert {@link Exception} to {@link String}.
   *
   * @param e {@link Exception}
   * @return {@link String}
   */
  private String exceptionToString(final Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Converts an exception into a JSON string representation.
   *
   * @param logLevel {@link LogLevel}
   * @param e The exception to convert.
   * @return A JSON string representing the exception.
   */
  private String exceptionToJson(final LogLevel logLevel, final Exception e) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"level\":\"").append(logLevel.name()).append("\",");
    json.append("\"type\":\"").append(e.getClass().getName()).append("\",");
    json.append("\"message\":\"").append(e.getMessage()).append("\",");

    // Add stack trace
    json.append("\"stackTrace\":[");
    StackTraceElement[] stackTrace = e.getStackTrace();
    for (int i = 0; i < stackTrace.length; i++) {
      json.append("\"").append(stackTrace[i].toString().replace("\"", "\\\"")).append("\"");
      if (i < stackTrace.length - 1) {
        json.append(",");
      }
    }
    json.append("]");
    json.append("}");
    return json.toString();
  }

  /**
   * Escapes all double-quote characters in the input string.
   *
   * @param input The string to escape.
   * @return The string with double-quote characters escaped
   */
  private String escapeDoubleQuotes(final String input) {
    return input.replace("\"", "\\\"");
  }

  @Override
  public LogLevel getCurrentLogLevel() {
    return this.currentLogLevel;
  }
}
