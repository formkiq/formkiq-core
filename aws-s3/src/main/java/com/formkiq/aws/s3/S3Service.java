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
package com.formkiq.aws.s3;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest.Builder;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 
 * S3 Services.
 *
 */
public class S3Service {

  /** {@link S3ServiceInterceptor}. */
  private final S3ServiceInterceptor interceptor;

  /**
   * URL Decode {@link String}.
   *
   * @param string {@link String}
   * @return {@link String}
   */
  public static String decode(final String string) {
    return URLDecoder.decode(string, StandardCharsets.UTF_8);
  }

  /**
   * URL Encode {@link String}.
   *
   * @param string {@link String}
   * @return {@link String}
   */
  public static String encode(final String string) {
    return URLEncoder.encode(string, StandardCharsets.UTF_8);
  }

  /**
   * Convert {@link InputStream} to byte[].
   * 
   * @param is {@link InputStream}
   * @return byte[]
   * @throws IOException IOException
   */
  public static byte[] toByteArray(final InputStream is) throws IOException {
    return IoUtils.toByteArray(is);
  }

  /** {@link S3Client}. */
  private final S3Client s3Client;

  /**
   * Constructor.
   * 
   * @param s3connectionBuilder {@link S3ConnectionBuilder}
   */
  public S3Service(final S3ConnectionBuilder s3connectionBuilder) {
    this(s3connectionBuilder, null);
  }

  /**
   * Constructor.
   *
   * @param s3connectionBuilder {@link S3ConnectionBuilder}
   * @param s3Interceptor {@link S3ServiceInterceptor}
   */
  public S3Service(final S3ConnectionBuilder s3connectionBuilder,
      final S3ServiceInterceptor s3Interceptor) {
    this.s3Client = s3connectionBuilder.build();
    this.interceptor = s3Interceptor;
  }

  /**
   * Copy S3 Object.
   * 
   * @param sourcebucket {@link String}
   * @param sourcekey {@link String}
   * @param destinationBucket {@link String}
   * @param destinationKey {@link String}
   * @param contentType {@link String}
   * @param metadata {@link Map}
   * @return {@link CopyObjectResponse}
   */
  public CopyObjectResponse copyObject(final String sourcebucket, final String sourcekey,
      final String destinationBucket, final String destinationKey, final String contentType,
      final Map<String, String> metadata) {

    CopyObjectRequest.Builder req = CopyObjectRequest.builder().sourceBucket(sourcebucket)
        .sourceKey(sourcekey).destinationBucket(destinationBucket).destinationKey(destinationKey);

    if (contentType != null) {
      req = req.contentType(contentType);
    }

    if (metadata != null) {
      req = req.metadata(metadata).metadataDirective(MetadataDirective.REPLACE);
    }

    return this.s3Client.copyObject(req.build());
  }

  /**
   * Perform {@link CopyObjectRequest}.
   * 
   * @param req {@link CopyObjectRequest}
   * @return {@link CopyObjectResponse}
   */
  public CopyObjectResponse copyRequest(final CopyObjectRequest req) {
    return this.s3Client.copyObject(req);
  }

  /**
   * Create S3 Bucket.
   * 
   * @param bucket {@link String}
   */
  public void createBucket(final String bucket) {
    this.s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    this.s3Client.putBucketVersioning(PutBucketVersioningRequest.builder().bucket(bucket)
        .versioningConfiguration(
            VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build())
        .build());
  }

  /**
   * Delete All Files in bucket.
   * 
   * @param bucket {@link String}
   */
  public void deleteAllFiles(final String bucket) {
    boolean isDone = false;

    while (!isDone) {

      ListObjectsRequest req = ListObjectsRequest.builder().bucket(bucket).build();
      ListObjectsResponse resp = this.s3Client.listObjects(req);

      for (S3Object s3Object : resp.contents()) {
        deleteObject(bucket, s3Object.key(), null);
      }

      isDone = !resp.isTruncated();
    }
  }

