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
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import org.testcontainers.containers.GenericContainer;

/**
 * 
 * Singleton for Test Services.
 *
 */
public final class MinioTestServices {

  /** Minio Image. */
  private static final String MINIO_IMAGE = "minio/minio:RELEASE.2023-08-23T10-07-06Z";
  /** Default Minio Port. */
  private static final Integer DEFAULT_PORT = Integer.valueOf(9000);
  /** Default Minio Console. */
  private static final Integer DEFAULT_CONSOLE_PORT = Integer.valueOf(9090);
  /** Minio Access Key. */
  static final String ACCESS_KEY = "minioadmin";
  /** Minio Secret Key. */
  static final String SECRET_KEY = "minioadmin";
  /** DynamoDB Local {@link GenericContainer}. */
  private static GenericContainer<?> minioContainer;

  /**
   * Get Endpoint.
   * 
   * @return {@link URI}
   */
  @SuppressWarnings("resource")
  public static URI getEndpoint() {
    GenericContainer<?> container = getMinioLocal(null);
    Integer port = container != null ? container.getFirstMappedPort() : DEFAULT_PORT;
    try {
      return new URI("http://127.0.0.1:" + port);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get Singleton Instance of {@link GenericContainer}.
   * 
   * @param webhookEndpoint {@link String}
   * @return {@link GenericContainer}
   */
  @SuppressWarnings("resource")
  public static GenericContainer<?> getMinioLocal(final String webhookEndpoint) {
    if (minioContainer == null && isPortAvailable()) {

      minioContainer =
          new GenericContainer<>(MINIO_IMAGE).withExposedPorts(DEFAULT_PORT, DEFAULT_CONSOLE_PORT)
              .withEnv("MINIO_ROOT_USER", ACCESS_KEY).withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
              .withEnv("MINIO_NOTIFY_WEBHOOK_ENABLE_DOCUMENTS", "on")
              .withEnv("MINIO_NOTIFY_WEBHOOK_ENDPOINT_DOCUMENTS",
                  webhookEndpoint + "/minio/s3/documents")
              .withEnv("MINIO_NOTIFY_WEBHOOK_ENABLE_STAGINGDOCUMENTS", "on")
              .withExtraHost("host.docker.internal", "host-gateway")
              .withEnv("MINIO_NOTIFY_WEBHOOK_ENDPOINT_STAGINGDOCUMENTS",
                  webhookEndpoint + "/minio/s3/stagingdocuments")
              .withCommand("server", "/data", "--console-address", ":9090");
    }

    return minioContainer;
  }

  /**
   * Checks Whether LocalStack is currently running on the default port.
   * 
   * @return boolean
   */
  private static boolean isPortAvailable() {
    boolean available;
    try (ServerSocket ignored = new ServerSocket(DEFAULT_PORT.intValue())) {
      available = true;
    } catch (IOException e) {
      available = false;
    }
    return available;
  }

  private MinioTestServices() {}
}
