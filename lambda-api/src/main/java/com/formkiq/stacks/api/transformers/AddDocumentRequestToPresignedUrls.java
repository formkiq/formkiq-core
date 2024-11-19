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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.handler.AddDocumentRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

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
  /** {@link CacheService}. */
  private final CacheService cacheService;
  /** Username. */
  private final String username;

  /**
   * constructor.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param authorization {@link ApiAuthorization}
   * @param documentSiteId {@link String}
   * @param urlDuration {@link Duration}
   * @param documentContentLength {@link Long}
   */
  public AddDocumentRequestToPresignedUrls(final AwsServiceCache awsservice,
      final ApiAuthorization authorization, final String documentSiteId, final Duration urlDuration,
      final Optional<Long> documentContentLength) {
    this.siteId = documentSiteId;
    this.s3PresignerService = awsservice.getExtension(S3PresignerService.class);
    this.cacheService = awsservice.getExtension(CacheService.class);
    this.s3Bucket = awsservice.environment("DOCUMENTS_S3_BUCKET");
    this.duration = urlDuration != null ? urlDuration : Duration.ofHours(1);
    this.contentLength = documentContentLength;
    this.username = authorization.getUsername();
  }

  @Override
  public Map<String, Object> apply(final AddDocumentRequest item) {

    Map<String, Object> map = new HashMap<>();

    String documentId = item.getDocumentId();
    map.put("documentId", documentId);

    if (isEmpty(item.getDeepLinkPath())) {
      String docUrl = generatePresignedUrl(item);
      addHeaders(map, item);
      map.put("url", docUrl);
    }

    List<Map<String, String>> child = new ArrayList<>();

    for (AddDocumentRequest o : notNull(item.getDocuments())) {

      Map<String, String> m = new HashMap<>();

      String docid = o.getDocumentId();
      m.put("documentId", docid);

      if (isEmpty(o.getDeepLinkPath())) {
        String url = generatePresignedUrl(o);
        m.put("url", url);
      }

      child.add(m);
    }

    if (!child.isEmpty()) {
      map.put("documents", child);
    }

    return map;
  }

  private void addHeaders(final Map<String, Object> map, final AddDocumentRequest item) {

    Map<String, String> headers = new HashMap<>();

    if (item.getChecksumType() != null) {

      String checksumType = item.getChecksumType().toUpperCase();
      headers.put("x-amz-sdk-checksum-algorithm", item.getChecksumType().toUpperCase());

      if ("SHA256".equals(checksumType)) {
        headers.put("x-amz-checksum-sha256", this.s3PresignerService.toBase64(item.getChecksum()));
      } else if ("SHA1".equals(checksumType)) {
        headers.put("x-amz-checksum-sha1", this.s3PresignerService.toBase64(item.getChecksum()));
      }
    }

    if (!headers.isEmpty()) {
      map.put("headers", headers);
    }
  }

  private String generatePresignedUrl(final AddDocumentRequest o) {

    String documentId = o.getDocumentId();
    String key = !isDefaultSiteId(this.siteId) ? this.siteId + "/" + documentId : documentId;

    ChecksumAlgorithm checksumAlgorithm =
        this.s3PresignerService.getChecksumAlgorithm(o.getChecksumType());

    String url = this.s3PresignerService.presignPutUrl(this.s3Bucket, key, this.duration,
        checksumAlgorithm, o.getChecksum(), this.contentLength, null).toString();

    String cacheKey = "s3PresignedUrl#" + this.s3Bucket + "#" + key;
    final int cacheInDays = 7;
    this.cacheService.write(cacheKey, this.username, cacheInDays);

    return url;
  }
}
