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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link Logger}.
 */
public class LoggerTest {

  /** {@link LoggerImpl}. */
  private LoggerImpl logger;
  /** {@link ByteArrayOutputStream}. */
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  public void setUp() {
    // Set up a LoggerImpl with INFO level and JSON log type
    logger = new LoggerImpl(LogLevel.INFO, LogType.JSON);

    // Redirect system output for testing
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
  }

  /**
   * Is Logged test.
   */
  @Test
  void testIsLogged() {
    assertTrue(new LoggerImpl(LogLevel.ERROR, LogType.JSON).isLogged(LogLevel.ERROR));
    assertFalse(new LoggerImpl(LogLevel.ERROR, LogType.JSON).isLogged(LogLevel.INFO));
    assertFalse(new LoggerImpl(LogLevel.DEBUG, LogType.JSON).isLogged(LogLevel.TRACE));
    assertFalse(new LoggerImpl(LogLevel.INFO, LogType.JSON).isLogged(LogLevel.TRACE));
    assertTrue(new LoggerImpl(LogLevel.INFO, LogType.JSON).isLogged(LogLevel.INFO));
    assertTrue(new LoggerImpl(LogLevel.INFO, LogType.JSON).isLogged(LogLevel.ERROR));
    assertTrue(new LoggerImpl(LogLevel.DEBUG, LogType.JSON).isLogged(LogLevel.INFO));
  }

  @Test
  public void testLogMessageInJsonFormat() {
    // Log a message with INFO level
    String message = "This is a test message.";
    logger.log(LogLevel.INFO, message);

    // Verify the output
    String expectedOutput = "{\"level\":\"INFO\",\"message\":\"This is a test message.\"}\n";
    assertEquals(expectedOutput, outputStream.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void testLogMessageInPlainTextFormat() {
    // Set up a LoggerImpl with plain text log type
    logger = new LoggerImpl(LogLevel.INFO, LogType.TEXT);

    // Log a message with INFO level
    String message = "This is a test message.";
    logger.log(LogLevel.INFO, message);

    // Verify the output
    String expectedOutput = "This is a test message.\n";
    assertEquals(expectedOutput, outputStream.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void testLogExceptionInJsonFormat() {
    // Log an exception with WARN level
    Exception exception = new NullPointerException("Test exception");
    logger.log(LogLevel.ERROR, exception);

    // Verify the output contains JSON representation
    String output = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("\"level\":\"ERROR\""));
    assertTrue(output.contains("\"type\":\"java.lang.NullPointerException\""));
    assertTrue(output.contains("\"message\":\"Test exception\""));
    assertTrue(output.contains("\"stackTrace\":"));
  }

  @Test
  public void testLogExceptionInPlainTextFormat() {
    // Set up a LoggerImpl with plain text log type
    logger = new LoggerImpl(LogLevel.ERROR, LogType.TEXT);

    // Log an exception with WARN level
    Exception exception = new NullPointerException("Test exception");
    logger.log(LogLevel.ERROR, exception);

    // Verify the output contains plain stack trace
    String output = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("java.lang.NullPointerException: Test exception"));
    assertTrue(output.contains("at com.formkiq.module.lambdaservices.logger.LoggerTest."
        + "testLogExceptionInPlainTextFormat"));
  }

  @Test
  public void testGetCurrentLogLevel() {
    // Verify the current log level
    assertEquals(LogLevel.INFO, logger.getCurrentLogLevel());
  }

  @Test
  public void testLogMessageBelowLogLevel() {
    // Log a message below the current log level
    String message = "This message should not be logged.";
    logger.log(LogLevel.DEBUG, message);

    // Verify that nothing is logged
    assertEquals("", outputStream.toString(StandardCharsets.UTF_8));
  }
}
