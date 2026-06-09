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
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Singleton for ElasticMQ test service.
 */
public final class ElasticMqTestServices {

  /** ElasticMQ Image. */
  private static final String ELASTICMQ_IMAGE = "softwaremill/elasticmq-native:1.7.1";
  /** Default ElasticMQ Port. */
  private static final Integer DEFAULT_PORT = Integer.valueOf(9324);
  /** ElasticMQ {@link GenericContainer}. */
  private static GenericContainer<?> elasticMqContainer;

  /**
   * Get Singleton Instance of {@link GenericContainer}.
   *
   * @return {@link GenericContainer}
   */
  @SuppressWarnings("resource")
  public static GenericContainer<?> getElasticMqLocal() {
    if (elasticMqContainer == null && isPortAvailable()) {
      elasticMqContainer = new GenericContainer<>(ELASTICMQ_IMAGE).withExposedPorts(DEFAULT_PORT)
          .waitingFor(Wait.forListeningPort());
    }

    return elasticMqContainer;
  }

  /**
   * Get Endpoint.
   *
   * @return {@link URI}
   */
  @SuppressWarnings("resource")
  public static URI getEndpoint() {
    GenericContainer<?> container = getElasticMqLocal();
    Integer port = container != null ? container.getFirstMappedPort() : DEFAULT_PORT;
    try {
      return new URI("http://127.0.0.1:" + port);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Checks whether ElasticMQ is already running on the default port.
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

  private ElasticMqTestServices() {}
}
