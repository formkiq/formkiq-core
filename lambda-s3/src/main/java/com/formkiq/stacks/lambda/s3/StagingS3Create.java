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

import static com.formkiq.stacks.dynamodb.DocumentService.DATE_FORMAT;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/** {@link RequestHandler} for handling Document Staging Create Events. */
@Reflectable
@ReflectableImport(classes = {DocumentItemDynamoDb.class, DocumentTag.class, DocumentTagType.class})
public class StagingS3Create implements RequestHandler<Map<String, Object>, Void> {

  /** Extension for FormKiQ config file. */
  private static final String FORMKIQ_B64_EXT = ".fkb64";

  /**
   * Get Bucket Name.
   *
   * @param event {@link Map}
   * @return {@link String}
   */
  @SuppressWarnings("unchecked")
  private static String getBucketName(final Map<String, Object> event) {
    Map<String, Object> s3 = (Map<String, Object>) event.get("s3");
    Map<String, Object> bucket = (Map<String, Object>) s3.get("bucket");

    String bucketName = bucket.get("name").toString();
    return bucketName;
  }

  /**
   * Get Object Key.
   *
   * @param event {@link Map}
   * @return {@link String}
   */
  @SuppressWarnings("unchecked")
  private static String getObjectKey(final Map<String, Object> event) {
    Map<String, Object> s3 = (Map<String, Object>) event.get("s3");
    Map<String, Object> object = (Map<String, Object>) s3.get("object");

    String key = object.get("key").toString();
    return key;
  }

