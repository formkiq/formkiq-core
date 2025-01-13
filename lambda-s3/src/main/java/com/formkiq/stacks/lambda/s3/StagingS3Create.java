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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.actions.services.DynamicObjectToAction;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.EventServiceSnsExtension;
import com.formkiq.module.http.HttpResponseStatus;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.httpsigv4.HttpServiceSigv4;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.FolderIndexProcessorExtension;
import com.formkiq.stacks.dynamodb.apimodels.MatchDocumentTag;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequest;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceExtension;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Strings.isUuid;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;

/** {@link RequestHandler} for handling Document Staging Create Events. */
@Reflectable
public class StagingS3Create implements RequestHandler<Map<String, Object>, Void> {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** {@link String}. */
  private static String documentsBucket;
  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor folderIndexProcesor;

  /** Extension for FormKiQ config file. */
  public static final String FORMKIQ_B64_EXT = ".fkb64";
  /** {@link ActionsNotificationService}. */
  private static ActionsNotificationService notificationService;
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link DocumentService}. */
  private static DocumentService service;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;
  /** SNS Document Event Arn. */
  private static String snsDocumentEvent;
  /** {@link DocumentSyncService}. */
  private static DocumentSyncService syncService = null;
  /** {@link Logger}. */
  private static Logger logger;

  static {

    if (System.getenv().containsKey("AWS_REGION")) {
      serviceCache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
              new SnsAwsServiceRegistry(), new SsmAwsServiceRegistry())
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
    Map<String, Object> s3Event = (Map<String, Object>) event.get("s3");
    Map<String, Object> bucket = (Map<String, Object>) s3Event.get("bucket");

    return bucket.get("name").toString();
  }

  /**
   * Get Object Key.
   *
   * @param event {@link Map}
   * @return {@link String}
   */
  private static String getObjectKey(final Map<String, Object> event) {
    Map<String, Object> s3Event = (Map<String, Object>) event.get("s3");
    Map<String, Object> object = (Map<String, Object>) s3Event.get("object");

    return object.get("key").toString();
  }

  /**
   * Initialize.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   */
  static void initialize(final AwsServiceCache awsServiceCache) {

    awsServiceCache.register(S3Service.class, new S3ServiceExtension());
    awsServiceCache.register(SsmService.class, new SsmServiceExtension());
    awsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    awsServiceCache.register(DocumentSearchService.class, new DocumentSearchServiceExtension());
    awsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServiceCache.register(DocumentSyncService.class, new DocumentSyncServiceExtension());
    awsServiceCache.register(ActionsService.class, new ActionsServiceExtension());
    awsServiceCache.register(FolderIndexProcessor.class, new FolderIndexProcessorExtension());
    awsServiceCache.register(EventService.class, new EventServiceSnsExtension());
    awsServiceCache.register(ActionsNotificationService.class,
        new ActionsNotificationServiceExtension());
    awsServiceCache.register(DynamoDbService.class, new DynamoDbServiceExtension());
    awsServiceCache.register(AttributeService.class, new AttributeServiceExtension());

    documentsBucket = awsServiceCache.environment("DOCUMENTS_S3_BUCKET");
    syncService = awsServiceCache.getExtension(DocumentSyncService.class);

    service = awsServiceCache.getExtension(DocumentService.class);
    actionsService = awsServiceCache.getExtension(ActionsService.class);
    s3 = awsServiceCache.getExtension(S3Service.class);

    snsDocumentEvent = awsServiceCache.environment("SNS_DOCUMENT_EVENT");
    notificationService = awsServiceCache.getExtension(ActionsNotificationService.class);
    folderIndexProcesor = awsServiceCache.getExtension(FolderIndexProcessor.class);

    if (isEmpty(awsServiceCache.environment("DOCUMENTS_IAM_URL"))) {
      SsmService ssm = awsServiceCache.getExtension(SsmService.class);
      String appEnvironment = awsServiceCache.environment("APP_ENVIRONMENT");
      String documentsIamUrl =
          ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

      awsServiceCache.environment().put("DOCUMENTS_IAM_URL", documentsIamUrl);
    }

    AwsCredentials awsCredentials = awsServiceCache.getExtension(AwsCredentials.class);
    awsServiceCache.register(HttpService.class, new ClassServiceExtension<>(
        new HttpServiceSigv4(awsServiceCache.region(), awsCredentials)));
    logger = awsServiceCache.getLogger();
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

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   *
   */
  public StagingS3Create() {
    // empty
  }

  /**
   * constructor.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public StagingS3Create(final AwsServiceCache awsServiceCache) {
    initialize(awsServiceCache);
    serviceCache = awsServiceCache;
  }

  /**
   * Generate {@link Map} of DocumentId / Content.
   *
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
   *
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

  private DynamicDocumentItem createDocument(final String siteId,
      final DynamicDocumentItem loadedDoc, final boolean hasContent,
      final DocumentItem existingDocument) throws ValidationException {

    DynamicDocumentItem doc = new DynamicDocumentItem(loadedDoc);

    doc = updateFromExistingDocument(doc, existingDocument);

    // saving service will set these
    doc.setInsertedDate(null);
    doc.setLastModifiedDate(null);

    if (hasContent) {
      doc.setContentLength(null);
      doc.setChecksum(ID.uuid());
      doc.setDeepLinkPath(null);
    } else if (doc.getChecksum() == null) {
      doc.setChecksum(ID.uuid());
    }

    if (isEmpty(doc.getPath())) {
      doc.setPath(doc.getDocumentId());
    }

    service.saveDocumentItemWithTag(siteId, doc);

    saveDocumentSync(siteId, doc, existingDocument);

    saveDocumentActions(siteId, doc);

    return doc;
  }

  /**
   * Delete S3 Object.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   */
  private void deleteObject(final String bucket, final String key) {
    String msg = String.format("Removing %s from bucket %s.", key, bucket);
    logger.trace(msg);
    s3.deleteObject(bucket, key, null);
  }

  /**
   * Get {@link String} content from an S3 Bucket / Key.
   *
   * @param bucket {@link String}
   * @param s3Key {@link String}
   * @return {@link String}
   */
  private String getContentFromS3(final String bucket, final String s3Key) {
    String s = s3.getContentAsString(bucket, s3Key, null);
    logger.trace(s);
    return s;
  }

  /**
   * Find DocumentId for File Path.
   *
   * @param siteId {@link String}
   * @param path {@link String}
   * @return {@link String}
   */
  private String getDocumentIdForPath(final String siteId, final String path) {

    String documentId;

    try {

      Map<String, String> index = folderIndexProcesor.getIndex(siteId, path);
      documentId = index.getOrDefault("documentId", ID.uuid());

    } catch (IOException e) {
      documentId = ID.uuid();
    }

    return documentId;
  }

  /**
   * Handle Document Compression Request.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @throws IOException IOException
   */
  private void handleCompressionRequest(final String bucket, final String key) throws IOException {

    final String contentString = s3.getContentAsString(bucket, key, null);
    Type mapStringObject = new TypeToken<Map<String, Object>>() {}.getType();
    final Map<String, Object> content = this.gson.fromJson(contentString, mapStringObject);
    final String siteId = content.get("siteId").toString();
    final String archiveKey = key.replace(".json", ".zip");
    Type jsonStringList = new TypeToken<List<String>>() {}.getType();
    final List<String> documentIds =
        this.gson.fromJson(content.get("documentIds").toString(), jsonStringList);

    DocumentCompressor documentCompressor = new DocumentCompressor(serviceCache);
    documentCompressor.compressDocuments(siteId, documentsBucket, bucket, archiveKey, documentIds);
  }

  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json;
    Date date = new Date();

    if (logger.isLogged(LogLevel.DEBUG)) {
      json = this.gson.toJson(map);
      logger.debug(json);
    }

    try {
      List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      processRecords(date, records);
    } catch (RuntimeException e) {
      logger.error(e);
      throw e;
    }

    return null;
  }

  /**
   * Loads Document from Bucket / DB.
   *
   * @param bucket {@link String}
   * @param siteId {@link String}
   * @param s3Key {@link String}
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem loadDocument(final String bucket, final String siteId,
      final String s3Key) {

    DynamicDocumentItem doc;

    if (s3Key.endsWith(FORMKIQ_B64_EXT)) {

      String s = getContentFromS3(bucket, s3Key);

      Map<String, Object> map = this.gson.fromJson(s, Map.class);
      doc = new DynamicDocumentItem(map);

    } else {
      doc = new DynamicDocumentItem(Collections.emptyMap());

      String key = resetDatabaseKey(siteId, s3Key);
      boolean uuid = isUuid(key);

      if (!uuid) {
        doc.setPath(key);
      }
    }

    updateDocumentId(siteId, s3Key, doc);

    if (isEmpty(doc.getUserId())) {

      S3ObjectMetadata metadata = s3.getObjectMetadata(bucket, s3Key, null);
      String username = metadata.getMetadata().entrySet().stream()
          .filter(s -> s.getKey().equalsIgnoreCase("userid")).findFirst().map(Map.Entry::getValue)
          .orElse("System");

      doc.setUserId(username);
    }

    updateContentLength(doc);

    return doc;
  }

  /**
   * If document has a tagschema that needs to be processed.
   *
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @throws IOException IOException
   */
  private void postDocumentTags(final String siteId, final DynamicDocumentItem doc)
      throws IOException {

    List<DynamicObject> tags = doc.getList("tags");

    if (!tags.isEmpty()) {

      String documentId = doc.getDocumentId();
      String url =
          serviceCache.environment("DOCUMENTS_IAM_URL") + "/documents/" + documentId + "/tags";
      HttpService http = serviceCache.getExtension(HttpService.class);
      Optional<Map<String, String>> parameters =
          siteId != null ? Optional.of(Map.of("siteId", siteId)) : Optional.empty();
      HttpResponse<String> response =
          http.post(url, Optional.empty(), parameters, this.gson.toJson(Map.of("tags", tags)));

      if (!HttpResponseStatus.is2XX(response)) {
        throw new IOException(url + " returned " + response.statusCode());
      }
    }
  }

  /**
   * Default File Processor.
   *
   * @param siteId {@link String}
   * @param bucket {@link String}
   * @param s3Key {@link String}
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  private void processDefaultFile(final String siteId, final String bucket, final String s3Key)
      throws IOException, ValidationException {

    DynamicDocumentItem loadDocument = loadDocument(bucket, siteId, s3Key);

    Map<String, String> contentMap = createContentMap(loadDocument);
    Map<String, String> contentTypeMap = createContentTypeMap(loadDocument);

    boolean hasContent = !contentMap.isEmpty();

    DocumentItem existingDocument = service.findDocument(siteId, loadDocument.getDocumentId());
    DynamicDocumentItem item = createDocument(siteId, loadDocument, hasContent, existingDocument);

    String tagSchemaId = item.getString("tagSchemaId");
    Boolean newCompositeTags = item.getBoolean("newCompositeTags");

    if (!StringUtils.isEmpty(tagSchemaId) && Boolean.FALSE.equals(newCompositeTags)) {
      postDocumentTags(siteId, item);
    }

    writeS3Document(bucket, s3Key, siteId, item, contentMap, contentTypeMap);

    if (contentMap.isEmpty()) {
      logger.trace(String.format("Skipping %s no content", item.getPath()));
    }

    if (existingDocument != null && !hasContent) {

      List<Action> actions = actionsService.getActions(siteId, item.getDocumentId());
      List<Action> syncs =
          actions.stream().filter(a -> ActionType.FULLTEXT.equals(a.type())).toList();
      syncs.forEach(a -> a.status(ActionStatus.PENDING));
      actionsService.saveNewActions(siteId, item.getDocumentId(), actions);

      logger.trace("publishing actions message to " + snsDocumentEvent);
      notificationService.publishNextActionEvent(actions, siteId, item.getDocumentId());
    }

    deleteObject(bucket, s3Key);
  }

  /**
   * Process S3 Event.
   *
   * @param date {@link Date}
   * @param event {@link Map}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  private void processEvent(final Date date, final Map<String, Object> event) throws Exception {

    String bucket = getBucketName(event);

    String key = getObjectKey(event);
    String s3Key = urlDecode(key);
    String siteId = getSiteId(s3Key);

    String eventName = event.get("eventName").toString();
    boolean objectCreated = eventName.contains("ObjectCreated") && !s3Key.endsWith("/");

    String s = String.format("{\"eventName\": \"%s\",\"bucket\": \"%s\",\"key\": \"%s\"}",
        eventName, bucket, key);
    logger.info(s);

    final String tempFolder = "tempfiles/";
    if (objectCreated && key.startsWith(tempFolder)) {

      if (Strings.getExtension(key).equals("json")) {
        this.handleCompressionRequest(bucket, key);
      } else {
        logger.trace(String.format("skipping event for key %s", key));
      }

    } else if (objectCreated) {
      if (s3Key.contains("patch_documents_tags_") && s3Key.endsWith(FORMKIQ_B64_EXT)) {
        processPatchDocumentsTags(siteId, bucket, s3Key, date);
      } else {
        processDefaultFile(siteId, bucket, s3Key);
      }
    }

    if (!objectCreated) {
      logger.trace("skipping event " + eventName);
    }
  }

  /**
   * Process Files generated from PATCH /documents/tags.
   *
   * @param siteId {@link String}
   * @param bucket {@link String}
   * @param s3Key {@link String}
   * @param date {@link Date}
   */
  private void processPatchDocumentsTags(final String siteId, final String bucket,
      final String s3Key, final Date date) {

    GetObjectTaggingResponse objectTags = s3.getObjectTags(bucket, s3Key);
    String user = objectTags.tagSet().stream().filter(t -> t.key().equals("userId")).findFirst()
        .orElse(Tag.builder().value("System").build()).value();

    String s = getContentFromS3(bucket, s3Key);

    UpdateMatchingDocumentTagsRequest request =
        this.gson.fromJson(s, UpdateMatchingDocumentTagsRequest.class);

    MatchDocumentTag matchTag = request.getMatch().getTag();
    logger.trace("matching Tag: " + request.getMatch().getTag() + " eq: " + matchTag.getEq()
        + " beginsWith: " + matchTag.getBeginsWith());

    SearchTagCriteria query = new SearchTagCriteria().key(matchTag.getKey()).eq(matchTag.getEq())
        .beginsWith(matchTag.getBeginsWith());

    runPatchDocumentsTags(siteId, request, query, date, user);

    s3.deleteObject(bucket, s3Key, null);
  }

  /**
   * Process Event Records.
   *
   * @param date {@link Date}
   * @param records {@link List} {@link Map}
   */
  private void processRecords(final Date date, final List<Map<String, Object>> records) {

    for (Map<String, Object> event : Objects.notNull(records)) {

      if (event.containsKey("body")) {

        String body = event.get("body").toString();

        Map<String, Object> map = this.gson.fromJson(body, Map.class);
        processRecords(date, (List<Map<String, Object>>) map.get("Records"));

      } else {
        logger.trace("handling " + records.size() + " record(s).");

        try {
          processEvent(date, event);
        } catch (Exception e) {
          logger.error(e);
        }
      }
    }
  }

  private void runPatchDocumentsTags(final String siteId,
      final UpdateMatchingDocumentTagsRequest request, final SearchTagCriteria query,
      final Date date, final String user) {

    PaginationMapToken token = null;
    final int maxresults = 100;


    DocumentSearchService searchService = serviceCache.getExtension(DocumentSearchService.class);

    List<com.formkiq.stacks.dynamodb.apimodels.AddDocumentTag> addTags =
        request.getUpdate().getTags();

    do {
      PaginationResults<String> results =
          searchService.searchForDocumentIds(siteId, query, token, maxresults);
      token = results.getToken();

      List<String> documentIds = results.getResults();
      logger.trace("found: " + documentIds.size() + " matching documents");

      Map<String, Collection<DocumentTag>> tagMap = new HashMap<>();

      for (String documentId : documentIds) {

        List<DocumentTag> tags = addTags.stream()
            .map(t -> new DocumentTag(documentId, t.getKey(), t.getValue(), date, user))
            .collect(Collectors.toList());
        tagMap.put(documentId, tags);
      }

      service.addTags(siteId, tagMap, null);

    } while (token != null);
  }

  /**
   * Save Actions.
   *
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   */
  private void saveDocumentActions(final String siteId, final DynamicDocumentItem doc) {
    if (doc.containsKey("actions")) {

      actionsService.deleteActions(siteId, doc.getDocumentId());

      DynamicObjectToAction transform = new DynamicObjectToAction();
      List<DynamicObject> list = doc.getList("actions");
      List<Action> actions = list.stream().map(transform).collect(Collectors.toList());

      actionsService.saveNewActions(siteId, doc.getDocumentId(), actions);
    }
  }

  /**
   * Save Document Sync.
   *
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @param existingDocument {@link DocumentItem}
   */
  private void saveDocumentSync(final String siteId, final DynamicDocumentItem doc,
      final DocumentItem existingDocument) {
    String agent = doc.getString("agent");

    if (DocumentSyncServiceType.FORMKIQ_CLI.name().equals(agent)) {
      String message = existingDocument != null ? DocumentSyncService.MESSAGE_UPDATED_CONTENT
          : DocumentSyncService.MESSAGE_ADDED_CONTENT;
      syncService.saveSync(siteId, doc.getDocumentId(), DocumentSyncServiceType.FORMKIQ_CLI,
          DocumentSyncStatus.COMPLETE, DocumentSyncType.CONTENT, doc.getUserId(), message);
    }
  }

  private void updateContentLength(final DynamicDocumentItem obj) {

    if (obj.containsKey("contentLength") && obj.get("contentLength") instanceof Double) {
      long contentLength = obj.getDouble("contentLength").longValue();
      obj.setContentLength(contentLength);
    }
  }

  /**
   * Update {@link DynamicDocumentItem} DocumentId.
   * 
   * @param siteId {@link String}
   * @param s3Key {@link String}
   * @param obj {@link DynamicDocumentItem}
   */
  private void updateDocumentId(final String siteId, final String s3Key,
      final DynamicDocumentItem obj) {

    if (isEmpty(obj.getDocumentId())) {

      if (obj.getPath() != null) {

        String documentId = getDocumentIdForPath(siteId, obj.getPath());
        obj.setDocumentId(documentId);

      } else {

        String key = resetDatabaseKey(siteId, s3Key);

        boolean uuid = isUuid(key);
        String documentId = uuid ? key : getDocumentIdForPath(siteId, key);
        obj.setDocumentId(documentId);
      }
    }
  }

  /**
   * Update from existing Document.
   * 
   * @param obj {@link DynamicDocumentItem}
   * @param existingDocument {@link DocumentItem}
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem updateFromExistingDocument(final DynamicDocumentItem obj,
      final DocumentItem existingDocument) {

    DynamicDocumentItem response;

    if (existingDocument != null) {
      DynamicDocumentItem origMap = new DocumentItemToDynamicDocumentItem().apply(existingDocument);
      origMap.putAll(obj);
      response = origMap;
    } else {
      response = obj;
    }

    return response;
  }

  private void writeS3Document(final String bucket, final String s3Key, final String siteId,
      final DynamicDocumentItem item, final Map<String, String> contentMap,
      final Map<String, String> contentTypeMap) {

    Map<String, String> map = Map.of("checksum", item.getChecksum());

    if (s3Key.endsWith(FORMKIQ_B64_EXT)) {

      for (Map.Entry<String, String> e : contentMap.entrySet()) {

        boolean isBase64 = item.getBoolean("isBase64");
        byte[] bytes =
            isBase64 ? Base64.getDecoder().decode(e.getValue().getBytes(StandardCharsets.UTF_8))
                : e.getValue().getBytes(StandardCharsets.UTF_8);

        String key = createDatabaseKey(siteId, e.getKey());
        String contentType = contentTypeMap.get(e.getKey());

        logger.trace(String.format("Inserted %s into bucket %s as %s", item.getPath(),
            documentsBucket, createDatabaseKey(siteId, item.getDocumentId())));

        s3.putObject(documentsBucket, key, bytes, contentType, map);
      }

    } else {

      S3ObjectMetadata metadata = s3.getObjectMetadata(bucket, s3Key, null);

      String destKey = createDatabaseKey(siteId, item.getDocumentId());

      logger.trace(String.format("Copying %s from bucket %s to %s in bucket %s.", s3Key, bucket,
          destKey, documentsBucket));

      s3.copyObject(bucket, s3Key, documentsBucket, destKey, metadata.getContentType(), map);
    }
  }
}
