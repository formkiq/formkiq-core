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
package com.formkiq.stacks.api.transformers;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.module.http.HttpResponseStatus;
import com.formkiq.stacks.api.handler.AddDocumentRequest;

/**
 * Upload Documents to an S3 Presigned Url {@link Function}.
 */
public class PresignedUrlsToS3Bucket implements Function<Map<String, Object>, Void> {

  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();
  /** {@link AddDocumentRequest}. */
  private AddDocumentRequest request;

  /**
   * constructor.
   * 
   * @param addDocumentRequest {@link AddDocumentRequest}
   */
  public PresignedUrlsToS3Bucket(final AddDocumentRequest addDocumentRequest) {
    this.request = addDocumentRequest;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Void apply(final Map<String, Object> mapResponse) {

    String content = this.request.getContent();

    try {
      postContent(mapResponse, this.request.isBase64(), content, this.request.getContentType());

      int i = 0;

      List<Map<String, Object>> docs = (List<Map<String, Object>>) mapResponse.get("documents");

      for (Map<String, Object> map : notNull(docs)) {

        AddDocumentRequest childReq = this.request.getDocuments().get(i);
        postContent(map, childReq.isBase64(), childReq.getContent(), childReq.getContentType());

        i++;
      }

    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  private void postContent(final Map<String, Object> map, final boolean isBase64,
      final String content, final String contentType)
      throws IOException, InterruptedException, URISyntaxException {

    if (!isEmpty(content)) {

      String url = (String) map.get("url");
      String ct = !isEmpty(contentType) ? contentType : MimeType.MIME_OCTET_STREAM.getContentType();

      byte[] bytes = isBase64 ? Base64.getDecoder().decode(content.getBytes(StandardCharsets.UTF_8))
          : content.getBytes(StandardCharsets.UTF_8);

      HttpResponse<String> response =
          this.http.send(
              HttpRequest.newBuilder(new URI(url)).header("Content-Type", ct)
                  .method("PUT", BodyPublishers.ofByteArray(bytes)).build(),
              BodyHandlers.ofString());

      if (!HttpResponseStatus.is2XX(response)) {
        throw new IOException(response.body());
      }

    } else {
      map.put("uploadUrl", map.get("url"));
    }

    map.remove("url");
  }
}
