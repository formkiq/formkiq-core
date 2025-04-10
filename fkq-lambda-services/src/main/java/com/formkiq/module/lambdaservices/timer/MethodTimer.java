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
package com.formkiq.module.lambdaservices.timer;

import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;

/**
 * Method Timer.
 */
public class MethodTimer {

  /** Timer Name. */
  private String timerName;
  /** Start of Timer. */
  private long startTime = -1;
  /** End of Timer. */
  private long endTime = -1;

  /**
   * constructor.
   */
  public MethodTimer() {}

  /**
   * Start Timer.
   *
   * @param name {@link String}
   * @return MethodTimer
   */
  public static MethodTimer startTimer(final String name) {
    return new MethodTimer().start(name);
  }

  /**
   * Start Timer.
   * 
   * @param name {@link String}
   * @return MethodTimer
   */
  public MethodTimer start(final String name) {
    this.timerName = name;
    this.startTime = System.currentTimeMillis();
    return this;
  }

  /**
   * Stop Timer.
   * 
   * @return MethodTimer
   */
  public MethodTimer stop() {
    if (startTime < 0) {
      throw new IllegalStateException("Timer has not been started.");
    }
    endTime = System.currentTimeMillis();
    return this;
  }

  /**
   * Returns the elapsed time in nanoseconds.
   * 
   * @return elapsed time in nanoseconds.
   * @throws IllegalStateException if the timer is still running.
   */
  public long getElapsedTime() {
    if (endTime < 0) {
      throw new IllegalStateException(
          "Timer is still running. Stop it before getting the elapsed time.");
    }
    return endTime - startTime;
  }

  /**
   * Log the Elapsed Time.
   * 
   * @param logger {@link Logger}
   */
  public void logElapsedTime(final Logger logger) {
    if (logger.isLogged(LogLevel.TRACE)) {
      logger.trace(
          String.format("%s: elapsed time in milliseconds: %d", this.timerName, getElapsedTime()));
    }
  }

  /**
   * Measures and prints the execution time of the provided code block.
   *
   * @param logger {@link Logger}
   * @param timerName {@link String}
   * @param task The code to run encapsulated as a Runnable.
   */
  public static void timer(final Logger logger, final String timerName, final Runnable task) {
    if (logger.isLogged(LogLevel.TRACE)) {
      MethodTimer methodTimer = startTimer(timerName);
      task.run();
      methodTimer.stop();
      methodTimer.logElapsedTime(logger);
    }
  }
}
