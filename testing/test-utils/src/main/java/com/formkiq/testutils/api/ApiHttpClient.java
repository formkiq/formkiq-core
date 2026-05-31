package com.formkiq.testutils.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Send API requests using {@link HttpClient}.
 */
public class ApiHttpClient {

  /**
   * Send API request.
   * @param group {@link String}
   * @param basePath {@link String}
   * @param method {@link String}
   * @param body {@link String}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  public static HttpResponse<String> send(final String group, final String basePath, final String method, final String body)
      throws IOException, InterruptedException {

    String jwt = JwtTokenEncoder.encodeCognito(new String[] {group}, "joesmith");

    try (HttpClient http = HttpClient.newHttpClient()) {
      HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(basePath))
          .method(method, body != null
              ? HttpRequest.BodyPublishers.ofString(body)
              : HttpRequest.BodyPublishers.noBody()).header("Authorization", jwt);
      return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
  }
}
