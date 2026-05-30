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

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

/**
 * 
 * JUnit 5 Extension for Minio.
 *
 */
public class MinioExtension
    implements BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {

  /** Minio. */
  private GenericContainer<?> minioLocal;

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    String webhookEndpoint = "http://host.docker.internal:" + NettyExtension.BASE_HTTP_SERVER_PORT;
    this.minioLocal = MinioTestServices.getMinioLocal(webhookEndpoint);

    if (this.minioLocal != null) {
      this.minioLocal.start();
    }
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    // empty
  }

  @Override
  public void close() throws Throwable {

    if (this.minioLocal != null) {
      this.minioLocal.stop();
    }
  }
}
