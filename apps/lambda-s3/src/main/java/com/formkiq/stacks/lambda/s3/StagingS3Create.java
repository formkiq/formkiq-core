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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.actions.AddActionsToAction;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.actions.AddAction;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.model.DocumentTagRecord;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
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
import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.aws.dynamodb.actions.ActionBuilder;
import com.formkiq.aws.dynamodb.actions.ActionStatus;
import com.formkiq.aws.dynamodb.actions.ActionType;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.EventServiceSnsExtension;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.httpsigv4.HttpServiceSigv4;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.stacks.dynamodb.documents.AddDocumentAttribute;
import com.formkiq.stacks.dynamodb.documents.AddDocumentAttributeDeserializer;
import com.formkiq.stacks.dynamodb.documents.AddDocumentRequest;
import com.formkiq.stacks.dynamodb.documents.AddDocumentRequestToDocumentRecordSet;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorExtension;
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
import software.amazon.awssdk.utils.http.SdkHttpUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
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
        new HttpServiceSigv4(awsServiceCache.region(), awsCredentials, "execute-api")));
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

  private static boolean writeS3File(final String siteId, final AddDocumentRequest loadDocument) {

    boolean hasContent = !isEmpty(loadDocument.getContent());

    if (hasContent) {

      byte[] bytes = loadDocument.isBase64()
          ? Base64.getDecoder().decode(loadDocument.getContent().getBytes(StandardCharsets.UTF_8))
          : loadDocument.getContent().getBytes(StandardCharsets.UTF_8);

      String documentId = loadDocument.getDocumentId();
      logger.trace(String.format("Inserted %s into bucket %s as %s", loadDocument.getPath(),
          documentsBucket, createDatabaseKey(siteId, documentId)));

      String key = createS3Key(siteId, loadDocument.getDocumentId(), null);
      Map<String, String> map = Map.of("checksum", loadDocument.getChecksum());
      s3.putObject(documentsBucket, key, bytes, loadDocument.getContentType(), map);
    } else {
      logger.trace(String.format("Skipping %s no content", loadDocument.getPath()));
    }

    return hasContent;
  }

  /** {@link Gson}. */
  private final Gson gson =
      new GsonBuilder().registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
          .registerTypeAdapter(AddDocumentAttribute.class, new AddDocumentAttributeDeserializer())
          .create();

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

  private void createDocument(final String siteId, final DocumentArtifact document,
      final AddDocumentRequest loadedDoc, final DocumentRecord existingDocument)
      throws ValidationException {

    String userId = loadedDoc.getUserId();
    DocumentRecordSet recordSet =
        new AddDocumentRequestToDocumentRecordSet(serviceCache, existingDocument, userId)
            .apply(siteId, loadedDoc);

    SaveDocumentOptions options = new SaveDocumentOptions().saveDocumentDate(true);
    service.saveDocument(siteId, recordSet, options);

    saveDocumentSync(siteId, recordSet.documentRecord().documentId(), loadedDoc.getAgent(),
        existingDocument);

    saveDocumentActions(siteId, document, notNull(loadedDoc.getActions()));
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

      Map<String, Object> index = folderIndexProcesor.getIndex(siteId, path);
      documentId = (String) index.getOrDefault("documentId", ID.uuid());

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

    List<DocumentArtifact> documents =
        documentIds.stream().map(d -> DocumentArtifact.of(d, null)).toList();

    DocumentCompressor documentCompressor = new DocumentCompressor(serviceCache);
    documentCompressor.compressDocuments(siteId, documentsBucket, bucket, archiveKey, documents);
  }

  /**
   * Handle Document Event Callback.
   * 
   * @param s3Key {@link String}
   */
  private void handleEventCallBack(final String s3Key) {
    String key = s3Key.replace("tempfiles/eventcallback/", "");
    SiteIdKeyGenerator.S3KeyParts parts = SiteIdKeyGenerator.getS3KeyParts(key);
    notificationService.publishNextActionEvent(parts.siteId(), parts.documentId(),
        parts.artifactId());
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
  private AddDocumentRequest loadDocument(final String bucket, final String siteId,
      final String s3Key) {

    AddDocumentRequest request;

    if (s3Key.endsWith(FORMKIQ_B64_EXT)) {

      String s = getContentFromS3(bucket, s3Key);
      request = this.gson.fromJson(s, AddDocumentRequest.class);

    } else {
      request = new AddDocumentRequest();

      String documentId = resetDatabaseKey(siteId, s3Key);
      // String documentId = parts.documentId();

      if (isUuid(documentId)) {
        request.setDocumentId(documentId);
      } else {
        request.setPath(documentId);
      }
    }

    // updateDocumentId(siteId, s3Key, request);
    // updateDocumentPath()

    S3ObjectMetadata metadata = s3.getObjectMetadata(bucket, s3Key, null);
    String username = metadata.getMetadata().entrySet().stream()
        .filter(s -> s.getKey().equalsIgnoreCase("userid")).findFirst().map(Map.Entry::getValue)
        .orElse("System");

    updateIfMissing(siteId, request, username, true);
    notNull(request.getDocuments()).forEach(d -> updateIfMissing(siteId, d, username, false));

    ApiAuthorization.login(new ApiAuthorization().username(request.getUserId()));

    return request;
  }

  /**
   * Default File Processor.
   *
   * @param siteId {@link String}
   * @param bucket {@link String}
   * @param s3Key {@link String}
   * @throws ValidationException ValidationException
   */
  private void processDefaultFile(final String siteId, final String bucket, final String s3Key)
      throws ValidationException {

    AddDocumentRequest loadDocument = loadDocument(bucket, siteId, s3Key);
    String artifactId = loadDocument.isArtifacts() ? ID.ulid() : null;
    DocumentArtifact document = DocumentArtifact.of(loadDocument.getDocumentId(), artifactId);

    DocumentRecord existingDocument = service.findDocument(siteId, document);
    createDocument(siteId, document, loadDocument, existingDocument);

    boolean hasContent = writeS3Document(bucket, s3Key, siteId, document, loadDocument);

    if (existingDocument != null && !hasContent) {

      List<Action> actions = actionsService.getActions(siteId, document);
      List<Action> syncs = actions.stream().filter(a -> ActionType.FULLTEXT.equals(a.type()))
          .map(a -> new ActionBuilder().action(a).insertedDate(new Date()).indexUlid()
              .status(ActionStatus.PENDING).build(siteId))
          .toList();

      actionsService.saveNewActions(syncs);

      logger.trace("publishing actions message to " + snsDocumentEvent);
      notificationService.publishNextActionEvent(siteId, document.documentId(), null);
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

      if (key.startsWith("tempfiles/eventcallback/")) {
        handleEventCallBack(key);
      } else if (Strings.getExtension(key).equals("json")) {
        handleCompressionRequest(bucket, key);
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

    SearchTagCriteria query = new SearchTagCriteria(matchTag.getKey(), matchTag.getBeginsWith(),
        matchTag.getEq(), null, null);

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

    for (Map<String, Object> event : notNull(records)) {

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
        } finally {
          ApiAuthorization.logout();
        }
      }
    }
  }

  private void runPatchDocumentsTags(final String siteId,
      final UpdateMatchingDocumentTagsRequest request, final SearchTagCriteria query,
      final Date date, final String user) {

    String token = null;
    final int maxresults = 100;

    DocumentSearchService searchService = serviceCache.getExtension(DocumentSearchService.class);

    List<com.formkiq.stacks.dynamodb.apimodels.AddDocumentTag> addTags =
        request.getUpdate().getTags();

    do {
      Pagination<String> results =
          searchService.searchForDocumentIds(siteId, query, token, maxresults);
      token = results.getNextToken();

      List<String> documentIds = results.getResults();
      logger.trace("found: " + documentIds.size() + " matching documents");

      Map<DocumentArtifact, Collection<DocumentTagRecord>> tagMap = new HashMap<>();

      for (String documentId : documentIds) {

        List<DocumentTagRecord> tags = addTags.stream()
            .flatMap(t -> DocumentTagRecord.builder().documentId(documentId).tagKey(t.getKey())
                .tagValue(t.getValue()).insertedDate(date).userId(user).build(siteId).stream())
            .toList();
        tagMap.put(DocumentArtifact.of(documentId, null), tags);
      }

      service.addTags(siteId, tagMap, null);

    } while (token != null);
  }

  /**
   * Save Actions.
   *
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param actions {@link AddAction}
   */
  private void saveDocumentActions(final String siteId, final DocumentArtifact document,
      final List<AddAction> actions) {
    if (!actions.isEmpty()) {

      actionsService.deleteActions(siteId, document);

      AddActionsToAction toAction = new com.formkiq.aws.dynamodb.actions.AddActionsToAction();
      List<Action> addActions =
          actions.stream().map(a -> toAction.apply(siteId, document, a)).toList();
      actionsService.saveNewActions(addActions);
    }
  }

  /**
   * Save Document Sync.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param agent {@link String}
   * @param existingDocument {@link DocumentItem}
   */
  private void saveDocumentSync(final String siteId, final String documentId, final String agent,
      final DocumentRecord existingDocument) {
    if (DocumentSyncServiceType.FORMKIQ_CLI.name().equals(agent)) {
      syncService.saveSync(siteId, documentId, DocumentSyncServiceType.FORMKIQ_CLI,
          DocumentSyncStatus.COMPLETE, DocumentSyncType.CONTENT, existingDocument != null);
    }
  }

  private void updateIfMissing(final String siteId, final AddDocumentRequest request,
      final String username, final boolean documentIdPathLookup) {

    if (isEmpty(request.getDocumentId())) {

      if (documentIdPathLookup) {
        request.setDocumentId(getDocumentIdForPath(siteId, request.getPath()));
      } else {
        request.setDocumentId(ID.uuid());
      }
    }

    if (isEmpty(request.getChecksum())) {
      request.setChecksum(ID.uuid());
    }

    if (isEmpty(request.getUserId())) {
      request.setUserId(username);
    }
  }

  private boolean writeS3Document(final String bucket, final String s3Key, final String siteId,
      final DocumentArtifact document, final AddDocumentRequest loadDocument) {

    boolean hasContent = false;

    if (s3Key.endsWith(FORMKIQ_B64_EXT)) {

      if (writeS3File(siteId, loadDocument)) {
        hasContent = true;
      }

      List<AddDocumentRequest> addDocumentRequests = notNull(loadDocument.getDocuments());
      for (AddDocumentRequest addDocumentRequest : addDocumentRequests) {
        if (writeS3File(siteId, addDocumentRequest)) {
          hasContent = true;
        }
      }

    } else {

      S3ObjectMetadata metadata = s3.getObjectMetadata(bucket, s3Key, null);

      String destKey = createS3Key(siteId, document);

      logger.trace(String.format("Copying %s from bucket %s to %s in bucket %s.", s3Key, bucket,
          destKey, documentsBucket));

      Map<String, String> map = Map.of("checksum", loadDocument.getChecksum());
      s3.copyObject(bucket, s3Key, documentsBucket, destKey, metadata.getContentType(), map);
      hasContent = true;
    }

    return hasContent;
  }
}
