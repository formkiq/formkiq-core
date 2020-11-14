/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.console;

import java.util.ArrayList;
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
    return this.recordedMessages;
  }

  /**
   * Get Recorded Byte[] Messages.
   * 
   * @return {@link List} byte[]
   */
  public List<byte[]> getRecordedByteMessages() {
    return this.recordedByteMessages;
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
