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
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.base64.MapToBase64;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeEntityKeyValue;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.FindEntityById;
import com.formkiq.aws.dynamodb.entity.RetentionEffectiveEndDateAttribute;
import com.formkiq.aws.dynamodb.entity.RetentionMode;
import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.plugins.useractivity.UserActivityContextData;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.documents.AddDocumentRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_MODE;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_POLICY;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Transforms {@link AddDocumentRequest} to a list of Presigned Urls.
 */
public class AddDocumentRequestToPresignedUrls
    implements BiFunction<AddDocumentRequest, String, Map<String, Object>> {

  /** Object Lock retention mode cache key. */
  static final String OBJECT_LOCK_RETENTION_MODE = "objectLockRetentionMode";
  /** Object Lock retain until date cache key. */
  static final String OBJECT_LOCK_RETAIN_UNTIL_DATE = "objectLockRetainUntilDate";
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
  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link DynamoDbService}. */
  private final DynamoDbService dynamoDbService;
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
    this.documentService = awsservice.getExtension(DocumentService.class);
    this.dynamoDbService = awsservice.getExtension(DynamoDbService.class);
    this.s3Bucket = awsservice.environment("DOCUMENTS_S3_BUCKET");
    this.duration = urlDuration != null ? urlDuration : Duration.ofHours(1);
    this.contentLength = documentContentLength;
    this.username = authorization.getUsername();
  }

  private void addHeaders(final Map<String, Object> map, final AddDocumentRequest item) {

    Map<String, String> headers = new HashMap<>();

    if (item.getChecksumType() != null) {

      String checksumType = item.getChecksumType().toUpperCase();
      headers.put("x-amz-sdk-checksum-algorithm", item.getChecksumType().toUpperCase());

      switch (checksumType) {
        case "SHA256" -> headers.put("x-amz-checksum-sha256",
            this.s3PresignerService.toBase64(item.getChecksum()));
        case "SHA512" -> headers.put("x-amz-checksum-sha512",
            this.s3PresignerService.toBase64(item.getChecksum()));
        case "SHA1" ->
          headers.put("x-amz-checksum-sha1", this.s3PresignerService.toBase64(item.getChecksum()));
        default -> throw new IllegalStateException("Unexpected value: " + checksumType);
      }
    }

    if (!headers.isEmpty()) {
      map.put("headers", headers);
    }
  }

  private void addObjectLockRetention(final Map<String, String> map,
      final DocumentArtifact document) {

    DocumentRecord documentRecord = this.documentService.findDocument(this.siteId, document);
    if (documentRecord != null) {

      List<DocumentAttributeRecord> documentAttributes = this.documentService
          .findDocumentAttribute(this.siteId, document, RETENTION_POLICY.getKey());

      if (!documentAttributes.isEmpty()) {
        EntityRecord entity = new FindEntityById().find(this.dynamoDbService,
            this.dynamoDbService.getTableName(), this.siteId, DocumentAttributeEntityKeyValue
                .fromString(documentAttributes.getFirst().getStringValue()));

        if (entity != null) {
          String retentionMode =
              DynamoDbTypes.toString(entity.attributes().get(RETENTION_MODE.getKey()));

          if (RetentionMode.GOVERNANCE.name().equals(retentionMode)) {
            map.put(OBJECT_LOCK_RETENTION_MODE, RetentionMode.GOVERNANCE.name());
            map.put(OBJECT_LOCK_RETAIN_UNTIL_DATE,
                new RetentionEffectiveEndDateAttribute().calculate(entity, documentRecord));
          }
        }
      }
    }
  }

  @Override
  public Map<String, Object> apply(final AddDocumentRequest req, final String artifactId) {

    Map<String, Object> map = new HashMap<>();

    String documentId = req.getDocumentId();
    map.put("documentId", documentId);
    map.put("artifactId", artifactId);
    DocumentArtifact document = DocumentArtifact.of(documentId, artifactId);

    if (isEmpty(req.getDeepLinkPath())) {
      String docUrl = generatePresignedUrl(req, document);
      addHeaders(map, req);
      map.put("url", docUrl);
    }

    List<Map<String, String>> child = new ArrayList<>();

    for (AddDocumentRequest o : notNull(req.getDocuments())) {

      Map<String, String> m = new HashMap<>();

      String docid = o.getDocumentId();
      m.put("documentId", docid);
      document = DocumentArtifact.of(docid, null);

      if (isEmpty(o.getDeepLinkPath())) {
        String url = generatePresignedUrl(o, document);
        m.put("url", url);
      }

      child.add(m);
    }

    if (!child.isEmpty()) {
      map.put("documents", child);
    }

    return map;
  }

  private String generatePresignedUrl(final AddDocumentRequest o, final DocumentArtifact document) {

    String key = createS3Key(siteId, document);

    final String cacheKey = "s3PresignedUrl#" + this.s3Bucket + "#" + key;
    final int cacheInDays = 7;

    Map<String, String> map = new HashMap<>();
    map.put("username", this.username);
    map.put("path", getOldPath());
    addObjectLockRetention(map, document);

    String s = new MapToBase64().apply(map);
    this.cacheService.write(cacheKey, s, cacheInDays);

    ChecksumAlgorithm checksumAlgorithm =
        this.s3PresignerService.getChecksumAlgorithm(o.getChecksumType());

    return this.s3PresignerService.presignPutUrl(this.s3Bucket, key, this.duration,
        checksumAlgorithm, o.getChecksum(), this.contentLength, null).toString();
  }

  private String getOldPath() {
    Collection<UserActivityContextData> activities = UserActivityContext.get();

    var updateActivity = activities.stream()
        .filter(a -> UserActivityType.UPDATE.equals(a.activityType())).findFirst();

    if (updateActivity.isPresent()) {
      ChangeRecord path = updateActivity.get().changeRecords().get("path");
      return path != null ? (String) path.oldValue() : null;
    }

    return null;
  }
}
