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
package com.formkiq.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Input bounds and report metadata safety. */
class BenchmarkConfigTest {

  @Test
  void normalizesEndpointAndDoesNotCopyArbitraryProperties() {
    // given
    var properties = new Properties();
    properties.setProperty("benchmarkUrl", "https://example.com/api/");
    properties.setProperty("benchmarkScenarios", "version, VERSION");
    properties.setProperty("secret", "do-not-report");

    // when
    var config = BenchmarkConfig.from(properties);

    // then
    assertEquals("https://example.com/api", config.endpoint().toString());
    assertEquals(1, config.scenarios().size());
    assertFalse(config.metadata().toString().contains("do-not-report"));
  }

  @Test
  void rejectsCredentialsInEndpoint() {
    // given
    var properties = new Properties();
    properties.setProperty("benchmarkUrl", "https://user:password@example.com/api");

    // when
    Executable operation = () -> BenchmarkConfig.from(properties);

    // then
    assertThrows(IllegalArgumentException.class, operation);
  }

  @Test
  void rejectsUnboundedLoad() {
    // given
    var properties = new Properties();
    properties.setProperty("benchmarkUrl", "https://example.com");
    properties.setProperty("benchmarkRequests", "100000");

    // when
    Executable operation = () -> BenchmarkConfig.from(properties);

    // then
    assertThrows(IllegalArgumentException.class, operation);
  }
}
