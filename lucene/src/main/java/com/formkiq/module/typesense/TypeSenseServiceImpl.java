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
package com.formkiq.module.typesense;

import static com.formkiq.module.http.HttpResponseStatus.is2XX;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.module.http.JsonService;
import com.formkiq.module.http.JsonServiceGson;

/**
 * 
 * Implementation of {@link TypeSenseService}.
 *
 */
public class TypeSenseServiceImpl implements TypeSenseService {

  /** {@link String}. */
  private String apiKey;
  /** {@link String}. */
  private String host;
  /** {@link JsonService}. */
  private JsonService json = new JsonServiceGson();
  /** {@link HttpService}. */
  private HttpService service;

  /**
   * constructor.
   * 
   * @param hostAddress {@link String}
   * @param typeSenseApiKey {@link String}
   */
  public TypeSenseServiceImpl(final String hostAddress, final String typeSenseApiKey) {
    if (hostAddress == null || typeSenseApiKey == null) {
      throw new IllegalArgumentException();
    }

    this.service = new HttpServiceJdk11();
    this.host = hostAddress;
    this.apiKey = typeSenseApiKey;
  }

  @Override
  public HttpResponse<String> addCollection(final String siteId) throws IOException {

    String site = getCollectionName(siteId);
    String url = String.format("%s/collections", this.host);

    String payload = "{\"name\":\"" + site + "\",\"fields\":[{\"name\":\".*\",\"type\":\"auto\"}]}";

    HttpHeaders headers = getHeader();

    HttpResponse<String> response =
        this.service.post(url, headers, BodyPublishers.ofString(payload));

    return response;
  }

  @Override
  public HttpResponse<String> addDocument(final String siteId, final String documentId,
      final Map<String, Object> data) throws IOException {

    Map<String, Object> payload = new HashMap<>(data);
    payload.put("id", documentId);
    payload.remove("documentId");

    String site = getCollectionName(siteId);

    String url =
        String.format("%s/collections/%s/documents", this.host, encode(getCollectionName(site)));

    HttpHeaders headers = getHeader();

    HttpResponse<String> response =
        this.service.post(url, headers, BodyPublishers.ofString(this.json.toJson(payload)));

    return response;
  }

  @Override
  public HttpResponse<String> deleteDocument(final String siteId, final String documentId)
      throws IOException {

    String site = getCollectionName(siteId);
    String url =
        String.format("%s/collections/%s/documents/%s", this.host, encode(site), documentId);

    HttpHeaders headers = getHeader();

    HttpResponse<String> response = this.service.delete(url, headers);

    return response;
    // if (!is2XX(response)) {
    // throw new IOException(response.body());
    // }
  }

  private String encode(final String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  /**
   * Get Collection Name.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  private String getCollectionName(final String siteId) {
    return siteId != null ? siteId : "default";
  }

  private HttpHeaders getHeader() {
    HttpHeaders headers = new HttpHeaders().add("X-TYPESENSE-API-KEY", this.apiKey);
    return headers;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<String> searchFulltext(final String siteId, final String text, final int maxResults)
      throws IOException {

    String site = getCollectionName(siteId);

    String url = String.format("%s/collections/%s/documents/search?q=%s&query_by=text&per_page=%s",
        this.host, encode(site), encode(text), "" + maxResults);

    HttpHeaders headers = getHeader();

    HttpResponse<String> response = this.service.get(url, headers);

    if (!is2XX(response)) {
      throw new IOException(response.body());
    }

    List<String> list = Collections.emptyList();
    Map<String, Object> map = this.json.fromJsonToMap(response.body());

    if (map.containsKey("hits")) {
      List<Map<String, Object>> hits = (List<Map<String, Object>>) map.get("hits");

      list = hits.stream().map(m -> {
        Map<String, String> mm = (Map<String, String>) m.get("document");
        return mm.get("id");
      }).collect(Collectors.toList());
    }

    return list;
  }

  @Override
  public HttpResponse<String> updateDocument(final String siteId, final String documentId,
      final Map<String, Object> data) throws IOException {

    String site = getCollectionName(siteId);

    String url =
        String.format("%s/collections/%s/documents/%s", this.host, encode(site), documentId);

    HttpHeaders headers = getHeader();

    HttpResponse<String> response =
        this.service.patch(url, headers, BodyPublishers.ofString(this.json.toJson(data)));

    return response;
    // if (!is2XX(response)) {
    // throw new IOException(response.body());
    // }
  }
}