  /**
   * Is {@link String} a {@link UUID}.
   *
   * @param s {@link String}
   * @return boolean
   */
  private static boolean isUuid(final String s) {
    try {
      UUID.fromString(s);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Decode the string according to RFC 3986: encoding for URI paths, query strings, etc. *
   *
   * @param value The string to decode.
   * @return The decoded string.
   */
  private static String urlDecode(final String value) {
    return SdkHttpUtils.urlDecode(value);
  }

  /** {@link DocumentService}. */
  private DocumentService service;
  /** {@link S3Service}. */
  private S3Service s3;
  /** {@link SqsService}. */
  private SqsService sqsService;
  /** {@link String}. */
  private String documentsBucket;

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** SQS Error Queue. */
  private String sqsErrorQueue;

  /** constructor. */
  public StagingS3Create() {
    this(System.getenv(),
        new DocumentServiceImpl(
            new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
            System.getenv("DOCUMENTS_TABLE")),
        new S3ConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SqsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param documentService {@link DocumentService}
   * @param s3Builder {@link S3ConnectionBuilder}
   * @param sqsBuilder {@link SqsConnectionBuilder}
   */
  protected StagingS3Create(final Map<String, String> map, final DocumentService documentService,
      final S3ConnectionBuilder s3Builder, final SqsConnectionBuilder sqsBuilder) {

    this.service = documentService;
    this.s3 = new S3Service(s3Builder);
    this.sqsService = new SqsService(sqsBuilder);

    this.documentsBucket = map.get("DOCUMENTS_S3_BUCKET");
    this.sqsErrorQueue = map.get("SQS_ERROR_URL");
  }

  /**
   * Determines whether {@link String} is a JSON config file.
   *
   * @param s3Client {@link S3Client}
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param documentId {@link String}
   * @return {@link DynamicDocumentItem}
   */
  @SuppressWarnings("unchecked")
  private DynamicDocumentItem configfile(final S3Client s3Client, final LambdaLogger logger,
      final String bucket, final String documentId) {

    DynamicDocumentItem obj = null;

    if (documentId.endsWith(FORMKIQ_B64_EXT)) {
      String s = this.s3.getContentAsString(s3Client, bucket, documentId, null);

      if ("true".equals(System.getenv("DEBUG"))) {
        logger.log(s);
      }

      Map<String, Object> map = this.gson.fromJson(s, Map.class);
      obj = new DynamicDocumentItem(map);

      if (obj.containsKey("insertedDate")) {

        SimpleDateFormat f = new SimpleDateFormat(DATE_FORMAT);

        try {
          obj.setInsertedDate(f.parse(obj.getString("insertedDate")));
        } catch (ParseException e) {
          obj.setInsertedDate(new Date());
        }

      } else {
        obj.setInsertedDate(new Date());
      }
    }

    return obj;
  }

  /**
   * Copies Documentid to a new file that is a {@link UUID}.
   *
   * @param s3Client {@link S3Client}
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param originalkey {@link String}
   * @param date {@link Date}
   */
  private void copyFile(final S3Client s3Client, final LambdaLogger logger, final String bucket,
      final String originalkey, final Date date) {

    String siteId = getSiteId(originalkey);
    String key = resetDatabaseKey(siteId, originalkey);

    boolean uuid = isUuid(key);
    String documentId = uuid ? key : UUID.randomUUID().toString();

    String destKey = createDatabaseKey(siteId, documentId);

    logger.log(String.format("Copying %s from bucket %s to %s in bucket %s.", originalkey, bucket,
        destKey, this.documentsBucket));

    S3ObjectMetadata metadata = this.s3.getObjectMetadata(s3Client, bucket, originalkey);

    String username = metadata.getMetadata().entrySet().stream()
        .filter(s -> s.getKey().equalsIgnoreCase("userid")).findFirst().map(s -> s.getValue())
        .orElse("System");

    DynamicDocumentItem doc = new DynamicDocumentItem(Collections.emptyMap());
    doc.setDocumentId(documentId);
    doc.setContentLength(metadata.getContentLength());
    doc.setContentType(metadata.getContentType());
    doc.setUserId(username);
    doc.setChecksum(metadata.getEtag());
    doc.setInsertedDate(date);

    if (!uuid) {
      doc.setPath(key);
    }

    this.service.saveDocumentItemWithTag(siteId, doc);

    this.s3.copyObject(s3Client, bucket, originalkey, this.documentsBucket, destKey,
        metadata.getContentType());
  }

  /**
   * Delete S3 Object.
   *
   * @param s3Client {@link S3Client}
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param key {@link String}
   */
  private void deleteObject(final S3Client s3Client, final LambdaLogger logger, final String bucket,
      final String key) {
    String msg = String.format("Removing %s from bucket %s.", key, bucket);
    logger.log(msg);
    this.s3.deleteObject(s3Client, bucket, key);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json = null;
    Date date = new Date();

    try {

      LambdaLogger logger = context.getLogger();

      if ("true".equals(System.getenv("DEBUG"))) {
        json = this.gson.toJson(map);
        logger.log(json);
      }

      List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      processRecords(logger, date, records);

    } catch (Exception e) {
      e.printStackTrace();

      if (json == null) {
        json = this.gson.toJson(map);
      }

      if (this.sqsErrorQueue != null) {
        this.sqsService.sendMessage(this.sqsErrorQueue, json);
      }
    }

    return null;
  }

  /**
   * Process S3 Event.
   * 
   * @param logger {@link LambdaLogger}
   * @param date {@link Date}
   * @param event {@link Map}
   */
  private void processEvent(final LambdaLogger logger, final Date date,
      final Map<String, Object> event) {

    String eventName = event.get("eventName").toString();
    boolean objectCreated = eventName.contains("ObjectCreated");

    String bucket = getBucketName(event);

    String key = getObjectKey(event);
    String documentId = urlDecode(key);
    String siteId = getSiteId(documentId);

    if (objectCreated) {

      try (S3Client s = this.s3.buildClient()) {

        DynamicDocumentItem doc = configfile(s, logger, bucket, documentId);

        if (doc != null) {
          write(s, logger, doc, date, siteId);
        } else {
          copyFile(s, logger, bucket, documentId, date);
        }

        deleteObject(s, logger, bucket, documentId);
      }
    }

    if (!objectCreated) {
      logger.log("skipping event " + eventName);
    }
  }

  /**
   * Process Event Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param date {@link Date}
   * @param records {@link List} {@link Map}
   */
  @SuppressWarnings("unchecked")
  private void processRecords(final LambdaLogger logger, final Date date,
      final List<Map<String, Object>> records) {

    for (Map<String, Object> event : records) {

      if (event.containsKey("body")) {

        String body = event.get("body").toString();

        Map<String, Object> map = this.gson.fromJson(body, Map.class);
        processRecords(logger, date, (List<Map<String, Object>>) map.get("Records"));

      } else {
        logger.log("handling " + records.size() + " record(s).");

        processEvent(logger, date, event);
      }
    }
  }

  /**
   * Write {@link DynamicDocumentItem} to S3 & DynamoDB.
   *
   * @param s3Client {@link S3Client}
   * @param logger {@link LambdaLogger}
   * @param doc {@link DynamicDocumentItem}
   * @param date {@link Date}
   * @param siteId {@link String}
   */
  private void write(final S3Client s3Client, final LambdaLogger logger,
      final DynamicDocumentItem doc, final Date date, final String siteId) {

    if (writeS3File(logger, s3Client, siteId, doc)) {

      logger.log(String.format("Inserted %s into bucket %s as %s", doc.getPath(),
          this.documentsBucket, createDatabaseKey(siteId, doc.getDocumentId())));

      this.service.saveDocumentItemWithTag(siteId, doc);

    } else {
      logger.log(String.format("Skipping %s no content", doc.getPath()));
    }
  }

  /**
   * Writes File to S3.
   * @param logger {@link LambdaLogger}
   * @param s3Client {@link S3Client}
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @return boolean
   */
  private boolean writeS3File(final LambdaLogger logger, final S3Client s3Client,
      final String siteId, final DynamicDocumentItem doc) {

    boolean wrote = false;

    updateDocumentIdIfNeeded(doc);

    Map<String, String> contentMap = createContentMap(doc);
    Map<String, String> contentTypeMap = createContentTypeMap(doc);

    for (Map.Entry<String, String> e : contentMap.entrySet()) {

      boolean isBase64 = doc.getBoolean("isBase64").booleanValue();
      byte[] bytes =
          isBase64 ? Base64.getDecoder().decode(e.getValue().getBytes(StandardCharsets.UTF_8))
              : e.getValue().getBytes(StandardCharsets.UTF_8);

      String key = createDatabaseKey(siteId, e.getKey());
      String contentType = contentTypeMap.get(e.getKey());
      
      PutObjectResponse response =
          this.s3.putObject(s3Client, this.documentsBucket, key, bytes, contentType);
      doc.setChecksum(response.eTag());
      doc.setContentLength(Long.valueOf(bytes.length));
      wrote = true;
    }

    return wrote;
  }

  /**
   * Update Document Id, if needed.
   * 
   * @param doc {@link DynamicDocumentItem}s
   */
  private void updateDocumentIdIfNeeded(final DynamicDocumentItem doc) {
    if (!doc.hasString("documentId")) {
      doc.put("documentId", UUID.randomUUID().toString());
    }

    List<DynamicObject> documents = doc.getList("documents");
    for (DynamicObject document : documents) {
      if (!document.hasString("documentId")) {
        document.put("documentId", UUID.randomUUID().toString());
      }
    }
  }

  /**
   * Generate {@link Map} of DocumentId / Content.
   * @param doc {@link DynamicDocumentItem}
   * @return {@link Map}
   */
  private Map<String, String> createContentMap(final DynamicDocumentItem doc) {

    Map<String, String> map = new HashMap<>();

    if (doc.hasString("content")) {
      if (doc.hasString("documentId")) {
        map.put(doc.getString("documentId"), doc.getString("content"));
      }
    }

    List<DynamicObject> documents = doc.getList("documents");
    for (DynamicObject document : documents) {
      if (document.hasString("content") && document.hasString("documentId")) {
        map.put(document.getString("documentId"), document.getString("content"));
      }
    }

    return map;
  }
  
  /**
   * Generate {@link Map} of DocumentId / Content Type.
   * @param doc {@link DynamicDocumentItem}
   * @return {@link Map}
   */
  private Map<String, String> createContentTypeMap(final DynamicDocumentItem doc) {

    Map<String, String> map = new HashMap<>();

    if (doc.hasString("documentId") && doc.getContentType() != null) {
      map.put(doc.getString("documentId"), doc.getContentType());
    }

    List<DynamicObject> documents = doc.getList("documents");
    for (DynamicObject document : documents) {
      if (document.hasString("contentType") && document.hasString("documentId")) {
        map.put(document.getString("documentId"), document.getString("contentType"));
      }
    }

    return map;
  }
}