  /**
   * Deletes all versions (including delete markers) for the specified file in a versioned bucket.
   *
   * @param bucketName the name of the S3 bucket
   * @param key the key (path/filename) of the file to delete
   * @return int number of objects deleted
   */
  public int deleteAllVersionsOfFile(final String bucketName, final String key) {

    int totalDeleted = 0;
    final int maxKeys = 1000;
    String keyMarker = null;
    String versionIdMarker = null;
    ListObjectVersionsResponse response;

    ListObjectVersionsRequest.Builder requestBuilder =
        ListObjectVersionsRequest.builder().bucket(bucketName).prefix(key).maxKeys(maxKeys);

    do {

      if (keyMarker != null && versionIdMarker != null) {
        requestBuilder.keyMarker(keyMarker).versionIdMarker(versionIdMarker);
      }

      response = s3Client.listObjectVersions(requestBuilder.build());

      for (ObjectVersion version : response.versions()) {
        if (version.key().equals(key)) {
          DeleteObjectRequest deleteVersionRequest = DeleteObjectRequest.builder()
              .bucket(bucketName).key(key).versionId(version.versionId()).build();
          totalDeleted++;
          s3Client.deleteObject(deleteVersionRequest);
        }
      }

      for (DeleteMarkerEntry deleteMarker : response.deleteMarkers()) {
        if (deleteMarker.key().equals(key)) {
          DeleteObjectRequest deleteMarkerRequest = DeleteObjectRequest.builder().bucket(bucketName)
              .key(key).versionId(deleteMarker.versionId()).build();
          totalDeleted++;
          s3Client.deleteObject(deleteMarkerRequest);
        }
      }

      // Prepare for next page of results, if any
      keyMarker = response.nextKeyMarker();
      versionIdMarker = response.nextVersionIdMarker();
    } while (response.isTruncated());

    return totalDeleted;
  }

  /**
   * Delete All S3 Object Tags.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   */
  public void deleteAllObjectTags(final String bucket, final String key) {
    DeleteObjectTaggingRequest req =
        DeleteObjectTaggingRequest.builder().bucket(bucket).key(key).build();
    this.s3Client.deleteObjectTagging(req);
  }

  /**
   * Delete Object.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param versionId {@link String}
   */
  public void deleteObject(final String bucket, final String key, final String versionId) {
    this.s3Client.deleteObject(
        DeleteObjectRequest.builder().bucket(bucket).key(key).versionId(versionId).build());

  }

  /**
   * Whether Bucket exists.
   * 
   * @param bucket {@link String}
   * @return boolean
   */
  public boolean exists(final String bucket) {
    try {
      this.s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      return true;
    } catch (NoSuchBucketException e) {
      return false;
    }
  }

  /**
   * Get File Content as byte[].
   * 
   * @param distributionBucket {@link String}
   * @param key {@link String}
   * @return byte[]
   */
  public byte[] getContentAsBytes(final String distributionBucket, final String key) {
    GetObjectRequest get = GetObjectRequest.builder().bucket(distributionBucket).key(key).build();
    ResponseBytes<GetObjectResponse> response = this.s3Client.getObjectAsBytes(get);
    return response.asByteArray();
  }

  /**
   * Get File Content as {@link InputStream}.
   * 
   * @param distributionBucket {@link String}
   * @param key {@link String}
   * @return {@link InputStream}
   */
  public InputStream getContentAsInputStream(final String distributionBucket, final String key) {
    GetObjectRequest get = GetObjectRequest.builder().bucket(distributionBucket).key(key).build();
    ResponseBytes<GetObjectResponse> response = this.s3Client.getObjectAsBytes(get);
    return response.asInputStream();
  }

  /**
   * Get File String Content.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param versionId {@link String}
   * @return {@link String}
   */
  public String getContentAsString(final String bucket, final String key, final String versionId) {

    GetObjectRequest gr =
        GetObjectRequest.builder().bucket(bucket).key(key).versionId(versionId).build();
    ResponseBytes<GetObjectResponse> response = this.s3Client.getObjectAsBytes(gr);

    return response.asUtf8String();
  }

