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
package com.formkiq.stacks.dynamodb.s3;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.useractivities.UserActivityStatus;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceInterceptor;
import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.stacks.dynamodb.GsonUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * {@link S3ServiceInterceptor} for S3 Document versioning.
 */
public class S3ServiceVersioningInterceptor implements S3ServiceInterceptor {

  /** {@link String}. */
  private final String watchBucket;
  /** {@link DynamoDbService}. */
  private final DynamoDbService versionService;
  /** {@link SimpleDateFormat} in ISO Standard format. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Activities Bucket. */
  private final String activitiesBucket;

  /**
   * constructor.
   * 
   * @param watchS3Bucket {@link String}
   * @param dbVersionService {@link DynamoDbService}
   * @param s3ActivitiesBucket {@link String}
   */
  public S3ServiceVersioningInterceptor(final String watchS3Bucket,
      final DynamoDbService dbVersionService, final String s3ActivitiesBucket) {
    this.watchBucket = watchS3Bucket;
    this.versionService = dbVersionService;
    this.activitiesBucket = s3ActivitiesBucket;
  }

  @Override
  public void putObjectEvent(final S3Service s3, final String bucket, final String key) {

    if (this.watchBucket.equalsIgnoreCase(bucket)) {

      ObjectVersion version = getPreviousObject(s3, bucket, key);
      String siteId = SiteIdKeyGenerator.getSiteIdName(SiteIdKeyGenerator.getSiteId(key));
      String documentId = SiteIdKeyGenerator.getDocumentId(key);

      if (version != null) {
        createVersion(siteId, documentId, s3, bucket, key, version);
      }

      createAudit(s3, siteId, documentId);
    }
  }

  private void createAudit(final S3Service s3, final String siteId, final String documentId) {

    String username = ApiAuthorization.getAuthorization().getUsername();

    UserActivity ua = UserActivity.builder().resource("documents").source("S3Event")
        .type(UserActivityType.NEW_VERSION).status(UserActivityStatus.COMPLETE)
        .documentId(documentId).userId(username).insertedDate(Instant.now())
        .s3Key(siteId, "documents", null, documentId).build(siteId);

    byte[] data = GsonUtil.getInstance().toJson(ua).getBytes(StandardCharsets.UTF_8);
    s3.putObject(this.activitiesBucket, ua.s3Key(), data, "application/json");
  }

  /**
   * S3 File has a version.
   * 
   * @param s3 {@link S3Service}
   * @param bucket {@link String}
   * @param key {@link String}
   * @return ObjectVersion
   */
  public ObjectVersion getPreviousObject(final S3Service s3, final String bucket,
      final String key) {
    ListObjectVersionsResponse response = s3.getObjectVersions(bucket, key, null, 2);
    List<ObjectVersion> objectVersions = notNull(response.versions());
    return objectVersions.size() > 1 ? objectVersions.get(1) : null;
  }

  public void createVersion(final String siteId, final String documentId, final S3Service s3,
      final String bucket, final String key, final ObjectVersion version) {

    String username = ApiAuthorization.getAuthorization().getUsername();

    Map<String, AttributeValue> attr = new HashMap<>();

    String fulldate = this.df.format(new Date());

    String pk = SiteIdKeyGenerator.createDatabaseKey(siteId, PREFIX_DOCS + documentId);
    attr.put(PK, AttributeValue.fromS(pk));
    attr.put(SK, AttributeValue.fromS("document" + TAG_DELIMINATOR + fulldate));

    S3ObjectMetadata resp = s3.getObjectMetadata(bucket, key, version.versionId());

    attr.put("insertedDate", AttributeValue.fromS(fulldate));
    attr.put("userId", AttributeValue.fromS(username));
    attr.put("contentType", AttributeValue.fromS(resp.getContentType()));
    attr.put("contentLength", AttributeValue.fromN(String.valueOf(resp.getContentLength())));

    String checksumType = resp.getChecksumType();
    if (!isEmpty(checksumType)) {
      attr.put("checksumType", AttributeValue.fromS(checksumType));
    }

    String checksum = resp.getChecksum();
    attr.put("checksum", AttributeValue.fromS(checksum));

    attr.put("s3version", AttributeValue.fromS(resp.getVersionId()));

    this.versionService.putItem(attr);
  }
}
