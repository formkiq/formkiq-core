/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.aws.s3;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
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
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * 
 * S3 Services.
 *
 */
public class S3Service {

  /**
   * URL Encode {@link String}.
   *
   * @param string {@link String}
   * @return {@link String}
   */
  public static String encode(final String string) {
    try {
      return URLEncoder.encode(string, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * URL Decode {@link String}.
   *
   * @param string {@link String}
   * @return {@link String}
   */
  public static String decode(final String string) {
    try {
      return URLDecoder.decode(string, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convert {@link InputStream} to byte[].
   * 
   * @param is {@link InputStream}
   * @return byte[]
   * @throws IOException IOException
   */
  public static byte[] toByteArray(final InputStream is) throws IOException {
    byte[] byteArray = IoUtils.toByteArray(is);
    return byteArray;
  }

  /** {@link S3ConnectionBuilder}. */
  private S3ConnectionBuilder builder;

  /**
   * Constructor.
   * 
   * @param s3connectionBuilder {@link S3ConnectionBuilder}
   */
  public S3Service(final S3ConnectionBuilder s3connectionBuilder) {
    this.builder = s3connectionBuilder;
  }

  /**
   * Build {@link S3Client}.
   * 
   * @return {@link S3Client}
   */
  public S3Client buildClient() {
    return this.builder.build();
  }

  /**
   * Copy S3 Object.
   * 
   * @param s3 {@link S3Client}
   * @param sourcebucket {@link String}
   * @param sourcekey {@link String}
   * @param destinationBucket {@link String}
   * @param destinationKey {@link String}
   * @param contentType {@link String}
   */
  public void copyObject(final S3Client s3, final String sourcebucket, final String sourcekey,
      final String destinationBucket, final String destinationKey, final String contentType) {
    CopyObjectRequest.Builder req =
        CopyObjectRequest.builder().copySource(encode(sourcebucket + "/" + sourcekey))
            .destinationBucket(destinationBucket).destinationKey(destinationKey);

    if (contentType != null) {
      req = req.contentType(contentType);
    }

    s3.copyObject(req.build());
  }

  /**
   * Create S3 Bucket.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   */
  public void createBucket(final S3Client s3, final String bucket) {
    s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    s3.putBucketVersioning(PutBucketVersioningRequest.builder().bucket(bucket)
        .versioningConfiguration(
            VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build())
        .build());
  }

  /**
   * Delete All Files in bucket.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   */
  public void deleteAllFiles(final S3Client s3, final String bucket) {
    boolean isDone = false;

    while (!isDone) {

      ListObjectsRequest req = ListObjectsRequest.builder().bucket(bucket).build();
      ListObjectsResponse resp = s3.listObjects(req);

      for (S3Object s3Object : resp.contents()) {
        deleteObject(s3, bucket, s3Object.key());
      }

      isDone = !resp.isTruncated().booleanValue();
    }
  }

  /**
   * Delete All S3 Object Tags.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   */
  public void deleteAllObjectTags(final S3Client s3, final String bucket, final String key) {
    DeleteObjectTaggingRequest req =
        DeleteObjectTaggingRequest.builder().bucket(bucket).key(key).build();
    s3.deleteObjectTagging(req);
  }

  /**
   * Delete Object.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   */
  public void deleteObject(final S3Client s3, final String bucket, final String key) {
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  /**
   * Whether Bucket exists.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @return boolean
   */
  public boolean exists(final S3Client s3, final String bucket) {
    try {
      s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      return true;
    } catch (NoSuchBucketException e) {
      return false;
    }
  }

  /**
   * Get File Content as {@link InputStream}.
   * 
   * @param s3 {@link S3Client}
   * @param distributionBucket {@link String}
   * @param key {@link String}
   * @return {@link InputStream}
   */
  public InputStream getContentAsInputStream(final S3Client s3, final String distributionBucket,
      final String key) {
    GetObjectRequest get = GetObjectRequest.builder().bucket(distributionBucket).key(key).build();
    ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(get);

    return response.asInputStream();
  }

  /**
   * Get File String Content.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * @param versionId {@link String}
   * @return {@link String}
   */
  public String getContentAsString(final S3Client s3, final String bucket, final String key,
      final String versionId) {

    GetObjectRequest gr =
        GetObjectRequest.builder().bucket(bucket).key(key)/* .versionId(versionId) */.build();
    ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(gr);

    String s = response.asUtf8String();
    return s;
  }

  /**
   * Get Bucket's Notifications.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @return {@link GetBucketNotificationConfigurationResponse}
   */
  public GetBucketNotificationConfigurationResponse getNotifications(final S3Client s3,
      final String bucket) {
    GetBucketNotificationConfigurationRequest req =
        GetBucketNotificationConfigurationRequest.builder().bucket(bucket).build();
    return s3.getBucketNotificationConfiguration(req);
  }

  /**
   * Get the S3 Object Meta Data.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * 
   * @return {@link S3ObjectMetadata}
   */
  public S3ObjectMetadata getObjectMetadata(final S3Client s3, final String bucket,
      final String key) {
    HeadObjectRequest hr = HeadObjectRequest.builder().bucket(bucket).key(key).build();
    S3ObjectMetadata md = new S3ObjectMetadata();

    try {
      HeadObjectResponse resp = s3.headObject(hr);

      Map<String, String> metadata = resp.metadata();
      md.setObjectExists(true);
      md.setContentType(resp.contentType());
      md.setMetadata(metadata);
      md.setEtag(resp.eTag());
      md.setContentLength(resp.contentLength());

    } catch (NoSuchKeyException e) {
      md.setObjectExists(false);
    }

    return md;
  }

  /**
   * Get Object Tags.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * @return {@link GetObjectTaggingResponse}
   */
  public GetObjectTaggingResponse getObjectTags(final S3Client s3, final String bucket,
      final String key) {
    GetObjectTaggingRequest req = GetObjectTaggingRequest.builder().bucket(bucket).key(key).build();
    GetObjectTaggingResponse response = s3.getObjectTagging(req);
    return response;
  }

  /**
   * Get Object Versions.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param prefix {@link String}
   * @param keyMarker {@link String}
   * @return {@link ListObjectVersionsResponse}
   */
  public ListObjectVersionsResponse getObjectVersions(final S3Client s3, final String bucket,
      final String prefix, final String keyMarker) {
    ListObjectVersionsRequest req = ListObjectVersionsRequest.builder().bucket(bucket)
        .prefix(prefix).keyMarker(keyMarker).build();
    ListObjectVersionsResponse response = s3.listObjectVersions(req);
    return response;
  }

  /**
   * List S3 Objects.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param prefix {@link String}
   * @return {@link ListObjectsResponse}
   */
  public ListObjectsResponse listObjects(final S3Client s3, final String bucket,
      final String prefix) {
    Builder listbuilder = ListObjectsRequest.builder().bucket(bucket);

    if (prefix != null) {
      listbuilder = listbuilder.prefix(prefix);
    }

    ListObjectsResponse listObjects = s3.listObjects(listbuilder.build());
    return listObjects;
  }

  /**
   * Generate a S3 Signed Url for getting an object.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @param duration {@link Duration}
   * @param versionId {@link String}
   * @return {@link URL}
   */
  public URL presignGetUrl(final String bucket, final String key, final Duration duration,
      final String versionId) {

    try (S3Presigner signer = this.builder.buildPresigner()) {

      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(bucket).key(key).versionId(versionId).build();

      GetObjectPresignRequest getRequest = GetObjectPresignRequest.builder()
          .signatureDuration(duration).getObjectRequest(getObjectRequest).build();

      PresignedGetObjectRequest req = signer.presignGetObject(getRequest);
      URL url = req.url();

      return url;
    }
  }

  /**
   * Generate a S3 Signed Url for creating an object using POST request.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @param duration {@link Duration}
   * @param contentLength {@link Optional} {@link Long}
   * @return {@link URL}
   */
  public URL presignPostUrl(final String bucket, final String key, final Duration duration,
      final Optional<Long> contentLength) {

    try (S3Presigner signer = this.builder.buildPresigner()) {

      UploadPartRequest.Builder uploadBuilder = UploadPartRequest.builder().bucket(bucket).key(key);

      if (contentLength.isPresent()) {
        uploadBuilder = uploadBuilder.contentLength(contentLength.get());
      }

      UploadPartPresignRequest prereq = UploadPartPresignRequest.builder()
          .signatureDuration(duration).uploadPartRequest(uploadBuilder.build()).build();

      PresignedUploadPartRequest req = signer.presignUploadPart(prereq);
      return req.url();
    }
  }

  /**
   * Generate a S3 Signed Url for creating an object using PUT request.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @param duration {@link Duration}
   * @return {@link URL}
   */
  public URL presignPutUrl(final String bucket, final String key, final Duration duration) {

    try (S3Presigner signer = this.builder.buildPresigner()) {

      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(bucket).key(key).build();

      PutObjectPresignRequest putRequest = PutObjectPresignRequest.builder()
          .signatureDuration(duration).putObjectRequest(putObjectRequest).build();

      PresignedPutObjectRequest req = signer.presignPutObject(putRequest);
      URL url = req.url();

      return url;
    }
  }

  /**
   * Put Object in Bucket.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * @param data byte[]
   * @param contentType {@link String}
   * @return {@link PutObjectResponse}
   */
  public PutObjectResponse putObject(final S3Client s3, final String bucket, final String key,
      final byte[] data, final String contentType) {
    return putObject(s3, bucket, key, data, contentType, null);
  }

  /**
   * Put Object in Bucket.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * @param data byte[]
   * @param contentType {@link String}
   * @param metadata {@link Map}
   * @return {@link PutObjectResponse}
   */
  public PutObjectResponse putObject(final S3Client s3, final String bucket, final String key,
      final byte[] data, final String contentType, final Map<String, String> metadata) {
    int contentLength = data.length;
    PutObjectRequest.Builder build = PutObjectRequest.builder().bucket(bucket).key(key)
        .contentLength(Long.valueOf(contentLength));

    if (contentType != null) {
      build.contentType(contentType);
    }

    if (metadata != null) {
      build.metadata(metadata);
    }

    PutObjectResponse response = s3.putObject(build.build(), RequestBody.fromBytes(data));
    return response;
  }

  /**
   * Put Object in Bucket.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * @param is {@link InputStream}
   * @param contentType {@link String}
   * @return {@link PutObjectResponse}
   * @throws IOException IOException
   */
  public PutObjectResponse putObject(final S3Client s3, final String bucket, final String key,
      final InputStream is, final String contentType) throws IOException {
    byte[] data = toByteArray(is);
    return putObject(s3, bucket, key, data, contentType, null);
  }

  /**
   * Set S3 Object Tag.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   */
  public void setObjectTag(final S3Client s3, final String bucket, final String key,
      final String tagKey, final String tagValue) {
    Collection<Tag> tagSet = Arrays.asList(Tag.builder().key(tagKey).value(tagValue).build());
    setObjectTags(s3, bucket, key, tagSet);
  }

  /**
   * Set S3 Object Tag.
   * 
   * @param s3 {@link S3Client}
   * @param bucket {@link String}
   * @param key {@link String}
   * @param tags {@link Collection} {@link Tag}
   */
  public void setObjectTags(final S3Client s3, final String bucket, final String key,
      final Collection<Tag> tags) {

    Tagging tagging = Tagging.builder().tagSet(tags).build();
    PutObjectTaggingRequest req =
        PutObjectTaggingRequest.builder().bucket(bucket).key(key).tagging(tagging).build();
    s3.putObjectTagging(req);
  }
}