  /**
   * Get Content in Parts.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param range {@link String}
   * @return {@link InputStream}
   */
  public InputStream getContentPartAsInputStream(final String bucket, final String key,
      final String range) {
    GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).range(range).build();
    ResponseBytes<GetObjectResponse> response = this.s3Client.getObjectAsBytes(get);
    return response.asInputStream();
  }

  /**
   * Get Bucket's Notifications.
   * 
   * @param bucket {@link String}
   * @return {@link GetBucketNotificationConfigurationResponse}
   */
  public GetBucketNotificationConfigurationResponse getNotifications(final String bucket) {
    GetBucketNotificationConfigurationRequest req =
        GetBucketNotificationConfigurationRequest.builder().bucket(bucket).build();
    return this.s3Client.getBucketNotificationConfiguration(req);
  }

  /**
   * Get the S3 Object Meta Data.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param versionId {@link String}
   * 
   * @return {@link S3ObjectMetadata}
   */
  public S3ObjectMetadata getObjectMetadata(final String bucket, final String key,
      final String versionId) {

    HeadObjectRequest hr =
        HeadObjectRequest.builder().bucket(bucket).key(key).versionId(versionId).build();
    S3ObjectMetadata md = new S3ObjectMetadata();

    try {
      HeadObjectResponse resp = this.s3Client.headObject(hr);

      Map<String, String> metadata = resp.metadata();
      md.setObjectExists(true);
      md.setContentType(resp.contentType());
      md.setMetadata(metadata);
      md.setEtag(resp.eTag());
      md.setContentLength(resp.contentLength());
      md.setVersionId(resp.versionId());
      md.setChecksumSha1(resp.checksumSHA1());
      md.setChecksumSha256(resp.checksumSHA256());

    } catch (NoSuchKeyException e) {
      md.setObjectExists(false);
    }

    return md;
  }

  /**
   * Get Object Tags.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @return {@link GetObjectTaggingResponse}
   */
  public GetObjectTaggingResponse getObjectTags(final String bucket, final String key) {
    GetObjectTaggingRequest req = GetObjectTaggingRequest.builder().bucket(bucket).key(key).build();
    return this.s3Client.getObjectTagging(req);
  }

  /**
   * Get Object Versions.
   * 
   * @param bucket {@link String}
   * @param prefix {@link String}
   * @param keyMarker {@link String}
   * @param maxKeys {@link Integer}
   * @return {@link ListObjectVersionsResponse}
   */
  public ListObjectVersionsResponse getObjectVersions(final String bucket, final String prefix,
      final String keyMarker, final Integer maxKeys) {
    ListObjectVersionsRequest req = ListObjectVersionsRequest.builder().bucket(bucket)
        .prefix(prefix).keyMarker(keyMarker).maxKeys(maxKeys).build();
    return this.s3Client.listObjectVersions(req);
  }

  /**
   * List S3 Objects.
   * 
   * @param bucket {@link String}
   * @param prefix {@link String}
   * @return {@link ListObjectsResponse}
   */
  public ListObjectsResponse listObjects(final String bucket, final String prefix) {
    Builder listbuilder = ListObjectsRequest.builder().bucket(bucket);

    if (prefix != null) {
      listbuilder = listbuilder.prefix(prefix);
    }

    return this.s3Client.listObjects(listbuilder.build());
  }

  /**
   * Put Object in Bucket.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param data byte[]
   * @param contentType {@link String}
   * @return {@link PutObjectResponse}
   */
  public PutObjectResponse putObject(final String bucket, final String key, final byte[] data,
      final String contentType) {
    return putObject(bucket, key, data, contentType, null);
  }

  /**
   * Put Object in Bucket.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param data byte[]
   * @param contentType {@link String}
   * @param metadata {@link Map}
   * @return {@link PutObjectResponse}
   */
  public PutObjectResponse putObject(final String bucket, final String key, final byte[] data,
      final String contentType, final Map<String, String> metadata) {
    int contentLength = data.length;
    PutObjectRequest.Builder build =
        PutObjectRequest.builder().bucket(bucket).key(key).contentLength((long) contentLength);

    if (contentType != null) {
      build.contentType(contentType);
    }

    if (metadata != null) {
      build.metadata(metadata);
    }

    PutObjectRequest request = build.build();

    if (this.interceptor != null) {
      this.interceptor.putObjectEvent(this, bucket, key);
    }

    return this.s3Client.putObject(request, RequestBody.fromBytes(data));
  }

  /**
   * Put Object in Bucket.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param is {@link InputStream}
   * @param contentType {@link String}
   * @return {@link PutObjectResponse}
   * @throws IOException IOException
   */
  public PutObjectResponse putObject(final String bucket, final String key, final InputStream is,
      final String contentType) throws IOException {
    byte[] data = toByteArray(is);
    return putObject(bucket, key, data, contentType);
  }

  /**
   * Set S3 Object Tag.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   */
  public void setObjectTag(final String bucket, final String key, final String tagKey,
      final String tagValue) {
    Collection<Tag> tagSet = List.of(Tag.builder().key(tagKey).value(tagValue).build());
    setObjectTags(bucket, key, tagSet);
  }

  /**
   * Set S3 Object Tag.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @param tags {@link Collection} {@link Tag}
   */
  public void setObjectTags(final String bucket, final String key, final Collection<Tag> tags) {

    Tagging tagging = Tagging.builder().tagSet(tags).build();
    PutObjectTaggingRequest req =
        PutObjectTaggingRequest.builder().bucket(bucket).key(key).tagging(tagging).build();
    this.s3Client.putObjectTagging(req);
  }
}
