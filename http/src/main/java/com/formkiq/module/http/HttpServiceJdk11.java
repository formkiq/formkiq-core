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
package com.formkiq.module.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * 
 * {@link HttpService} implementation using JDK11 {@link HttpClient}.
 *
 */
public class HttpServiceJdk11 implements HttpService {

  /** {@link HttpClient}. */
  private HttpClient client;

  /**
   * constructor.
   */
  public HttpServiceJdk11() {
    this.client = HttpClient.newHttpClient();
  }


  /**
   * Build {@link Builder}.
   * 
   * @param url {@link String}
   * @param headers {@link HttpHeaders}
   * @return {@link Builder}
   * @throws IOException IOException
   */
  private Builder build(final String url, final Optional<HttpHeaders> headers) throws IOException {
    Builder builder = HttpRequest.newBuilder().uri(toUri(url));

    if (headers.isPresent()) {
      for (Map.Entry<String, String> e : headers.get().getAll().entrySet()) {
        builder.headers(e.getKey(), e.getValue());
      }
    }

    return builder;
  }

  @Override
  public HttpResponse<String> delete(final String url, final Optional<HttpHeaders> headers)
      throws IOException {
    HttpRequest request = build(url, headers).DELETE().build();
    try {
      return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public HttpResponse<String> get(final String url, final Optional<HttpHeaders> headers)
      throws IOException {
    HttpRequest request = build(url, headers).GET().build();
    try {
      return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }


  @Override
  public HttpResponse<String> patch(final String url, final Optional<HttpHeaders> headers,
      final String payload) throws IOException {

    BodyPublisher body =
        payload != null ? BodyPublishers.ofString(payload) : HttpRequest.BodyPublishers.noBody();

    HttpRequest request = build(url, headers).method("PATCH", body).build();

    try {
      return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }


  @Override
  public HttpResponse<String> post(final String url, final Optional<HttpHeaders> headers,
      final String payload) throws IOException {

    BodyPublisher body =
        payload != null ? BodyPublishers.ofString(payload) : HttpRequest.BodyPublishers.noBody();
    HttpRequest request = build(url, headers).POST(body).build();
    try {
      return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }


  @Override
  public HttpResponse<String> put(final String url, final Optional<HttpHeaders> headers,
      final Path payload) throws IOException {

    HttpRequest request =
        build(url, headers).PUT(HttpRequest.BodyPublishers.ofFile(payload)).build();

    try {
      return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }


  /**
   * Convert {@link String} to {@link URI}.
   * 
   * @param url {@link String}
   * @return {@link URI}
   * @throws IOException IOException
   */
  private URI toUri(final String url) throws IOException {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

}
