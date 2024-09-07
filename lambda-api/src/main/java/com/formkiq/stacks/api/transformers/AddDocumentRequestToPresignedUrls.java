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

import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.handler.AddDocumentRequest;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Transforms {@link AddDocumentRequest} to a list of Presigned Urls.
 */
public class AddDocumentRequestToPresignedUrls
    implements Function<AddDocumentRequest, Map<String, Object>> {

  /** {@link S3PresignerService}. */
  private final S3PresignerService s3PresignerService;
  /** S3 Bucket. */
  private final String s3Bucket;
  /** {@link Duration}. */
  private final Duration duration;
  /** {@link Optional} Content Length. */
  private final Optional<Long> contentLength;
  /** Site Id. */
  private final String siteId;

  /**
   * constructor.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param documentSiteId {@link String}
   * @param urlDuration {@link Duration}
   * @param documentContentLength {@link Long}
   */
  public AddDocumentRequestToPresignedUrls(final AwsServiceCache awsservice,
      final String documentSiteId, final Duration urlDuration,
      final Optional<Long> documentContentLength) {
    this.siteId = documentSiteId;
    this.s3PresignerService = awsservice.getExtension(S3PresignerService.class);
    this.s3Bucket = awsservice.environment("DOCUMENTS_S3_BUCKET");
    this.duration = urlDuration != null ? urlDuration : Duration.ofHours(1);
    this.contentLength = documentContentLength;
  }

  @Override
  public Map<String, Object> apply(final AddDocumentRequest item) {

    Map<String, Object> map = new HashMap<>();

    String documentId = item.getDocumentId();
    map.put("documentId", documentId);

    if (Strings.isEmpty(item.getDeepLinkPath())) {
      String docUrl = generatePresignedUrl(documentId);
      map.put("url", docUrl);
    }

    List<Map<String, String>> child = new ArrayList<>();

    for (AddDocumentRequest o : notNull(item.getDocuments())) {

      Map<String, String> m = new HashMap<>();

      String docid = o.getDocumentId();
      m.put("documentId", docid);

      if (Strings.isEmpty(o.getDeepLinkPath())) {
        String url = generatePresignedUrl(docid);
        m.put("url", url);
      }

      child.add(m);
    }

    if (!child.isEmpty()) {
      map.put("documents", child);
    }

    return map;
  }

  private String generatePresignedUrl(final String documentId) {

    String key = !isDefaultSiteId(this.siteId) ? this.siteId + "/" + documentId : documentId;

    Map<String, String> map = Map.of("checksum", UUID.randomUUID().toString());
    URL url = this.s3PresignerService.presignPutUrl(this.s3Bucket, key, this.duration,
        this.contentLength, map);

    return url.toString();
  }
}
