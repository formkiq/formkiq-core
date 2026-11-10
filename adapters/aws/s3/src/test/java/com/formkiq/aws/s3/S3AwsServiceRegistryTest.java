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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Verifies endpoint selection for S3 clients and presigned requests. */
class S3AwsServiceRegistryTest {
  /** Bucket used for all request types. */
  private static final String BUCKET = "formkiq-test-documents";
  /** Existing interface endpoint URL supplied by CloudFormation. */
  private static final URI VPC_ENDPOINT =
      URI.create("https://bucket.vpce-example-servicehash.s3.us-east-2.vpce.amazonaws.com");

  @Test
  void blankEndpointUrlsUseRegionalEndpoints() {
    for (String endpointUrl : new String[] {"", " \t "}) {
      // given
      AwsServiceCache cache = cache(Map.of("S3_ENDPOINT_URL", endpointUrl));
      // when
      initialize(cache, Map.of());
      S3PresignerService presigner =
          new S3PresignerService(cache.getExtension(S3PresignerConnectionBuilder.class));
      URL url = presigner.presignGetUrl(BUCKET, "document", Duration.ofMinutes(5), null,
          new PresignGetUrlConfig());
      try (var client = cache.getExtension(S3ConnectionBuilder.class).build()) {
        // then
        assertTrue(client.serviceClientConfiguration().endpointOverride().isEmpty());
        assertEquals(BUCKET + ".s3.us-east-2.amazonaws.com", url.getHost());
        assertEquals("/document", url.getPath());
      }
    }
  }

  /**
   * Build service configuration with a fixed signing region.
   *
   * @param environment Environment settings
   * @return Service cache
   */
  private AwsServiceCache cache(final Map<String, String> environment) {
    return new AwsServiceCache().environment(environment).region(Region.US_EAST_2);
  }

  @Test
  void defaultsToRegionalEndpoints() {
    // given
    AwsServiceCache cache = cache(Map.of());
    // when
    initialize(cache, Map.of());
    S3PresignerService presigner =
        new S3PresignerService(cache.getExtension(S3PresignerConnectionBuilder.class));
    URL url = presigner.presignGetUrl(BUCKET, "document", Duration.ofMinutes(5), null,
        new PresignGetUrlConfig());
    try (var client = cache.getExtension(S3ConnectionBuilder.class).build()) {
      // then
      assertTrue(client.serviceClientConfiguration().endpointOverride().isEmpty());
      assertEquals(BUCKET + ".s3.us-east-2.amazonaws.com", url.getHost());
      assertEquals("/document", url.getPath());
    }
  }

  /**
   * Initialize the registry with credentials that never require AWS access.
   *
   * @param cache Service cache
   * @param endpoints Explicit endpoint overrides
   */
  private void initialize(final AwsServiceCache cache, final Map<String, URI> endpoints) {
    new S3AwsServiceRegistry().initService(cache, endpoints,
        StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
  }

  @Test
  void preservesExplicitLocalEndpointOverrides() {
    // given
    AwsServiceCache cache = cache(Map.of("S3_ENDPOINT_URL", VPC_ENDPOINT.toString()));
    URI s3Endpoint = URI.create("http://localhost:9000");
    URI presignerEndpoint = URI.create("http://localhost:9001");
    // when
    initialize(cache, Map.of("s3", s3Endpoint, "s3presigner", presignerEndpoint));
    S3PresignerService presigner =
        new S3PresignerService(cache.getExtension(S3PresignerConnectionBuilder.class));
    URL url = presigner.presignGetUrl(BUCKET, "document", Duration.ofMinutes(5), null,
        new PresignGetUrlConfig());
    try (var client = cache.getExtension(S3ConnectionBuilder.class).build()) {
      // then
      assertEquals(s3Endpoint,
          client.serviceClientConfiguration().endpointOverride().orElseThrow());
      assertEquals(presignerEndpoint.getHost(), url.getHost());
      assertEquals(presignerEndpoint.getPort(), url.getPort());
    }
  }

  @Test
  void rejectsMalformedEndpointUrl() {
    // given
    AwsServiceCache cache = cache(Map.of("S3_ENDPOINT_URL", "https://[invalid"));
    // when
    Executable operation = () -> initialize(cache, Map.of());
    // then
    assertThrows(IllegalArgumentException.class, operation);
  }

  @Test
  void vpcClientSendsPathStyleS3Requests() throws Exception {
    // given
    AtomicReference<String> requestedPath = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      requestedPath.set(exchange.getRequestURI().getPath());
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.start();
    AwsServiceCache cache =
        cache(Map.of("S3_ENDPOINT_URL", "http://localhost:" + server.getAddress().getPort()));
    try {
      // when
      initialize(cache, Map.of());
      try (var client = cache.getExtension(S3ConnectionBuilder.class).build()) {
        var response = client.headObject(request -> request.bucket(BUCKET).key("site/document"));
        // then
        assertTrue(response.sdkHttpResponse().isSuccessful());
        assertEquals("/" + BUCKET + "/site/document", requestedPath.get());
      }
    } finally {
      server.stop(0);
    }
  }

  @Test
  void vpcModeUsesTheEndpointForClientAndPresignedGetAndPut() {
    // given
    AwsServiceCache cache = cache(Map.of("S3_ENDPOINT_URL", VPC_ENDPOINT.toString()));
    // when
    initialize(cache, Map.of());
    S3PresignerService presigner =
        new S3PresignerService(cache.getExtension(S3PresignerConnectionBuilder.class));
    URL get = presigner.presignGetUrl(BUCKET, "site/document", Duration.ofMinutes(5), "version-1",
        new PresignGetUrlConfig());
    URL put = presigner.presignPutUrl(BUCKET, "site/document", Duration.ofMinutes(5),
        new PresignPutUrlConfig(null, null, Optional.empty(), Map.of(), false));
    try (var client = cache.getExtension(S3ConnectionBuilder.class).build()) {
      // then
      assertEquals(VPC_ENDPOINT,
          client.serviceClientConfiguration().endpointOverride().orElseThrow());
      assertEquals(VPC_ENDPOINT.getHost(), get.getHost());
      assertEquals(VPC_ENDPOINT.getHost(), put.getHost());
      assertEquals("/" + BUCKET + "/site/document", get.getPath());
      assertEquals(get.getPath(), put.getPath());
      assertTrue(get.getQuery().contains("versionId=version-1"));
      assertTrue(get.getQuery().contains("X-Amz-Signature="));
      assertTrue(put.getQuery().contains("X-Amz-Signature="));
    }
  }
}
