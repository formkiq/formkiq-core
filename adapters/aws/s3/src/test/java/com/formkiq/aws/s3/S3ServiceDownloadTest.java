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
package com.formkiq.aws.s3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Verifies downloads stream to disk before the HTTP response is complete. */
class S3ServiceDownloadTest {
  /** Destination directory for downloaded objects. */
  @TempDir
  Path directory;

  @Test
  void downloadsIncrementallyAndReturnsObjectVersion() throws Exception {
    // given
    byte[] payload = new byte[256 * 1024];
    Arrays.fill(payload, (byte) 42);
    final CountDownLatch firstChunk = new CountDownLatch(1);
    final CountDownLatch finishResponse = new CountDownLatch(1);
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      try {
        exchange.getResponseHeaders().add("x-amz-version-id", "downloaded-version");
        exchange.sendResponseHeaders(200, payload.length);
        try (var output = exchange.getResponseBody()) {
          output.write(payload, 0, payload.length / 2);
          output.flush();
          firstChunk.countDown();
          if (!finishResponse.await(10, TimeUnit.SECONDS)) {
            throw new java.io.IOException("Timed out waiting for streaming assertion");
          }
          output.write(payload, payload.length / 2, payload.length / 2);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        exchange.close();
      }
    });
    server.start();
    S3ConnectionBuilder connection = new S3ConnectionBuilder(false).setRegion(Region.US_EAST_1)
        .setCredentials(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .setEndpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
    try (var client = connection.build(); var executor = Executors.newSingleThreadExecutor()) {
      S3Service service = new S3Service(connection);
      Path destination = directory.resolve("definition.cvd");

      // when
      var download =
          executor.submit(() -> service.downloadToFile("bucket", "definition.cvd", destination));
      // then
      try {
        assertTrue(firstChunk.await(5, TimeUnit.SECONDS));
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while ((!Files.exists(destination) || Files.size(destination) == 0)
            && System.nanoTime() < deadline) {
          TimeUnit.MILLISECONDS.sleep(10);
        }
        assertTrue(Files.exists(destination) && Files.size(destination) > 0,
            "File must receive bytes before the HTTP response completes");
      } finally {
        finishResponse.countDown();
      }
      var response = download.get(5, TimeUnit.SECONDS);
      assertEquals("downloaded-version", response.versionId());
      assertArrayEquals(payload, Files.readAllBytes(destination));
    } finally {
      finishResponse.countDown();
      server.stop(0);
    }
  }
}
