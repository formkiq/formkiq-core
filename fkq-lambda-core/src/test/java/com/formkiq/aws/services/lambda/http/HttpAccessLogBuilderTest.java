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
package com.formkiq.aws.services.lambda.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link HttpAccessLogBuilder} correctly builds {@link HttpAccessLog} and produces the
 * expected JSON using Gson.
 */
class HttpAccessLogBuilderTest {

  /** {@link Gson}. */
  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().serializeNulls().setPrettyPrinting().create();

  @Test
  @DisplayName("Build HttpAccessLog and verify JSON output using Gson")
  void testJsonOutput() {

    // given
    final int status = 200;
    HttpAccessLog log = new HttpAccessLogBuilder().requestTime("2025-10-05T14:38:08Z")
        .requestId("R-lgrgHiiYcEPeA=").clientIp("50.71.4.111")
        .userId("c1abc5a0-3001-701c-2184-1fe53d0cfc9e")
        .http("GET", "HTTP/1.1", "GET /sites", Map.of("documentId", "1"), Map.of("siteId", "111"))
        .resp(status).userAgent("Mozilla/5.0 (...)").build();

    // when
    String json = GSON.toJson(log);

    // then
    assertTrue(json.contains("\"requestTime\": \"2025-10-05T14:38:08Z\""));
    assertTrue(json.contains("\"requestId\": \"R-lgrgHiiYcEPeA=\""));
    assertTrue(json.contains("\"client\": {"));
    assertTrue(json.contains("\"ip\": \"50.71.4.111\""));
    assertTrue(json.contains("\"user\": {"));
    assertTrue(json.contains("\"id\": \"c1abc5a0-3001-701c-2184-1fe53d0cfc9e\""));
    assertTrue(json.contains("\"http\": {"));
    assertTrue(json.contains("\"method\": \"GET\""));
    assertTrue(json.contains("\"protocol\": \"HTTP/1.1\""));
    assertTrue(json.contains("\"route\": \"GET /sites\""));
    assertTrue(json.contains("\"resp\": {"));
    assertTrue(json.contains("\"status\": 200"));
    assertTrue(json.contains("\"documentId\": \"1\""));
    assertTrue(json.contains("\"siteId\": \"111\""));
    assertTrue(json.contains("\"userAgent\": \"Mozilla/5.0 (...)\""));

    // --- Round-trip test (deserialize back) ---
    HttpAccessLog parsed = GSON.fromJson(json, HttpAccessLog.class);
    assertEquals(log, parsed, "Deserialized record should equal the original");
  }
}
