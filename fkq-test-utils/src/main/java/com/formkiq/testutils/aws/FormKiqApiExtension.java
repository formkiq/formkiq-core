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

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.integration.ClientAndServer;

/**
 * 
 * JUnit 5 Extension for FormKiQ.
 *
 */
public class FormKiqApiExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  /** {@link AbstractFormKiqApiResponseCallback}. */
  private AbstractFormKiqApiResponseCallback callback;

  /** {@link ClientAndServer}. */
  private ClientAndServer formkiqServer;

  /** Port to run Test server. */
  private int port = -1;
  /** Is server running. */
  private boolean running = false;

  /**
   * constructor.
   * 
   * @param responseCallback {@link AbstractFormKiqApiResponseCallback}
   */
  public FormKiqApiExtension(final AbstractFormKiqApiResponseCallback responseCallback) {
    this.callback = responseCallback;
    this.port = responseCallback.getServerPort();
    this.callback = responseCallback;
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    if (!this.running) {
      this.formkiqServer = startClientAndServer(Integer.valueOf(this.port));

      this.formkiqServer.when(request()).respond(this.callback);
      this.running = true;
    }
  }

  @Override
  public void close() throws Throwable {

    if (this.formkiqServer != null) {
      this.formkiqServer.stop();
    }
    this.formkiqServer = null;
    this.running = false;
  }

  /**
   * Get Base Path.
   * 
   * @return {@link String}
   */
  public String getBasePath() {
    return "http://localhost:" + this.port;
  }

  /**
   * Get callback.
   * 
   * @return {@link AbstractFormKiqApiResponseCallback}
   */
  public AbstractFormKiqApiResponseCallback getCallback() {
    return this.callback;
  }
}
