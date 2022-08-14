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
import java.io.IOException;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
import com.formkiq.graalvm.annotations.ReflectableClasses;
import com.formkiq.graalvm.annotations.ReflectableField;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.actions.services.DynamicObjectToAction;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddDocumentTag;
import com.formkiq.stacks.client.requests.AddDocumentTagRequest;
import com.formkiq.stacks.dynamodb.DateUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/** {@link RequestHandler} for handling Document Staging Create Events. */
@Reflectable
@ReflectableImport(classes = {DocumentItemDynamoDb.class, DocumentTag.class, DocumentTagType.class,
    AddDocumentTag.class})
@ReflectableClasses({
    @ReflectableClass(className = AddDocumentTagRequest.class, allPublicConstructors = true,
        fields = {@ReflectableField(name = "tag"), @ReflectableField(name = "tags")}),
    @ReflectableClass(className = AddDocumentTag.class, allPublicConstructors = true,
        fields = {@ReflectableField(name = "key"), @ReflectableField(name = "value"),
            @ReflectableField(name = "values")})})
public class StagingS3Create implements RequestHandler<Map<String, Object>, Void> {

  /** Extension for FormKiQ config file. */
  public static final String FORMKIQ_B64_EXT = ".fkb64";
  /** Virtual Folder Deliminator. */
  public static final String VIRTUAL_FOLDER_DELIM = "::/";

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

  /** App Environment. */
  private String appEnvironment;
  /** {@link String}. */
  private String documentsBucket;
  /** IAM Documents Url. */
  private String documentsIamUrl = null;
  /** {@link FormKiqClient}. */
  private FormKiqClientV1 formkiqClient = null;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** {@link S3Service}. */
  private S3Service s3;

  /** {@link DocumentSearchService}. */
  private DocumentSearchService searchService;

  /** {@link DocumentService}. */
  private DocumentService service;
  /** {@link ActionsService}. */
  private ActionsService actionsService;
  /** SQS Error Queue. */
  private String sqsErrorQueue;
  /** {@link SqsService}. */
  private SqsService sqsService;
  /** {@link SsmConnectionBuilder}. */
  private SsmConnectionBuilder ssmConnection;
  /** {@link Region}. */
  private Region region;
  /** {@link AwsCredentials}. */
  private AwsCredentials credentials;

