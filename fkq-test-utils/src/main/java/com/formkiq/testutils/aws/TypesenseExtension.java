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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 
 * JUnit 5 Extension for TypeSense.
 *
 */
public class TypesenseExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  /** Test API Key. */
  public static final String API_KEY = "Hu1234s212AdxdZ";
  /** Default DynamoDB Port. */
  public static final int DEFAULT_PORT = 8108;
  /** Type Sense Image. */
  private static DockerImageName image = DockerImageName.parse("typesense/typesense:0.25.1");
  /** Mapped Port. */
  private static Integer mappedPort = null;

  /**
   * Get Mapped Port.
   * 
   * @return {@link Integer}
   */
  public static Integer getMappedPort() {
    return mappedPort;
  }

  /** {@link GenericContainer}. */
  private GenericContainer<?> container;

  @SuppressWarnings("resource")
  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    final Integer exposedPort = Integer.valueOf(DEFAULT_PORT);
    this.container = new GenericContainer<>(image)
        .withEnv(Map.of("TYPESENSE_DATA_DIR", "/tmp", "TYPESENSE_API_KEY", API_KEY))
        .withExposedPorts(exposedPort);
    this.container.start();

    final int timeout = 4;
    TimeUnit.SECONDS.sleep(timeout);

    mappedPort = this.container.getFirstMappedPort();
  }

  @Override
  public void close() throws Throwable {
    this.container.close();
  }
}
