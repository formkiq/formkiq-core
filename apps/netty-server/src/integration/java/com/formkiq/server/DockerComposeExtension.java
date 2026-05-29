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
package com.formkiq.server;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ComposeContainer;

/**
 * JUnit Extension for Netty Server.
 */
public class DockerComposeExtension implements BeforeAllCallback, AfterAllCallback {

  /** Millisecond Factor. */
  private static final int MILLISECOND_FACTOR = 1000;
  /** Http Server Port. */
  static final int BASE_HTTP_SERVER_PORT = 8080;
  /** {@link ComposeContainer}. */
  private static ComposeContainer environment;

  @Override
  public void afterAll(final ExtensionContext context) {
    if (environment != null) {
      environment.stop();
    }
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    environment = new ComposeContainer(new File("docker-compose.yml"),
        new File("docker-compose-integration-override.yml"));
    environment.start();

    final int timeoutInSeconds = 30;
    waitForHttpServer(timeoutInSeconds);
  }

  private boolean isHttpServerReady() {
    try (Socket socket = new Socket()) {
      socket.connect(
          new InetSocketAddress("localhost", DockerComposeExtension.BASE_HTTP_SERVER_PORT),
          MILLISECOND_FACTOR);
    } catch (IOException e) {
      return false;
    }

    try {
      HttpURLConnection connection = (HttpURLConnection) URI
          .create("http://localhost:" + DockerComposeExtension.BASE_HTTP_SERVER_PORT + "/").toURL()
          .openConnection();
      connection.setConnectTimeout(MILLISECOND_FACTOR);
      connection.setReadTimeout(MILLISECOND_FACTOR);
      connection.connect();

      return connection.getResponseCode() > 0;
    } catch (IOException e) {
      return false;
    }
  }

  private void waitForHttpServer(final int timeoutInSeconds)
      throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    long endTime = startTime + (long) timeoutInSeconds * MILLISECOND_FACTOR;

    while (System.currentTimeMillis() < endTime) {
      if (isHttpServerReady()) {
        return;
      }

      TimeUnit.SECONDS.sleep(1);
    }

    throw new IOException(
        "Timed out waiting for port " + DockerComposeExtension.BASE_HTTP_SERVER_PORT);
  }
}