  /** constructor. */
  public StagingS3Create() {
    this(System.getenv(), Region.of(System.getenv("AWS_REGION")),
        EnvironmentVariableCredentialsProvider.create().resolveCredentials(),
        new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new S3ConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SqsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SsmConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param awsRegion {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param dynamoDb {@link DynamoDbConnectionBuilder}
   * @param s3Builder {@link S3ConnectionBuilder}
   * @param sqsBuilder {@link SqsConnectionBuilder}
   * @param ssmConnectionBuilder {@link SsmConnectionBuilder}
   */
  protected StagingS3Create(final Map<String, String> map, final Region awsRegion,
      final AwsCredentials awsCredentials, final DynamoDbConnectionBuilder dynamoDb,
      final S3ConnectionBuilder s3Builder, final SqsConnectionBuilder sqsBuilder,
      final SsmConnectionBuilder ssmConnectionBuilder) {

    this.region = awsRegion;
    this.credentials = awsCredentials;
    String documentsTable = map.get("DOCUMENTS_TABLE");
    this.service = new DocumentServiceImpl(dynamoDb, documentsTable);
    this.searchService =
        new DocumentSearchServiceImpl(this.service, dynamoDb, documentsTable, null);
    this.actionsService = new ActionsServiceDynamoDb(dynamoDb, documentsTable);
    this.s3 = new S3Service(s3Builder);
    this.sqsService = new SqsService(sqsBuilder);
    this.ssmConnection = ssmConnectionBuilder;

    this.documentsBucket = map.get("DOCUMENTS_S3_BUCKET");
    this.sqsErrorQueue = map.get("SQS_ERROR_URL");
    this.appEnvironment = map.get("APP_ENVIRONMENT");
  }

  /**
   * Determines whether {@link String} is a JSON config file.
   *
   * @param s3Client {@link S3Client}
   * @param logger {@link LambdaLogger}
   * @param bucket {@link String}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link DynamicDocumentItem}
   */
  @SuppressWarnings("unchecked")
  private DynamicDocumentItem configfile(final S3Client s3Client, final LambdaLogger logger,
      final String bucket, final String siteId, final String documentId) {

    DynamicDocumentItem obj = null;

    if (documentId.endsWith(FORMKIQ_B64_EXT)) {
      String s = this.s3.getContentAsString(s3Client, bucket, documentId, null);

      if ("true".equals(System.getenv("DEBUG"))) {
        logger.log(s);
      }

      Map<String, Object> map = this.gson.fromJson(s, Map.class);
      obj = new DynamicDocumentItem(map);

      if (obj.containsKey("insertedDate")) {

        SimpleDateFormat f = DateUtil.getIsoDateFormatter();

        try {
          obj.setInsertedDate(f.parse(obj.getString("insertedDate")));
        } catch (ParseException e) {
          obj.setInsertedDate(new Date());
        }

      } else {
        obj.setInsertedDate(new Date());
      }

      if (obj.getPath() != null && obj.getPath().contains(VIRTUAL_FOLDER_DELIM)) {
        String realDocumentId = getDocumentIdForPath(siteId, obj.getPath());
        obj.setDocumentId(realDocumentId);
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
    String documentIdForPath = !uuid ? getDocumentIdForPath(siteId, key) : null;
    String documentId =
        documentIdForPath != null ? documentIdForPath : uuid ? key : UUID.randomUUID().toString();

    String destKey = createDatabaseKey(siteId, documentId);

    S3ObjectMetadata metadata = this.s3.getObjectMetadata(s3Client, bucket, originalkey);

    // if file path isn't in the database it's already saved
    if (documentIdForPath == null) {
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

      saveDocument(siteId, doc);
    }

    logger.log(String.format("Copying %s from bucket %s to %s in bucket %s.", originalkey, bucket,
        destKey, this.documentsBucket));

    this.s3.copyObject(s3Client, bucket, originalkey, this.documentsBucket, destKey,
        metadata.getContentType());
  }

  /**
   * Save {@link DynamicDocumentItem}.
   * 
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   */
  private void saveDocument(final String siteId, final DynamicDocumentItem doc) {
    this.service.saveDocumentItemWithTag(siteId, doc);

    if (doc.containsKey("actions")) {

      DynamicObjectToAction transform = new DynamicObjectToAction();
      List<DynamicObject> list = doc.getList("actions");
      List<Action> actions =
          list.stream().map(s -> transform.apply(s)).collect(Collectors.toList());

      this.actionsService.saveActions(siteId, doc.getDocumentId(), actions);
    }
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

  /**
   * Build connection to the IAM API url.
   */
  private void createFormKiQConnectionIfNeeded() {

    if (this.documentsIamUrl == null) {
      final int cacheTime = 5;
      SsmService ssmService = new SsmServiceCache(this.ssmConnection, cacheTime, TimeUnit.MINUTES);
      this.documentsIamUrl =
          ssmService.getParameterValue("/formkiq/" + this.appEnvironment + "/api/DocumentsIamUrl");
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

  /**
   * Find DocumentId for File Path.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @return {@link String}
   */
  private String getDocumentIdForPath(final String siteId, final String path) {
    SearchQuery q = new SearchQuery().tag(new SearchTagCriteria().key("path").eq(path));
    PaginationResults<DynamicDocumentItem> result = this.searchService.search(siteId, q, null, 1);
    return !result.getResults().isEmpty() ? result.getResults().get(0).getDocumentId() : null;
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
    String documentId = urlDecode(key);
    String siteId = getSiteId(documentId);

    if (objectCreated) {

      try (S3Client s = this.s3.buildClient()) {

        DynamicDocumentItem doc = configfile(s, logger, bucket, siteId, documentId);

        if (doc != null) {
          write(s, logger, doc, date, siteId);

          String tagSchemaId = doc.getString("tagSchemaId");
          Boolean newCompositeTags = doc.getBoolean("newCompositeTags");

          if (!StringUtils.isEmpty(tagSchemaId) && Boolean.FALSE.equals(newCompositeTags)) {
            createFormKiQConnectionIfNeeded();
            postDocumentTags(siteId, doc);
          }

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

        try {
          processEvent(logger, date, event);
        } catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
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

    } else {
      logger.log(String.format("Skipping %s no content", doc.getPath()));
    }

    saveDocument(siteId, doc);
  }

  /**
   * Writes File to S3.
   * 
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
}
