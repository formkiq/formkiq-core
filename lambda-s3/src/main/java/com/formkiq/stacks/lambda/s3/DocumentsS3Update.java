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
import static com.formkiq.module.events.document.DocumentEventType.CREATE;
import static com.formkiq.module.events.document.DocumentEventType.DELETE;
import static com.formkiq.module.events.document.DocumentEventType.UPDATE;
import static com.formkiq.stacks.dynamodb.DocumentService.SYSTEM_DEFINED_TAGS;
import static com.formkiq.stacks.dynamodb.DocumentVersionService.S3VERSION_ATTRIBUTE;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.dynamodb.cache.CacheServiceExtension;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3PresignerServiceExtension;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.s3.S3ServiceInterceptor;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
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
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.s3.S3ServiceInterceptorExtension;
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
  /** {@link S3ServiceInterceptor}. */
  private static S3ServiceInterceptor s3ServiceInterceptor;
  /** {@link CacheService}. */
  private static CacheService cacheService;
  /** {@link S3PresignerService}. */
  private static S3PresignerService s3PresignedService;
  /** {@link Logger}. */
  private static Logger logger;

  static {

    if (System.getenv().containsKey("AWS_REGION")) {
      serviceCache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
              new SsmAwsServiceRegistry())
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
  private static String getBucketName(final Map<String, Object> event) {
    Map<String, Object> s3 = (Map<String, Object>) event.get("s3");
    Map<String, Object> bucket = (Map<String, Object>) s3.get("bucket");
    return (String) bucket.get("name");
  }

  /**
   * Get Object Key.
   *
   * @param event {@link Map}
   * @return {@link String}
   */
  private static String getObjectKey(final Map<String, Object> event) {
    Map<String, Object> s3 = (Map<String, Object>) event.get("s3");
    Map<String, Object> object = (Map<String, Object>) s3.get("object");

    return (String) object.get("key");
  }

  /**
   * Get Version Id.
   *
   * @param event {@link Map}
   * @return {@link String}
   */
  private static String getVersionId(final Map<String, Object> event) {
    Map<String, Object> s3 = (Map<String, Object>) event.get("s3");
    Map<String, Object> object = (Map<String, Object>) s3.get("object");

    return (String) object.get("versionId");
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
    awsServiceCache.register(S3ServiceInterceptor.class, new S3ServiceInterceptorExtension());
    awsServiceCache.register(S3PresignerService.class, new S3PresignerServiceExtension());
    awsServiceCache.register(CacheService.class, new CacheServiceExtension());

    AwsCredentials awsCredentials = awsServiceCache.getExtension(AwsCredentials.class);
    awsServiceCache.register(HttpService.class, new ClassServiceExtension<>(
        new HttpServiceSigv4(awsServiceCache.region(), awsCredentials)));

    s3PresignedService = awsServiceCache.getExtension(S3PresignerService.class);
    service = awsServiceCache.getExtension(DocumentService.class);
    snsDocumentEvent = awsServiceCache.environment("SNS_DOCUMENT_EVENT");
    actionsService = awsServiceCache.getExtension(ActionsService.class);
    s3service = awsServiceCache.getExtension(S3Service.class);
    documentEventService = awsServiceCache.getExtension(EventService.class);
    notificationService = awsServiceCache.getExtension(ActionsNotificationService.class);
    s3ServiceInterceptor = awsServiceCache.getExtension(S3ServiceInterceptor.class);
    cacheService = awsServiceCache.getExtension(CacheService.class);

    if (isEmpty(awsServiceCache.environment("DOCUMENTS_IAM_URL"))) {
      SsmService ssm = awsServiceCache.getExtension(SsmService.class);

      String appEnvironment = awsServiceCache.environment("APP_ENVIRONMENT");

      String documentsIamUrl =
          ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");
      awsServiceCache.environment().put("DOCUMENTS_IAM_URL", documentsIamUrl);
    }

    logger = awsServiceCache.getLogger();
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

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

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
    this();
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

    return new DocumentEvent().siteId(site).documentId(documentId).s3bucket(s3Bucket).s3key(s3Key)
        .type(eventType).userId(doc.getUserId()).contentType(contentType).path(doc.getPath());
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
        && resp.getContentLength() < EventServiceSns.MAX_SNS_CONTENT_SIZE) {
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

    ApiAuthorization.logout();

    if (logger.isLogged(LogLevel.DEBUG)) {
      String json = this.gson.toJson(map);
      logger.debug(json);
    }

    List<Map<String, Object>> list = processRecords(map);

    for (Map<String, Object> e : list) {

      String eventName = (String) e.getOrDefault("eventName", null);
      String bucket = (String) e.getOrDefault("s3bucket", null);
      String key = (String) e.getOrDefault("s3key", null);
      String s3VersionId = (String) e.getOrDefault("s3VersionId", null);

      login(bucket, key);

      String s = String.format("{\"eventName\": \"%s\",\"bucket\": \"%s\",\"key\": \"%s\"}",
          eventName, bucket, key);
      logger.info(s);

      if (bucket != null && key != null) {

        boolean create = eventName != null && eventName.toLowerCase().contains("objectcreated");

        boolean remove = eventName != null && eventName.toLowerCase().contains("objectremove");

        logger.trace(
            String.format("processing event %s for file %s in bucket %s", eventName, bucket, key));

        try {

          if (remove) {

            processS3Delete(bucket, key);

          } else {
            processS3File(create, bucket, key, s3VersionId);
          }

        } catch (IOException | InterruptedException ex) {
          logger.error(ex);
          throw new RuntimeException(ex);

        } finally {
          ApiAuthorization.logout();
        }
      }
    }

    return null;
  }

  private void login(final String s3Bucket, final String s3Key) {

    String cacheKey = "s3PresignedUrl#" + s3Bucket + "#" + s3Key;
    String username = cacheService.read(cacheKey);
    if (!isEmpty(username)) {
      username = "System";
    }

    ApiAuthorization authorization = new ApiAuthorization().username(username);
    ApiAuthorization.login(authorization);
  }

  /**
   * Process S3 Event.
   * 
   * @param event {@link Map}
   * @return {@link Map}
   */
  private Map<String, Object> processEvent(final Map<String, Object> event) {

    Map<String, Object> map = new HashMap<>();
    String eventName = event.get("eventName").toString();

    String bucket = getBucketName(event);
    String key = getObjectKey(event);
    String versionId = getVersionId(event);
    map.put("s3bucket", bucket);
    map.put("s3key", key);
    map.put("s3VersionId", versionId);
    map.put("eventName", eventName);

    return map;
  }

  /**
   * Process Event Records.
   *
   * @param records {@link List} {@link Map}
   * @return {@link Map}
   */
  private List<Map<String, Object>> processRecords(final List<Map<String, Object>> records) {

    List<Map<String, Object>> list = new ArrayList<>();

    for (Map<String, Object> event : records) {

      if (event.containsKey("body")) {

        String body = event.get("body").toString();

        Map<String, Object> map = this.gson.fromJson(body, Map.class);
        list.addAll(processRecords(map));

      } else if (event.containsKey("eventName")) {
        list.add(processEvent(event));
      } else {
        list.addAll(processRecords(event));
      }
    }

    return list;
  }

  /**
   * Process Event Records.
   *
   * @param map {@link Map}
   * @return {@link List} {@link Map}
   */
  private List<Map<String, Object>> processRecords(final Map<String, Object> map) {

    List<Map<String, Object>> list = new ArrayList<>();

    if (map.containsKey("Records")) {
      List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      list.addAll(processRecords(records));
    } else if (map.containsKey("Message")) {
      String body = map.get("Message").toString();
      Map<String, Object> messageMap = this.gson.fromJson(body, Map.class);
      list.addAll(processRecords(messageMap));
    } else if (map.containsKey("Sns")) {
      Map<String, Object> messageMap = (Map<String, Object>) map.get("Sns");
      list.addAll(processRecords(messageMap));
    } else if (map.containsKey("s3key")) {
      list.add(map);
    }

    return list;
  }

  /**
   * Process S3 Delete Request.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  private void processS3Delete(final String bucket, final String key)
      throws IOException, InterruptedException {

    if (!s3service.getObjectMetadata(bucket, key, null).isObjectExists()) {

      String siteId = getSiteId(key);
      String documentId = resetDatabaseKey(siteId, key);

      String msg = String.format("Removing %s from bucket %s.", key, bucket);
      logger.trace(msg);

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

      sendSnsMessage(event, doc.getContentType(), bucket, key, null);
    }
  }

  /**
   * Process S3 File.
   *
   * @param create boolean
   * @param s3bucket {@link String}
   * @param s3key {@link String}
   * @param s3VersionId {@link String}
   * @throws FileNotFoundException FileNotFoundException
   */
  private void processS3File(final boolean create, final String s3bucket, final String s3key,
      final String s3VersionId) throws FileNotFoundException {

    String key = urlDecode(s3key);

    String siteId = getSiteId(key);
    String documentId = resetDatabaseKey(siteId, key);

    S3ObjectMetadata resp = s3service.getObjectMetadata(s3bucket, key, null);

    if (!resp.isObjectExists()) {
      throw new FileNotFoundException("Object " + documentId + " not found in bucket " + s3bucket);
    }

    String contentType = resp.getContentType();
    Long contentLength = resp.getContentLength();

    DocumentItem item = findDocument(siteId, documentId);

    if (item != null) {

      if (logger.isLogged(LogLevel.TRACE)) {
        logger.trace("metadata: " + resp.getMetadata());
        logger.trace("item checksum: " + resp.getChecksum());
        logger.trace("item content-type: " + resp.getContentType());
        logger.trace("content-type: " + contentType);
        logger.trace("s3 version id: " + s3VersionId);
        logger.trace("s3 file version id: " + resp.getVersionId());
      }

      Map<String, AttributeValue> attributes = buildAttributes(resp, contentType, contentLength);

      // if the event and s3 version id match, then correct event to process
      if (s3VersionId == null || s3VersionId.equals(resp.getVersionId())) {
        service.updateDocument(siteId, documentId, attributes);
        s3ServiceInterceptor.putObjectEvent(s3service, s3bucket, s3key);

        List<DocumentTag> tags = getObjectTags(s3bucket, key);
        service.addTags(siteId, documentId, tags, null);

        service.deleteDocumentFormats(siteId, documentId);
      }

      DocumentEvent event =
          buildDocumentEvent(create ? CREATE : UPDATE, siteId, item, s3bucket, key, contentType);
      sendSnsMessage(event, contentType, s3bucket, key, resp);

    } else {
      logger.error("Cannot find document " + documentId + " in site " + siteId);
    }
  }

  private static Map<String, AttributeValue> buildAttributes(final S3ObjectMetadata resp,
      final String contentType, final Long contentLength) {
    Map<String, AttributeValue> attributes = new HashMap<>();

    if (!Strings.isEmpty(contentType)) {
      attributes.put("contentType", AttributeValue.fromS(contentType));
    }

    String checksum = resp.getChecksum();
    attributes.put("checksum", AttributeValue.fromS(checksum));

    if (resp.getChecksumType() != null) {
      attributes.put("checksumType", AttributeValue.fromS(resp.getChecksumType()));
    }

    if (contentLength != null) {
      attributes.put("contentLength", AttributeValue.fromN("" + contentLength));
    }

    if (resp.getVersionId() != null) {
      attributes.put(S3VERSION_ATTRIBUTE, AttributeValue.fromS(resp.getVersionId()));
    }
    return attributes;
  }

  private DocumentItem findDocument(final String siteId, final String documentId) {
    return service.findDocument(siteId, documentId);
  }

  /**
   * Either sends the Create Message to SNS.
   *
   * @param event {@link DocumentEvent}
   * @param contentType {@link String}
   * @param s3bucket {@link String}
   * @param key {@link String}
   * @param resp {@link S3ObjectMetadata}
   */
  private void sendSnsMessage(final DocumentEvent event, final String contentType,
      final String s3bucket, final String key, final S3ObjectMetadata resp) {

    String siteId = event.siteId();
    String documentId = event.documentId();

    if ("application/json".equals(contentType)) {
      String content = getContent(s3bucket, key, resp, contentType);
      event.content(content);
    }

    PresignGetUrlConfig config = new PresignGetUrlConfig().contentType(contentType);
    URL url = s3PresignedService.presignGetUrl(s3bucket, key, Duration.ofDays(1), null, config);
    event.url(url.toString());

    // String eventJson = documentEventService.publish(event);
    // logger.trace(eventJson);

    String eventType = event.type();
    // logger.trace("publishing " + event.type() + " document message to " + snsDocumentEvent);

    if (CREATE.equals(eventType)) {
      List<Action> actions = actionsService.getActions(siteId, documentId);
      notificationService.publishNextActionEvent(actions, siteId, documentId);
    }
  }
}
