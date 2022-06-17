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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.module.documentevents.DocumentEventType.ACTIONS;
import static com.formkiq.module.documentevents.DocumentEventType.CREATE;
import static com.formkiq.module.documentevents.DocumentEventType.DELETE;
import static com.formkiq.module.documentevents.DocumentEventType.UPDATE;
import static com.formkiq.stacks.dynamodb.DocumentService.SYSTEM_DEFINED_TAGS;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.documentevents.DocumentEvent;
import com.formkiq.module.documentevents.DocumentEventService;
import com.formkiq.module.documentevents.DocumentEventServiceSns;
import com.formkiq.stacks.common.formats.MimeType;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentTagToDynamicDocumentTag;
import com.formkiq.stacks.dynamodb.DynamicDocumentTag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/** {@link RequestHandler} for writing MetaData for Documents to DynamoDB. */
@Reflectable
public class DocumentsS3Update implements RequestHandler<Map<String, Object>, Void> {

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
   * Decode the string according to RFC 3986: encoding for URI paths, query strings, etc. *
   *
   * @param value The string to decode.
   * @return The decoded string.
   */
  public static String urlDecode(final String value) {
    return SdkHttpUtils.urlDecode(value);
  }

  /** SNS Document Event Arn. */
  private String snsDocumentEvent;
  /** SQS Url to send errors to. */
  private String sqsErrorUrl;
  /** {@link DocumentService}. */
  private DocumentService service;
  /** {@link S3Service}. */
  private S3Service s3service;
  /** {@link SqsService}. */
  private SqsService sqsService;
  /** {@link DocumentEventService}. */
  private DocumentEventService documentEventService;
  /** {@link ActionsService}. */
  private ActionsService actionsService;

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** constructor. */
  public DocumentsS3Update() {
    this(System.getenv(),
        new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new S3ConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SqsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SnsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))));
  }

  /**
   * constructor.
   * 
   * @param map {@link Map}
   * @param db {@link DynamoDbConnectionBuilder}
   * @param s3builder {@link S3ConnectionBuilder}
   * @param sqsBuilder {@link SqsConnectionBuilder}
   * @param snsBuilder {@link SnsConnectionBuilder}
   */
  protected DocumentsS3Update(final Map<String, String> map, final DynamoDbConnectionBuilder db,
      final S3ConnectionBuilder s3builder, final SqsConnectionBuilder sqsBuilder,
      final SnsConnectionBuilder snsBuilder) {

    this.sqsErrorUrl = map.get("SQS_ERROR_URL");
    this.snsDocumentEvent = map.get("SNS_DOCUMENT_EVENT");
    this.actionsService = new ActionsServiceDynamoDb(db, map.get("DOCUMENTS_TABLE"));
    this.service = new DocumentServiceImpl(db, map.get("DOCUMENTS_TABLE"));
    this.s3service = new S3Service(s3builder);
    this.sqsService = new SqsService(sqsBuilder);
    this.documentEventService = new DocumentEventServiceSns(snsBuilder);
  }

  /**
   * Get Object Tags from S3.
   * 
   * @param s3 {@link S3Client}
   * @param item {@link DocumentItem}
   * @param bucket {@link String}
   * @param documentId {@link String}
   * @return {@link List} {@link DynamicDocumentTag}
   */
  private List<DynamicDocumentTag> getObjectTags(final S3Client s3, final DocumentItem item,
      final String bucket, final String documentId) {

    GetObjectTaggingResponse objectTags = this.s3service.getObjectTags(s3, bucket, documentId);

    List<DocumentTag> tags = objectTags.tagSet().stream().map(t -> new DocumentTag(documentId,
        t.key(), t.value(), item.getInsertedDate(), item.getUserId())).collect(Collectors.toList());

    // Any System Defined Tags in the S3 Metadata, set them to SystemDefined.
    tags.stream().filter(t -> SYSTEM_DEFINED_TAGS.contains(t.getKey()))
        .forEach(t -> t.setType(DocumentTagType.SYSTEMDEFINED));

    List<DynamicDocumentTag> dtags = tags.stream()
        .map(t -> new DocumentTagToDynamicDocumentTag().apply(t)).collect(Collectors.toList());

    return dtags;
  }

  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    LambdaLogger logger = context.getLogger();

    try {

      boolean debug = "true".equals(System.getenv("DEBUG"));
      if (debug) {
        String json = this.gson.toJson(map);
        logger.log(json);
      }

      List<Map<String, Object>> list = processRecords(logger, map);

      for (Map<String, Object> e : list) {

        Object eventName = e.getOrDefault("eventName", null);
        Object bucket = e.getOrDefault("s3bucket", null);
        Object key = e.getOrDefault("s3key", null);

        if (bucket != null && key != null) {

          boolean create =
              eventName != null && eventName.toString().toLowerCase().contains("objectcreated");

          boolean remove =
              eventName != null && eventName.toString().toLowerCase().contains("objectremove");

          if (debug) {
            logger.log(String.format("processing event %s for file %s in bucket %s", eventName,
                bucket, key));
          }

          if (remove) {

            processS3Delete(logger, bucket.toString(), key.toString());

          } else {
            processS3File(logger, create, bucket.toString(), key.toString(), debug);
          }
        }
      }

    } catch (Exception e) {

      e.printStackTrace();
      sendToDlq(map);
    }

    return null;
  }

  private void sendToDlq(final Map<String, Object> map) {
    String json = this.gson.toJson(map);
    if (this.sqsErrorUrl != null) {
      this.sqsService.sendMessage(this.sqsErrorUrl, json);
    }
  }

  /**
   * Process S3 Delete Request.
   * 
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param key {@link String}
   */
  private void processS3Delete(final LambdaLogger logger, final String bucket, final String key) {

    String siteId = getSiteId(key.toString());
    String documentId = resetDatabaseKey(siteId, key.toString());
    DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId", documentId));

    String msg = String.format("Removing %s from bucket %s.", key, bucket);
    logger.log(msg);

    this.service.deleteDocument(siteId, documentId);
    sendSnsMessage(logger, DELETE, siteId, doc, bucket, key, null);
  }

  /**
   * Process S3 Event.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link Map}
   * @return {@link Map}
   * @throws FileNotFoundException FileNotFoundException
   */
  private Map<String, Object> processEvent(final LambdaLogger logger,
      final Map<String, Object> event) throws FileNotFoundException {

    Map<String, Object> map = new HashMap<>();
    String eventName = event.get("eventName").toString();

    String bucket = getBucketName(event);
    String key = getObjectKey(event);
    map.put("s3bucket", bucket);
    map.put("s3key", key);
    map.put("eventName", eventName);

    return map;
  }

  /**
   * Process S3 File.
   * 
   * @param logger {@link LambdaLogger}
   * @param create boolean
   * @param s3bucket {@link String}
   * @param s3key {@link String}
   * @param debug boolean
   * @throws FileNotFoundException FileNotFoundException
   */
  private void processS3File(final LambdaLogger logger, final boolean create, final String s3bucket,
      final String s3key, final boolean debug) throws FileNotFoundException {

    String key = urlDecode(s3key);

    String siteId = getSiteId(key);
    String documentId = resetDatabaseKey(siteId, key);

    try (S3Client s3 = this.s3service.buildClient()) {

      S3ObjectMetadata resp = this.s3service.getObjectMetadata(s3, s3bucket, key);

      if (!resp.isObjectExists()) {
        throw new FileNotFoundException(
            "Object " + documentId + " not found in bucket " + s3bucket);
      }

      String contentType = resp.getContentType();
      Long contentLength = resp.getContentLength();

      DocumentItem item = this.service.findDocument(siteId, documentId);

      if (item != null) {

        DynamicDocumentItem doc = new DocumentItemToDynamicDocumentItem().apply(item);

        if (contentType != null && contentType.length() > 0) {
          doc.setContentType(contentType);
        }

        doc.setChecksum(resp.getEtag());

        if (contentLength != null) {
          doc.setContentLength(contentLength);
        }

        logger.log("saving document " + createDatabaseKey(siteId, item.getDocumentId()));

        List<DynamicDocumentTag> tags = getObjectTags(s3, item, s3bucket, key);
        doc.put("tags", tags);

        if (debug) {
          logger.log("original " + this.gson.toJson(item));
          logger.log("new " + this.gson.toJson(doc));
        }

        this.service.saveDocumentItemWithTag(siteId, doc);

        this.service.deleteDocumentFormats(siteId, item.getDocumentId());

        String content = getContent(s3bucket, key, s3, resp, doc);

        sendSnsMessage(logger, create ? CREATE : UPDATE, siteId, doc, s3bucket, key, content);

      } else {
        logger.log("Cannot find document " + documentId + " in site " + siteId);
      }
    }
  }

  private String getContent(final String s3bucket, final String key, final S3Client s3,
      final S3ObjectMetadata resp, final DynamicDocumentItem doc) {

    String content = null;

    if (MimeType.isPlainText(doc.getContentType()) && resp.getContentLength() != null
        && resp.getContentLength().longValue() < DocumentEventServiceSns.MAX_SNS_MESSAGE_SIZE) {
      content = this.s3service.getContentAsString(s3, s3bucket, key, null);
    }

    return content;
  }

  /**
   * Process Event Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param records {@link List} {@link Map}
   * @return {@link Map}
   * @throws FileNotFoundException FileNotFoundException
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> processRecords(final LambdaLogger logger,
      final List<Map<String, Object>> records) throws FileNotFoundException {

    List<Map<String, Object>> list = new ArrayList<>();

    for (Map<String, Object> event : records) {

      if (event.containsKey("body")) {

        String body = event.get("body").toString();

        Map<String, Object> map = this.gson.fromJson(body, Map.class);
        list.addAll(processRecords(logger, map));

      } else if (event.containsKey("eventName")) {
        list.add(processEvent(logger, event));
      } else {
        list.addAll(processRecords(logger, event));
      }
    }

    return list;
  }

  /**
   * Process Event Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param map {@link Map}
   * @return {@link List} {@link Map}
   * @throws FileNotFoundException FileNotFoundException
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> processRecords(final LambdaLogger logger,
      final Map<String, Object> map) throws FileNotFoundException {

    List<Map<String, Object>> list = new ArrayList<>();

    if (map.containsKey("Records")) {
      List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      list.addAll(processRecords(logger, records));
    } else if (map.containsKey("Message")) {
      String body = map.get("Message").toString();
      Map<String, Object> messageMap = this.gson.fromJson(body, Map.class);
      list.addAll(processRecords(logger, messageMap));
    } else if (map.containsKey("Sns")) {
      Map<String, Object> messageMap = (Map<String, Object>) map.get("Sns");
      list.addAll(processRecords(logger, messageMap));
    } else if (map.containsKey("s3key")) {
      list.add(map);
    }

    return list;
  }

  /**
   * Either sends the Create Message to SNS.
   * 
   * @param logger {@link LambdaLogger}
   * @param eventType {@link String}
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @param s3Bucket {@link String}
   * @param s3Key {@link String}
   * @param content {@link String}
   */
  private void sendSnsMessage(final LambdaLogger logger, final String eventType,
      final String siteId, final DynamicDocumentItem doc, final String s3Bucket, final String s3Key,
      final String content) {

    String site = siteId != null ? siteId : SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String documentId = resetDatabaseKey(siteId, doc.getDocumentId());

    DocumentEvent event = new DocumentEvent().siteId(site).documentId(documentId).s3bucket(s3Bucket)
        .s3key(s3Key).type(eventType).userId(doc.getUserId()).contentType(doc.getContentType())
        .path(doc.getPath());

    if ("application/json".equals(doc.getContentType())) {
      event.content(content);
    }

    String eventJson = this.documentEventService.publish(this.snsDocumentEvent, event);

    boolean debug = "true".equals(System.getenv("DEBUG"));
    if (debug) {
      logger.log("event: " + eventJson);
    }

    logger.log("publishing " + event.type() + " document message to " + this.snsDocumentEvent);

    if (CREATE.equals(eventType) && this.actionsService.hasActions(siteId, documentId)) {
      sendDocumentActionsEvent(logger, site, documentId);
    }
  }

  /**
   * Send Actions SNS message.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  private void sendDocumentActionsEvent(final LambdaLogger logger, final String siteId,
      final String documentId) {
    DocumentEvent event = new DocumentEvent().siteId(siteId).documentId(documentId).type(ACTIONS);
    logger.log("publishing " + event.type() + " document message to " + this.snsDocumentEvent);
    this.documentEventService.publish(this.snsDocumentEvent, event);
  }
}
