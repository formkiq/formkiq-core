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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default {@link Logger} implementation.
 */
public class LoggerImpl implements Logger {

  /** Pattern matching characters that must be escaped in JSON. */
  private static final Pattern SPECIAL_CHARS = Pattern.compile("[\"\\\\\b\f\n\r\t]");

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

  public void log(final LogLevel level, final Throwable e) {
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
            + escapeDoubleQuotes(escapeJsonString(message)) + "\"}");
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
  private String exceptionToString(final Throwable e) {
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
  private String exceptionToJson(final LogLevel logLevel, final Throwable e) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"level\":\"").append(logLevel.name()).append("\",");
    json.append("\"type\":\"").append(e.getClass().getName()).append("\",");
    json.append("\"message\":\"").append(escapeJsonString(e.getMessage())).append("\",");

    // Add stack trace
    json.append("\"stackTrace\":[");
    StackTraceElement[] stackTrace = e.getStackTrace();
    for (int i = 0; i < stackTrace.length; i++) {
      json.append("\"").append(escapeJsonString(stackTrace[i].toString())).append("\"");
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
    return input != null ? input.replace("\"", "\\\"") : null;
  }

  /**
   * Escapes characters in the input string so that it can be used as a JSON string literal.
   *
   * @param input the raw string
   * @return the string with necessary JSON escapes applied
   */
  public static String escapeJsonString(final String input) {
    if (input == null) {
      return "";
    }

    Matcher matcher = SPECIAL_CHARS.matcher(input);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String replacement = switch (matcher.group()) {
        case "\"" -> "\\\\\"";
        case "\\" -> "\\\\\\\\";
        case "\b" -> "\\\\b";
        case "\f" -> "\\\\f";
        case "\n" -> "\\\\n";
        case "\r" -> "\\\\r";
        case "\t" -> "\\\\t";
        default -> matcher.group();
      };
      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);
    return result.toString();
  }

  @Override
  public LogLevel getCurrentLogLevel() {
    return this.currentLogLevel;
  }
}
