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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Recorder for {@link HttpURLConnection}.
 *
 */
public class HttpUrlConnectionRecorder extends HttpURLConnection {

  /** {@link ByteArrayOutputStream}. */
  private ByteArrayOutputStream outputstream = new ByteArrayOutputStream();

  /**
   * constructor.
   * 
   * @param u {@link URL}
   */
  protected HttpUrlConnectionRecorder(final URL u) {
    super(u);
  }

  @Override
  public void connect() throws IOException {
    // do nothing
  }

  /**
   * Tests whether {@link String} or part of {@link String} has been sent.
   * 
   * @param s {@link String}
   * @return boolean
   */
  public boolean contains(final String s) {
    String fullText = new String(this.outputstream.toByteArray(), StandardCharsets.UTF_8);
    return fullText.contains(s);
  }

  @Override
  public void disconnect() {
    // do nothing
  }

  @Override
  public InputStream getInputStream() throws IOException {
    byte[] bytes = "".getBytes(StandardCharsets.UTF_8);
    return new ByteArrayInputStream(bytes);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return this.outputstream;
  }

  @Override
  public int getResponseCode() throws IOException {
    final int status = 200;
    return status;
  }

  @Override
  public boolean usingProxy() {
    return false;
  }
}
