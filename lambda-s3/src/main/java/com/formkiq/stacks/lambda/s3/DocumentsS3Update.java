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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.dynamodb.objects.Strings.removeQuotes;
import static com.formkiq.module.events.document.DocumentEventType.CREATE;
import static com.formkiq.module.events.document.DocumentEventType.DELETE;
import static com.formkiq.module.events.document.DocumentEventType.UPDATE;
import static com.formkiq.stacks.dynamodb.DocumentService.SYSTEM_DEFINED_TAGS;
import static com.formkiq.stacks.dynamodb.DocumentVersionService.S3VERSION_ATTRIBUTE;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.ssm.SmsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.EventServiceSns;
import com.formkiq.module.events.EventServiceSnsExtension;
import com.formkiq.module.events.document.DocumentEvent;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.httpsigv4.HttpServiceSigv4;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/** {@link RequestHandler} for writing MetaData for Documents to DynamoDB. */
@Reflectable
public class DocumentsS3Update implements RequestHandler<Map<String, Object>, Void> {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** Bad Request. */
  static final int BAD_REQUEST = 400;
  /** {@link EventService}. */
  private static EventService documentEventService;

  /** {@link ActionsNotificationService}. */
  private static ActionsNotificationService notificationService;

  /** {@link S3Service}. */
  private static S3Service s3service;

  /** Server Error. */
  static final int SERVER_ERROR = 500;

  /** {@link DocumentService}. */
  private static DocumentService service;

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;
  /** SNS Document Event Arn. */
  private static String snsDocumentEvent;

