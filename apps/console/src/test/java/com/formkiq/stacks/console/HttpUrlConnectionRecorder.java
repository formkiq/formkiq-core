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
