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
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
import com.formkiq.graalvm.annotations.ReflectableField;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceImpl;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.actions.services.DynamicObjectToAction;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddDocumentTag;
import com.formkiq.stacks.client.requests.AddDocumentTagRequest;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.FolderIndexProcessorImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/** {@link RequestHandler} for handling Document Staging Create Events. */
@Reflectable
@ReflectableImport(classes = {DocumentItemDynamoDb.class, DocumentTag.class, DocumentMetadata.class,
    DocumentTagType.class, AddDocumentTag.class, DocumentVersionServiceDynamoDb.class,
    DocumentVersionServiceNoVersioning.class})
@ReflectableClass(className = AddDocumentTagRequest.class, allPublicConstructors = true,
    fields = {@ReflectableField(name = "tag"), @ReflectableField(name = "tags")})
@ReflectableClass(className = AddDocumentTagRequest.class, allPublicConstructors = true,
    fields = {@ReflectableField(name = "tag"), @ReflectableField(name = "tags")})
@ReflectableClass(className = AddDocumentTag.class, allPublicConstructors = true,
    fields = {@ReflectableField(name = "key"), @ReflectableField(name = "value"),
        @ReflectableField(name = "values")})
public class StagingS3Create implements RequestHandler<Map<String, Object>, Void> {

  /** Extension for FormKiQ config file. */
  public static final String FORMKIQ_B64_EXT = ".fkb64";

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

