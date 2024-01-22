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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.TypesenseExtension;

/**
 * JUnit Extension for Netty Server.
 */
public class NettyExtension implements BeforeAllCallback, AfterAllCallback {

  /** API Key. */
  static final String ADMIN_USERNAME = "testapikey";
  /** API Key. */
  static final String ADMIN_PASSWORD = "testapikey";
  /** API Key. */
  static final String API_KEY = "testapikey";
  /** Http Server Port. */
  static final int BASE_HTTP_SERVER_PORT = 8080;
  /** Millisecond Factor. */
  private static final int MILLISECOND_FACTOR = 1000;
  /** {@link Thread}. */
  private Thread serverThread;

  @Override
  public void afterAll(final ExtensionContext context) throws IOException {
    this.serverThread.interrupt();
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    List<String> params = new ArrayList<>();
    params.add("--port=" + BASE_HTTP_SERVER_PORT);
    params.add("--dynamodb-url=" + DynamoDbTestServices.getEndpoint());
    params.add("--s3-url=" + MinioTestServices.getEndpoint());
    params.add("--s3-presigner-url=" + MinioTestServices.getEndpoint());
    params.add("--minio-access-key=" + MinioTestServices.ACCESS_KEY);
    params.add("--minio-secret-key=" + MinioTestServices.SECRET_KEY);
    params.add("--admin-username=" + ADMIN_USERNAME);
    params.add("--admin-password=" + ADMIN_PASSWORD);
    params.add("--api-key=" + API_KEY);
    params.add("--typesense-host=http://localhost:" + TypesenseExtension.getMappedPort());
    params.add("--typesense-api-key=" + TypesenseExtension.API_KEY);

    this.serverThread = new Thread(() -> {
      try {
        String[] args = params.toArray(new String[0]);
        new HttpServer(args).run();
      } catch (ParseException | InterruptedException e) {
        e.printStackTrace();
        // Handle exceptions
      }
    });
    this.serverThread.start();

    final int timeoutInSeconds = 10;
    waitForPortAvailability(BASE_HTTP_SERVER_PORT, timeoutInSeconds);
  }

  private void waitForPortAvailability(final int port, final int timeoutInSeconds)
      throws InterruptedException {
    long startTime = System.currentTimeMillis();
    long endTime = startTime + timeoutInSeconds * MILLISECOND_FACTOR;

    while (System.currentTimeMillis() < endTime) {
      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress("localhost", port), MILLISECOND_FACTOR);
        return;
      } catch (IOException e) {
        // Port is not available yet, wait and retry
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }
}

