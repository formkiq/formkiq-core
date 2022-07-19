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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.GetDocumentContentRequest;
import com.formkiq.stacks.client.requests.GetDocumentUploadRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * FormKiQ Documents Service.
 *
 */
public class FkqDocumentService {

  /** 200 OK. */
  private static final int STATUS_OK = 200;
  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  /**
   * Add "file" but this just creates DynamoDB record and not the S3 file.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param path {@link String}
   * @param content byte[]
   * @param contentType {@link String}
   * @return {@link String}
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  public String addDocument(final FormKiqClientV1 client, final String siteId, final String path,
      final byte[] content, final String contentType)
      throws IOException, URISyntaxException, InterruptedException {
    // given
    final int status = 200;
    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(siteId).path(path).contentLength(content.length);

    // when
    HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

    // then
    if (response.statusCode() == status) {
      Map<String, Object> map = toMap(response);
      String s3url = map.get("url").toString();
      this.http.send(HttpRequest.newBuilder(new URI(s3url)).header("Content-Type", contentType)
          .method("PUT", BodyPublishers.ofByteArray(content)).build(), BodyHandlers.ofString());
      return map.get("documentId").toString();
    }

    throw new IOException("unexpected response " + response.statusCode());
  }

  /**
   * Convert {@link HttpResponse} to {@link Map}.
   * 
   * @param response {@link HttpResponse}
   * @return {@link Map}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  protected Map<String, Object> toMap(final HttpResponse<String> response) throws IOException {
    Map<String, Object> m = this.gson.fromJson(response.body(), Map.class);
    return m;
  }

  /**
   * Fetch Document.
   * 
   * @param client {@link FormKiqClientV1}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  public void waitForDocumentContent(final FormKiqClientV1 client, final String siteId,
      final String documentId) throws IOException, InterruptedException, URISyntaxException {

    GetDocumentContentRequest request =
        new GetDocumentContentRequest().siteId(siteId).documentId(documentId);

    while (true) {

      HttpResponse<String> response = client.getDocumentContentAsHttpResponse(request);
      if (STATUS_OK == response.statusCode()) {
        break;
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }
}
