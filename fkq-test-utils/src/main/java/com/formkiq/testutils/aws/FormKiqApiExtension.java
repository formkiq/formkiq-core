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
import java.util.Random;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;

/**
 * 
 * JUnit 5 Extension for FormKiQ.
 *
 */
public class FormKiqApiExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  /** {@link Random}. */
  private static final Random NUM_RAND = new Random();
  /** {@link ClientAndServer}. */
  private ClientAndServer formkiqServer;

  /** Port to run Test server. */
  private int port = -1;

  /** {@link ExpectationResponseCallback}. */
  private ExpectationResponseCallback responseCallback;

  /**
   * constructor.
   * 
   * @param callback {@link ExpectationResponseCallback}
   */
  public FormKiqApiExtension(final ExpectationResponseCallback callback) {
    this.responseCallback = callback;
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    final int topPort = 8000;
    final int bottomPort = 7000;
    this.port = NUM_RAND.nextInt(topPort - bottomPort) + bottomPort;

    this.formkiqServer = startClientAndServer(Integer.valueOf(this.port));

    this.formkiqServer.when(request()).respond(this.responseCallback);
  }

  @Override
  public void close() throws Throwable {

    if (this.formkiqServer != null) {
      this.formkiqServer.stop();
    }
    this.formkiqServer = null;
  }

  /**
   * Get Base Path.
   * 
   * @return {@link String}
   */
  public String getBasePath() {
    return "http://localhost:" + this.port;
  }
}