  static {

    if (System.getenv().containsKey("AWS_REGION")) {
      serviceCache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
              new SnsAwsServiceRegistry(), new SmsAwsServiceRegistry())
          .build();

      initialize(serviceCache);
    }
  }

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
   * Initialize.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   */
  static void initialize(final AwsServiceCache awsServiceCache) {

    awsServiceCache.register(S3Service.class, new S3ServiceExtension());
    awsServiceCache.register(ActionsService.class, new ActionsServiceExtension());
    awsServiceCache.register(SsmService.class, new SsmServiceExtension());
    awsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    awsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServiceCache.register(EventService.class, new EventServiceSnsExtension());
    awsServiceCache.register(ActionsNotificationService.class,
        new ActionsNotificationServiceExtension());

    AwsCredentials awsCredentials = awsServiceCache.getExtension(AwsCredentials.class);
    awsServiceCache.register(HttpService.class, new ClassServiceExtension<HttpService>(
        new HttpServiceSigv4(awsServiceCache.region(), awsCredentials)));

    service = awsServiceCache.getExtension(DocumentService.class);
    snsDocumentEvent = awsServiceCache.environment("SNS_DOCUMENT_EVENT");
    actionsService = awsServiceCache.getExtension(ActionsService.class);
    s3service = awsServiceCache.getExtension(S3Service.class);
    documentEventService = awsServiceCache.getExtension(EventService.class);
    notificationService = awsServiceCache.getExtension(ActionsNotificationService.class);

    if (isEmpty(awsServiceCache.environment("DOCUMENTS_IAM_URL"))) {
      SsmService ssm = awsServiceCache.getExtension(SsmService.class);

      String appEnvironment = awsServiceCache.environment("APP_ENVIRONMENT");

      String documentsIamUrl =
          ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");
      awsServiceCache.environment().put("DOCUMENTS_IAM_URL", documentsIamUrl);
    }
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

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** constructor. */
  public DocumentsS3Update() {
    // empty
  }

  /**
   * constructor.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public DocumentsS3Update(final AwsServiceCache awsServiceCache) {
    initialize(awsServiceCache);
    serviceCache = awsServiceCache;
  }

  /**
   * Builds Document Event.
   * 
   * @param eventType {@link String}
   * @param siteId {@link String}
   * @param doc {@link DocumentItem}
   * @param s3Bucket {@link String}
   * @param s3Key {@link String}
   * @param contentType {@link String}
   * @return {@link DocumentEvent}
   */
  private DocumentEvent buildDocumentEvent(final String eventType, final String siteId,
      final DocumentItem doc, final String s3Bucket, final String s3Key, final String contentType) {
    String site = siteId != null ? siteId : SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String documentId = resetDatabaseKey(siteId, doc.getDocumentId());

    DocumentEvent event =
        new DocumentEvent().siteId(site).documentId(documentId).s3bucket(s3Bucket).s3key(s3Key)
            .type(eventType).userId(doc.getUserId()).contentType(contentType).path(doc.getPath());

    return event;
  }

  /**
   * Check Response for 400 / 500 throw exception.
   * 
   * @param module {@link String}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param response {@link HttpResponse}
   * @throws IOException IOException
   */
  private void checkResponse(final String module, final String siteId, final String documentId,
      final HttpResponse<String> response) throws IOException {
    int statusCode = response.statusCode();
    if (statusCode == BAD_REQUEST || statusCode == SERVER_ERROR) {
      throw new IOException(String.format("Unable to delete document %s from site %s in module %s",
          documentId, siteId, module));
    }
  }

  private String getContent(final String s3bucket, final String key, final S3ObjectMetadata resp,
      final String contentType) {

    String content = null;

    if (MimeType.isPlainText(contentType) && resp.getContentLength() != null
        && resp.getContentLength().longValue() < EventServiceSns.MAX_SNS_MESSAGE_SIZE) {
      content = s3service.getContentAsString(s3bucket, key, null);
    }

    return content;
  }

  /**
   * Get Object Tags from S3.
   * 
   * @param bucket {@link String}
   * @param documentId {@link String}
   * @return {@link List} {@link DocumentTag}
   */
  private List<DocumentTag> getObjectTags(final String bucket, final String documentId) {

    GetObjectTaggingResponse objectTags = s3service.getObjectTags(bucket, documentId);

    List<DocumentTag> tags = objectTags.tagSet().stream()
        .map(t -> new DocumentTag(documentId, t.key(), t.value(), new Date(), "System"))
        .collect(Collectors.toList());

    // Any System Defined Tags in the S3 Metadata, set them to SystemDefined.
    tags.stream().filter(t -> SYSTEM_DEFINED_TAGS.contains(t.getKey()))
        .forEach(t -> t.setType(DocumentTagType.SYSTEMDEFINED));

    return tags;
  }

  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    LambdaLogger logger = context.getLogger();

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

      String s = String.format("{\"eventName\": \"%s\",\"bucket\": \"%s\",\"key\": \"%s\"}",
          eventName, bucket, key);
      logger.log(s);

      if (bucket != null && key != null) {

        boolean create =
            eventName != null && eventName.toString().toLowerCase().contains("objectcreated");

        boolean remove =
            eventName != null && eventName.toString().toLowerCase().contains("objectremove");

        if (debug) {
          logger.log(String.format("processing event %s for file %s in bucket %s", eventName,
              bucket, key));
        }

        try {

          if (remove) {

            processS3Delete(logger, bucket.toString(), key.toString());

          } else {
            processS3File(logger, create, bucket.toString(), key.toString(), debug);
          }

        } catch (IOException | InterruptedException ex) {
          throw new RuntimeException(ex);
        }
      }
    }

    return null;
  }

  private boolean isChecksumChanged(final S3ObjectMetadata resp, final DocumentItem doc) {
    String s3Checksum = resp.getMetadata().getOrDefault("checksum", "");
    return doc.getChecksum() != null && !s3Checksum.contains(doc.getChecksum());
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
      final Map<String, Object> event) {

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
   * Process Event Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param records {@link List} {@link Map}
   * @return {@link Map}
   * @throws FileNotFoundException FileNotFoundException
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> processRecords(final LambdaLogger logger,
      final List<Map<String, Object>> records) {

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
      final Map<String, Object> map) {

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
   * Process S3 Delete Request.
   * 
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param key {@link String}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  private void processS3Delete(final LambdaLogger logger, final String bucket, final String key)
      throws IOException, InterruptedException {

    if (!s3service.getObjectMetadata(bucket, key, null).isObjectExists()) {

      String siteId = getSiteId(key.toString());
      String documentId = resetDatabaseKey(siteId, key.toString());

      String msg = String.format("Removing %s from bucket %s.", key, bucket);
      logger.log(msg);

      boolean moduleOcr = serviceCache.hasModule("ocr");
      boolean moduleFulltext = serviceCache.hasModule("opensearch");
      boolean moduleTypesense = serviceCache.hasModule("typesense");

      if (moduleOcr || moduleFulltext || moduleTypesense) {

        HttpService http = serviceCache.getExtension(HttpService.class);

        Optional<Map<String, String>> parameters =
            siteId != null ? Optional.of(Map.of("siteId", siteId)) : Optional.empty();

        if (moduleOcr) {

          String u =
              serviceCache.environment("DOCUMENTS_IAM_URL") + "/documents/" + documentId + "/ocr";
          HttpResponse<String> response = http.delete(u, Optional.empty(), parameters);

          checkResponse("ocr", siteId, documentId, response);
        }

        if (moduleFulltext || moduleTypesense) {
          String u = serviceCache.environment("DOCUMENTS_IAM_URL") + "/documents/" + documentId
              + "/fulltext";
          HttpResponse<String> response = http.delete(u, Optional.empty(), parameters);

          checkResponse("opensearch", siteId, documentId, response);
        }
      }

      service.deleteDocument(siteId, documentId, false);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId", documentId));

      DocumentEvent event =
          buildDocumentEvent(DELETE, siteId, doc, bucket, key, doc.getContentType());

      sendSnsMessage(logger, event, doc.getContentType(), bucket, key, null);
    }
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

    S3ObjectMetadata resp = s3service.getObjectMetadata(s3bucket, key, null);

    if (!resp.isObjectExists()) {
      throw new FileNotFoundException("Object " + documentId + " not found in bucket " + s3bucket);
    }

    String contentType = resp.getContentType();
    Long contentLength = resp.getContentLength();

    DocumentItem item = service.findDocument(siteId, documentId);

    if (item != null) {

      Map<String, AttributeValue> attributes = new HashMap<>();

      boolean isChecksumChanged = isChecksumChanged(resp, item);

      if (debug) {
        logger.log("metadata: " + resp.getMetadata());
        logger.log("item checksum: " + item.getChecksum());
      }

      if (isChecksumChanged) {
        attributes.put("lastModifiedDate", AttributeValue.fromS(this.df.format(new Date())));
      }

      if (contentType != null && contentType.length() > 0) {
        attributes.put("contentType", AttributeValue.fromS(contentType));
      }

      String checksum = removeQuotes(resp.getEtag());
      attributes.put("checksum", AttributeValue.fromS(checksum));

      if (contentLength != null) {
        attributes.put("contentLength", AttributeValue.fromN("" + contentLength));
      }

      if (resp.getVersionId() != null) {
        attributes.put(S3VERSION_ATTRIBUTE, AttributeValue.fromS(resp.getVersionId()));
      }

      service.updateDocument(siteId, documentId, attributes, isChecksumChanged);

      List<DocumentTag> tags = getObjectTags(s3bucket, key);
      service.addTags(siteId, documentId, tags, null);

      service.deleteDocumentFormats(siteId, documentId);

      DocumentEvent event =
          buildDocumentEvent(create ? CREATE : UPDATE, siteId, item, s3bucket, key, contentType);
      sendSnsMessage(logger, event, contentType, s3bucket, key, resp);

    } else {
      logger.log("Cannot find document " + documentId + " in site " + siteId);
    }
  }

  /**
   * Either sends the Create Message to SNS.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link DocumentEvent}
   * @param contentType {@link String}
   * @param s3bucket {@link String}
   * @param key {@link String}
   * @param resp {@link S3ObjectMetadata}
   */
  private void sendSnsMessage(final LambdaLogger logger, final DocumentEvent event,
      final String contentType, final String s3bucket, final String key,
      final S3ObjectMetadata resp) {

    String siteId = event.siteId();
    String documentId = event.documentId();

    if ("application/json".equals(contentType)) {
      String content = getContent(s3bucket, key, resp, contentType);
      event.content(content);
    }

    String eventJson = documentEventService.publish(event);

    boolean debug = "true".equals(System.getenv("DEBUG"));
    if (debug) {
      logger.log("event: " + eventJson);
    }

    String eventType = event.type();
    logger.log("publishing " + event.type() + " document message to " + snsDocumentEvent);

    if (CREATE.equals(eventType)) {
      List<Action> actions = actionsService.getActions(siteId, documentId);
      notificationService.publishNextActionEvent(actions, siteId, documentId);
    }
  }
}