  /** {@link ActionsService}. */
  private ActionsService actionsService;
  /** App Environment. */
  private String appEnvironment;
  /** {@link AwsCredentials}. */
  private AwsCredentials credentials;
  /** {@link String}. */
  private String documentsBucket;
  /** IAM Documents Url. */
  private String documentsIamUrl = null;
  /** {@link FolderIndexProcessor}. */
  private FolderIndexProcessor folderIndexProcesor;
  /** {@link FormKiqClient}. */
  private FormKiqClientV1 formkiqClient = null;
  /** {@link DocumentSyncService}. */
  private DocumentSyncService syncService = null;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link ActionsNotificationService}. */
  private ActionsNotificationService notificationService;
  /** {@link Region}. */
  private Region region;
  /** {@link S3Service}. */
  private S3Service s3;
  /** {@link DocumentService}. */
  private DocumentService service;
  /** SNS Document Event Arn. */
  private String snsDocumentEvent;
  /** {@link SsmConnectionBuilder}. */
  private SsmConnectionBuilder ssmConnection;

  /**
   * constructor.
   * 
   */
  public StagingS3Create() {
    this(System.getenv(), EnvironmentVariableCredentialsProvider.create().resolveCredentials(),
        new DynamoDbConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new S3ConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SsmConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SnsConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param awsCredentials {@link AwsCredentials}
   * @param dbBuilder {@link DynamoDbConnectionBuilder}
   * @param s3Builder {@link S3ConnectionBuilder}
   * @param ssmConnectionBuilder {@link SsmConnectionBuilder}
   * @param snsBuilder {@link SnsConnectionBuilder}
   * @throws Exception Exception
   */
  protected StagingS3Create(final Map<String, String> map, final AwsCredentials awsCredentials,
      final DynamoDbConnectionBuilder dbBuilder, final S3ConnectionBuilder s3Builder,
      final SsmConnectionBuilder ssmConnectionBuilder, final SnsConnectionBuilder snsBuilder) {

    this.region = Region.of(map.get("AWS_REGION"));
    this.credentials = awsCredentials;

    String documentsTable = map.get("DOCUMENTS_TABLE");

    AwsServiceCache serviceCache = new AwsServiceCache().environment(map);
    AwsServiceCache.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(dbBuilder));
    DocumentVersionServiceExtension dsExtension = new DocumentVersionServiceExtension();
    DocumentVersionService versionService = dsExtension.loadService(serviceCache);

    DocumentSyncServiceExtension syncExtension = new DocumentSyncServiceExtension();
    this.syncService = syncExtension.loadService(serviceCache);

    this.service = new DocumentServiceImpl(dbBuilder, documentsTable, versionService);
    this.actionsService = new ActionsServiceDynamoDb(dbBuilder, documentsTable);
    this.s3 = new S3Service(s3Builder);
    this.ssmConnection = ssmConnectionBuilder;
    this.snsDocumentEvent = map.get("SNS_DOCUMENT_EVENT");
    this.notificationService =
        new ActionsNotificationServiceImpl(this.snsDocumentEvent, snsBuilder);
    this.folderIndexProcesor = new FolderIndexProcessorImpl(dbBuilder, documentsTable);

    this.documentsBucket = map.get("DOCUMENTS_S3_BUCKET");
    this.appEnvironment = map.get("APP_ENVIRONMENT");
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

  private DynamicDocumentItem createDocument(final LambdaLogger logger, final String siteId,
      final DynamicDocumentItem loadedDoc, final Date date, final boolean hasContent,
      final DocumentItem existingDocument) {

    DynamicDocumentItem doc = new DynamicDocumentItem(loadedDoc);

    doc = updateFromExistingDocument(siteId, doc, existingDocument);

    // saving service will set these
    doc.setInsertedDate(null);
    doc.setLastModifiedDate(null);

    if (hasContent) {
      doc.setContentLength(null);
      doc.setChecksum(UUID.randomUUID().toString());
    } else if (doc.getChecksum() == null) {
      doc.setChecksum(UUID.randomUUID().toString());
    }

    if (isEmpty(doc.getPath())) {
      doc.setPath(doc.getDocumentId());
    }

    this.service.saveDocumentItemWithTag(siteId, doc);

    saveDocumentSync(siteId, doc, existingDocument);

    saveActions(siteId, doc);

    return doc;
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
      this.syncService.saveSync(siteId, doc.getDocumentId(), DocumentSyncServiceType.FORMKIQ_CLI,
          DocumentSyncStatus.COMPLETE, DocumentSyncType.CONTENT, doc.getUserId(), message);
    }
  }

  /**
   * Save Actions.
   * 
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   */
  private void saveActions(final String siteId, final DynamicDocumentItem doc) {
    if (doc.containsKey("actions")) {

      this.actionsService.deleteActions(siteId, doc.getDocumentId());

      DynamicObjectToAction transform = new DynamicObjectToAction();
      List<DynamicObject> list = doc.getList("actions");
      List<Action> actions =
          list.stream().map(s -> transform.apply(s)).collect(Collectors.toList());

      this.actionsService.saveActions(siteId, doc.getDocumentId(), actions);
    }
  }

  /**
   * Build connection to the IAM API url.
   */
  private void createFormKiQConnectionIfNeeded() {

    if (this.documentsIamUrl == null) {
      final int cacheTime = 5;
      SsmService ssmService = new SsmServiceCache(this.ssmConnection, cacheTime, TimeUnit.MINUTES);

      try (SsmClient ssmClient = this.ssmConnection.build()) {
        this.documentsIamUrl = ssmService
            .getParameterValue("/formkiq/" + this.appEnvironment + "/api/DocumentsIamUrl");
      }
    }

    if (this.formkiqClient == null) {
      FormKiqClientConnection fkqConnection =
          new FormKiqClientConnection(this.documentsIamUrl).region(this.region);

      if (this.credentials != null) {
        fkqConnection = fkqConnection.credentials(this.credentials);
      }

      this.formkiqClient = new FormKiqClientV1(fkqConnection);
    }
  }

  /**
   * Delete S3 Object.
   *
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param key {@link String}
   */
  private void deleteObject(final LambdaLogger logger, final String bucket, final String key) {
    String msg = String.format("Removing %s from bucket %s.", key, bucket);
    logger.log(msg);
    this.s3.deleteObject(bucket, key, null);
  }

  /**
   * Find DocumentId for File Path.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @return {@link String}
   * @throws IOException IOException
   */
  private String getDocumentIdForPath(final String siteId, final String path) {

    String documentId = null;

    try {

      Map<String, String> index = this.folderIndexProcesor.getIndex(siteId, path);
      documentId = index.getOrDefault("documentId", UUID.randomUUID().toString());

    } catch (IOException e) {
      documentId = UUID.randomUUID().toString();
    }

    return documentId;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json = null;
    Date date = new Date();

    LambdaLogger logger = context.getLogger();

    if ("true".equals(System.getenv("DEBUG"))) {
      json = this.gson.toJson(map);
      logger.log(json);
    }

    List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
    processRecords(logger, date, records);

    return null;
  }

  /**
   * Loads Document from Bucket / DB.
   * 
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param siteId {@link String}
   * @param s3Key {@link String}
   * @return {@link DynamicDocumentItem}
   */
  @SuppressWarnings("unchecked")
  private DynamicDocumentItem loadDocument(final LambdaLogger logger, final String bucket,
      final String siteId, final String s3Key) {

    DynamicDocumentItem doc = null;

    if (s3Key.endsWith(FORMKIQ_B64_EXT)) {

      String s = this.s3.getContentAsString(bucket, s3Key, null);

      if ("true".equals(System.getenv("DEBUG"))) {
        logger.log(s);
      }

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

      S3ObjectMetadata metadata = this.s3.getObjectMetadata(bucket, s3Key, null);
      String username = metadata.getMetadata().entrySet().stream()
          .filter(s -> s.getKey().equalsIgnoreCase("userid")).findFirst().map(s -> s.getValue())
          .orElse("System");

      doc.setUserId(username);
    }

    updateContentLength(siteId, doc);

    return doc;
  }

  /**
   * If document has a tagschema that needs to be processed.
   * 
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  private void postDocumentTags(final String siteId, final DynamicDocumentItem doc)
      throws IOException, InterruptedException {

    List<DynamicObject> tags = doc.getList("tags");

    if (!tags.isEmpty()) {

      List<AddDocumentTag> addTags = tags.stream().map(t -> {

        List<String> values = t.containsKey("values") ? values = t.getStringList("values") : null;

        AddDocumentTag tag =
            new AddDocumentTag().key(t.getString("key")).value(t.getString("value")).values(values);

        return tag;

      }).collect(Collectors.toList());

      String documentId = doc.getDocumentId();
      AddDocumentTagRequest req =
          new AddDocumentTagRequest().siteId(siteId).documentId(documentId).tags(addTags);

      this.formkiqClient.addDocumentTag(req);
    }
  }

  /**
   * Process S3 Event.
   * 
   * @param logger {@link LambdaLogger}
   * @param date {@link Date}
   * @param event {@link Map}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  private void processEvent(final LambdaLogger logger, final Date date,
      final Map<String, Object> event) throws IOException, InterruptedException {

    String eventName = event.get("eventName").toString();
    boolean objectCreated = eventName.contains("ObjectCreated");

    String bucket = getBucketName(event);

    String key = getObjectKey(event);
    String s3Key = urlDecode(key);
    String siteId = getSiteId(s3Key);

    if (objectCreated) {

      DynamicDocumentItem loadDocument = loadDocument(logger, bucket, siteId, s3Key);

      Map<String, String> contentMap = createContentMap(loadDocument);
      Map<String, String> contentTypeMap = createContentTypeMap(loadDocument);

      boolean hasContent = !contentMap.isEmpty();

      DocumentItem existingDocument =
          this.service.findDocument(siteId, loadDocument.getDocumentId());
      DynamicDocumentItem item =
          createDocument(logger, siteId, loadDocument, date, hasContent, existingDocument);

      if (item != null) {

        String tagSchemaId = item.getString("tagSchemaId");
        Boolean newCompositeTags = item.getBoolean("newCompositeTags");

        if (!StringUtils.isEmpty(tagSchemaId) && Boolean.FALSE.equals(newCompositeTags)) {
          createFormKiQConnectionIfNeeded();
          postDocumentTags(siteId, item);
        }

        writeS3Document(logger, bucket, s3Key, siteId, item, contentMap, contentTypeMap);

        if (contentMap.isEmpty()) {
          logger.log(String.format("Skipping %s no content", item.getPath()));
        }

        if (existingDocument != null && !hasContent) {

          List<Action> actions = this.actionsService.getActions(siteId, item.getDocumentId());
          List<Action> syncs = actions.stream().filter(a -> ActionType.FULLTEXT.equals(a.type()))
              .collect(Collectors.toList());
          syncs.forEach(a -> a.status(ActionStatus.PENDING));
          this.actionsService.saveActions(siteId, item.getDocumentId(), actions);

          logger.log("publishing actions message to " + this.snsDocumentEvent);
          this.notificationService.publishNextActionEvent(actions, siteId, item.getDocumentId());
        }
      }

      deleteObject(logger, bucket, s3Key);
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

        try {
          processEvent(logger, date, event);
        } catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void updateContentLength(final String siteId, final DynamicDocumentItem obj) {

    if (obj.containsKey("contentLength") && obj.get("contentLength") instanceof Double) {
      long contentLength = obj.getDouble("contentLength").longValue();
      obj.setContentLength(Long.valueOf(contentLength));
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
   * @param siteId {@link String}
   * @param obj {@link DynamicDocumentItem}
   * @param existingDocument {@link DocumentItem}
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem updateFromExistingDocument(final String siteId,
      final DynamicDocumentItem obj, final DocumentItem existingDocument) {

    DynamicDocumentItem response = null;

    if (existingDocument != null) {
      DynamicDocumentItem origMap = new DocumentItemToDynamicDocumentItem().apply(existingDocument);
      origMap.putAll(obj);
      response = origMap;
    } else {
      response = obj;
    }

    return response;
  }

  private void writeS3Document(final LambdaLogger logger, final String bucket, final String s3Key,
      final String siteId, final DynamicDocumentItem item, final Map<String, String> contentMap,
      final Map<String, String> contentTypeMap) {

    Map<String, String> map = Map.of("checksum", item.getChecksum());

    if (s3Key.endsWith(FORMKIQ_B64_EXT)) {

      for (Map.Entry<String, String> e : contentMap.entrySet()) {

        boolean isBase64 = item.getBoolean("isBase64").booleanValue();
        byte[] bytes =
            isBase64 ? Base64.getDecoder().decode(e.getValue().getBytes(StandardCharsets.UTF_8))
                : e.getValue().getBytes(StandardCharsets.UTF_8);

        String key = createDatabaseKey(siteId, e.getKey());
        String contentType = contentTypeMap.get(e.getKey());

        logger.log(String.format("Inserted %s into bucket %s as %s", item.getPath(),
            this.documentsBucket, createDatabaseKey(siteId, item.getDocumentId())));

        this.s3.putObject(this.documentsBucket, key, bytes, contentType, map);
      }

    } else {

      S3ObjectMetadata metadata = this.s3.getObjectMetadata(bucket, s3Key, null);

      String destKey = createDatabaseKey(siteId, item.getDocumentId());

      logger.log(String.format("Copying %s from bucket %s to %s in bucket %s.", s3Key, bucket,
          destKey, this.documentsBucket));

      this.s3.copyObject(bucket, s3Key, this.documentsBucket, destKey, metadata.getContentType(),
          map);
    }
  }
}
