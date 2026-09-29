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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;

/** Tests real SDK endpoint selection without AWS network calls. */
class S3TransferAccelerationTest {
  /** Documents bucket. */
  private static final String BUCKET = "formkiq-test-documents";

  @Test
  void acceleratedGetPreservesOptionsAndRegularDefault() {
    // given
    S3PresignerService service = new S3PresignerService(builder());
    PresignGetUrlConfig config = new PresignGetUrlConfig().accelerate(true)
        .contentType("application/pdf").contentDispositionByPath("report.pdf", false);
    // when
    URL accelerated = service.presignGetUrl(BUCKET, "site/document/artifact", Duration.ofMinutes(5),
        "version-123", config);
    URL regular = service.presignGetUrl(BUCKET, "site/document/artifact", Duration.ofMinutes(5),
        "version-123", new PresignGetUrlConfig());
    // then
    assertEquals(BUCKET + ".s3-accelerate.amazonaws.com", accelerated.getHost());
    assertFalse(regular.getHost().contains("s3-accelerate"));
    assertEquals("/site/document/artifact", accelerated.getPath());
    String query = URLDecoder.decode(accelerated.getQuery(), StandardCharsets.UTF_8);
    assertTrue(query.contains("versionId=version-123"));
    assertTrue(query.contains("X-Amz-Expires=300"));
    assertTrue(query.contains("response-content-type=application/pdf"));
    assertTrue(query.contains("attachment; filename*=UTF-8''report.pdf"));
    assertTrue(query.contains("X-Amz-Signature="));
    assertTrue(query.contains("X-Amz-SignedHeaders=host&"));
  }

  @Test
  void acceleratedPutPreservesChecksumAndMetadata() {
    // given
    S3PresignerService service = new S3PresignerService(builder());
    String checksum = "00".repeat(32);
    PresignPutUrlConfig config = new PresignPutUrlConfig(ChecksumAlgorithm.SHA256, checksum,
        Optional.of(100L), Map.of("x-amz-meta-example", "value"), true);
    // when
    URL url = service.presignPutUrl(BUCKET, "document", Duration.ofMinutes(5), config);
    URL regular = service.presignPutUrl(BUCKET, "document", Duration.ofMinutes(5),
        ChecksumAlgorithm.SHA256, checksum, Optional.of(100L), null);
    // then
    assertEquals(BUCKET + ".s3-accelerate.amazonaws.com", url.getHost());
    assertFalse(regular.getHost().contains("s3-accelerate"));
    String query = URLDecoder.decode(url.getQuery(), StandardCharsets.UTF_8);
    assertTrue(query.contains("x-amz-meta-example=value"));
    assertTrue(query.contains("content-length"));
    assertTrue(query.contains("x-amz-checksum-sha256"));
  }

  private S3PresignerConnectionBuilder builder() {
    return new S3PresignerConnectionBuilder().setRegion(Region.US_EAST_1)
        .setCredentials(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .acceleratedBucket(BUCKET);
  }

  @Test
  void concurrentRequestsKeepTheirEndpointSelection() throws Exception {
    // given
    S3PresignerService service = new S3PresignerService(builder());
    final int workers = 8;
    CyclicBarrier barrier = new CyclicBarrier(workers);
    List<Future<URL>> results = new ArrayList<>();
    try (var executor = Executors.newFixedThreadPool(workers)) {
      // when
      for (int i = 0; i < workers; i++) {
        final boolean accelerate = i % 2 == 0;
        results.add(executor.submit(() -> {
          barrier.await(10, TimeUnit.SECONDS);
          return service.presignGetUrl(BUCKET, "key", Duration.ofMinutes(1), null,
              new PresignGetUrlConfig().accelerate(accelerate));
        }));
      }
      // then
      for (int i = 0; i < workers; i++) {
        assertEquals(i % 2 == 0,
            results.get(i).get(10, TimeUnit.SECONDS).getHost().contains("s3-accelerate"));
      }
    }
  }

  @Test
  void pathStyleFalseIsHonored() {
    // given
    S3PresignerConnectionBuilder connection = builder().pathStyleAccessEnabled(true);
    S3PresignerService service = new S3PresignerService(connection);
    // when
    URL pathStyle = service.presignGetUrl(BUCKET, "key", Duration.ofMinutes(1), null,
        new PresignGetUrlConfig());
    connection.pathStyleAccessEnabled(false);
    URL accelerated = service.presignGetUrl(BUCKET, "key", Duration.ofMinutes(1), null,
        new PresignGetUrlConfig().accelerate(true));
    // then
    assertEquals("/" + BUCKET + "/key", pathStyle.getPath());
    assertEquals(BUCKET + ".s3-accelerate.amazonaws.com", accelerated.getHost());
  }

  @Test
  void rejectsDisabledAndOtherBuckets() {
    // given
    S3PresignerService enabled = new S3PresignerService(builder());
    S3PresignerService disabled = new S3PresignerService(builder().acceleratedBucket(null));
    PresignGetUrlConfig config = new PresignGetUrlConfig().accelerate(true);
    for (String bucket : List.of("formkiq-test-staging", "formkiq-test-ocr", "external-bucket")) {
      // when
      Executable request =
          () -> enabled.presignGetUrl(bucket, "key", Duration.ofMinutes(1), null, config);
      // then
      assertThrows(IllegalArgumentException.class, request);
    }
    // when
    Executable request =
        () -> disabled.presignGetUrl(BUCKET, "key", Duration.ofMinutes(1), null, config);
    // then
    assertThrows(IllegalArgumentException.class, request);
  }

  @Test
  void rejectsIncompatibleConfiguration() {
    // given
    for (S3PresignerConnectionBuilder connection : List.of(builder().pathStyleAccessEnabled(true),
        builder().setEndpointOverride(URI.create("http://localhost:4566")),
        builder().acceleratedBucket("bucket.with.dots"))) {
      // when
      Executable build = () -> connection.build(true);
      // then
      assertThrows(IllegalArgumentException.class, build);
    }
  }
}
