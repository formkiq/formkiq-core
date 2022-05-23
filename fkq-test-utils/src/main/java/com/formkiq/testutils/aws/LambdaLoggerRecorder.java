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
package com.formkiq.testutils.aws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * 
 * Captures Messages from the {@link LambdaLogger}.
 *
 */
public class LambdaLoggerRecorder implements LambdaLogger {

  /** List of {@link String} Messages recorded. */
  private List<String> recordedMessages = new ArrayList<>();
  /** List of byte[] Messages recorded. */
  private List<byte[]> recordedByteMessages = new ArrayList<>();

  @Override
  public void log(final String message) {
    this.recordedMessages.add(message);
  }

  @Override
  public void log(final byte[] message) {
    this.recordedByteMessages.add(message);
  }

  /**
   * Get Recorded {@link String} Messages.
   * 
   * @return {@link List} byte[]
   */
  public List<String> getRecordedMessages() {
    return Collections.unmodifiableList(this.recordedMessages);
  }

  /**
   * Get Recorded Byte[] Messages.
   * 
   * @return {@link List} byte[]
   */
  public List<byte[]> getRecordedByteMessages() {
    return Collections.unmodifiableList(this.recordedByteMessages);
  }

  /**
   * Returns whether a message has been recorded that contains the passed in {@link String}.
   * 
   * @param s {@link String}
   * @return boolean
   */
  public boolean containsString(final String s) {
    return this.recordedMessages.stream().filter(m -> m.contains(s)).findFirst().isPresent();
  }
}
