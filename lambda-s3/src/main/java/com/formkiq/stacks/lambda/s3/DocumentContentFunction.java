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
package com.formkiq.stacks.lambda.s3;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * {@link Function} for getting the Document Content Urls.
 *
 */
public class DocumentContentFunction {

  /** S3 Documents Bucket. */
  private String documentsBucket;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link S3PresignerService}. */
  private S3PresignerService s3Service;
  /** {@link HttpService}. */
  private HttpService http;
  /** {@link String}. */
  private String documentsIamUrl;

  /**
   * constructor.
   * 
   * @param serviceCache {@link AwsServiceCache}
   */
  public DocumentContentFunction(final AwsServiceCache serviceCache) {
    this.s3Service = serviceCache.getExtension(S3PresignerService.class);
    this.documentsBucket = serviceCache.environment("DOCUMENTS_S3_BUCKET");
    this.documentsIamUrl = serviceCache.environment("documentsIamUrl");
    this.http = serviceCache.getExtension(HttpService.class);
  }

  /**
   * Find Content Url.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link List} {@link String}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private List<String> findContentUrls(final LambdaLogger logger, final String siteId,
      final DocumentItem item) throws IOException {

    List<String> urls = Collections.emptyList();
    String documentId = item.getDocumentId();
    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);

    if (logger != null) {
      logger.log("content type: " + item.getContentType());
    }

    if (MimeType.isPlainText(item.getContentType())) {

      PresignGetUrlConfig config = new PresignGetUrlConfig()
          .contentDispositionByPath(item.getPath(), false).contentType(item.getContentType());

      String url = this.s3Service
          .presignGetUrl(this.documentsBucket, s3Key, Duration.ofHours(1), null, config).toString();
      urls = Arrays.asList(url);

    } else {

      Map<String, String> parameters = new HashMap<>();
      parameters.put("contentUrl", "true");
      parameters.put("text", "true");

      if (siteId != null) {
        parameters.put("siteId", siteId);
      }

      String url = this.documentsIamUrl + "/documents/" + documentId + "/ocr";

      HttpResponse<String> response = this.http.get(url, Optional.empty(), Optional.of(parameters));

      if (logger != null) {
        logger.log("GET /documents/{documentId}/ocr response: " + response.body());
      }

      Map<String, Object> map = this.gson.fromJson(response.body(), Map.class);

      if (map != null && map.containsKey("contentUrls")) {
        urls = (List<String>) map.get("contentUrls");
      }
    }

    return urls;
  }

  /**
   * Get Content from external urls.
   * 
   * @param contentUrls {@link List} {@link String}
   * @return {@link StringBuilder}
   * @throws IOException IOException
   */
  public StringBuilder getContentUrls(final List<String> contentUrls) throws IOException {

    StringBuilder sb = new StringBuilder();

    try {
      for (String contentUrl : contentUrls) {
        HttpRequest req =
            HttpRequest.newBuilder(new URI(contentUrl)).timeout(Duration.ofMinutes(1)).build();
        HttpResponse<String> response =
            HttpClient.newBuilder().build().send(req, BodyHandlers.ofString());
        sb.append(response.body());
      }
    } catch (URISyntaxException | InterruptedException e) {
      throw new IOException(e);
    }

    return sb;
  }

  /**
   * Convert {@link DocumentItem} to list of Document Content Urls.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link List} {@link String}
   * @throws IOException IOException
   */
  public List<String> getContentUrls(final LambdaLogger logger, final String siteId,
      final DocumentItem item) throws IOException {
    List<String> contentUrls = findContentUrls(logger, siteId, item);

    if (logger != null) {
      logger.log("FOUND: " + contentUrls.size() + " content urls");
    }
    return contentUrls;
  }

}
