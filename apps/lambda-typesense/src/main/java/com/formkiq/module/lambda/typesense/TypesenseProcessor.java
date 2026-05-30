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
package com.formkiq.module.lambda.typesense;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.module.http.HttpResponseStatus.is2XX;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.model.DocumentMapToDocument;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;

/** {@link RequestHandler} for handling DynamoDb to Typesense Processor. */
@Reflectable
public class TypesenseProcessor implements RequestHandler<Map<String, Object>, Void> {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;

  static {

    if (System.getenv().containsKey("AWS_REGION")) {
      serviceCache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry()).build();

      initialize(serviceCache);
    }
  }

  /**
   * Initialize.
   * 
   * @param awsServices {@link AwsServiceCache}
   */
  public static void initialize(final AwsServiceCache awsServices) {

    awsServices.register(TypeSenseService.class, new TypeSenseServiceExtension());
    awsServices.register(DynamoDbService.class, new DynamoDbServiceExtension());
    awsServices.register(DocumentSyncService.class, new DocumentSyncServiceExtension());
    serviceCache = awsServices;
  }

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   */
  public TypesenseProcessor() {}

  /**
   * constructor.
   * 
   * @param awsServices {@link AwsServiceCache}
   */
  public TypesenseProcessor(final AwsServiceCache awsServices) {
    initialize(awsServices);
  }

  private void addDocumentSync(final HttpResponse<String> response, final String siteId,
      final String documentId, final boolean s3VersionChanged, final boolean added) {

    DocumentSyncStatus status =
        is2XX(response) ? DocumentSyncStatus.COMPLETE : DocumentSyncStatus.FAILED;

    DocumentSyncType syncType =
        s3VersionChanged ? DocumentSyncType.CONTENT : DocumentSyncType.METADATA;

    DocumentSyncService syncService = serviceCache.getExtension(DocumentSyncService.class);
    syncService.saveSync(siteId, documentId, DocumentSyncServiceType.TYPESENSE, status, syncType,
        !added);
  }

  /**
   * Add or Update Document.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param data {@link Map}
   * @param s3VersionChanged boolean
   * @return HttpResponse
   * @throws IOException IOException
   */
  public HttpResponse<String> addOrUpdate(final String siteId, final String documentId,
      final Map<String, Object> data, final boolean s3VersionChanged) throws IOException {

    TypeSenseService typeSenseService = serviceCache.getExtension(TypeSenseService.class);

    HttpResponse<String> response = typeSenseService.addOrUpdateDocument(siteId, documentId, data);

    if (is2XX(response)) {

      boolean added = "POST".equals(response.request().method());
      addDocumentSync(response, siteId, documentId, s3VersionChanged, added);

    } else {
      addDocumentSync(response, siteId, documentId, s3VersionChanged, true);
    }

    return response;
  }

  /**
   * Delete Syncs.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  private void deleteSyncs(final String siteId, final String documentId) {
    DocumentSyncService syncService = serviceCache.getExtension(DocumentSyncService.class);
    syncService.deleteAll(siteId, documentId);
  }

  private String getAttributeStringValue(final Map<String, String> field) {
    return field.containsKey("S") ? field.get("S") : field.get("s");
  }

  /**
   * Get DocumentId from NewImage / OldImage.
   * 
   * @param newImage {@link Map}
   * @param oldImage {@link Map}
   * @param fieldName {@link String}
   * @return {@link String}
   */
  private String getField(final Map<String, Object> newImage, final Map<String, Object> oldImage,
      final String fieldName) {

    Map<String, String> field =
        newImage.containsKey(fieldName) ? (Map<String, String>) newImage.get(fieldName)
            : Collections.emptyMap();

    if (field.isEmpty()) {
      field = oldImage.containsKey(fieldName) ? (Map<String, String>) oldImage.get(fieldName)
          : Collections.emptyMap();
    }

    return getAttributeStringValue(field);
  }

  /**
   * Get User Id.
   * 
   * @param newImage {@link Map}
   * @param oldImage {@link Map}
   * @return {@link String}
   */
  private String getUserId(final Map<String, Object> newImage, final Map<String, Object> oldImage) {
    String userId = getField(newImage, oldImage, "userId");
    userId = userId != null ? userId : "System";
    return userId;
  }

  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    Logger logger = serviceCache.getLogger();
    if (logger.isLogged(LogLevel.DEBUG)) {
      String json = this.gson.toJson(map);
      logger.debug(json);
    }

    List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
    processRecords(logger, records);

    return null;
  }

  private boolean isDocumentSk(final Map<String, Object> data) {
    Map<String, String> map = (Map<String, String>) data.get(SK);
    String sk = getAttributeStringValue(map);
    return "document".equals(sk);
  }

  private boolean isS3VersionChanged(final String eventName, final Map<String, Object> oldImage,
      final Map<String, Object> newImage) {

    boolean changed = false;

    if ("MODIFY".equalsIgnoreCase(eventName)) {

      String oldS3 = getField(oldImage, oldImage, DocumentVersionService.S3VERSION_ATTRIBUTE);
      String newS3 = getField(newImage, newImage, DocumentVersionService.S3VERSION_ATTRIBUTE);

      changed = (oldS3 == null && newS3 != null) || (oldS3 != null && !oldS3.equals(newS3));
    }

    return changed;
  }

  /**
   * Process Record.
   * 
   * @param logger {@link Logger}
   * 
   * @param record {@link Map}
   */
  private void processRecord(final Logger logger, final Map<String, Object> record) {

    String eventName = record.get("eventName").toString();
    Map<String, Object> dynamodb = toMap(record.get("dynamodb"));

    Map<String, Object> newImage =
        dynamodb.containsKey("NewImage") ? toMap(dynamodb.get("NewImage")) : Collections.emptyMap();

    Map<String, Object> oldImage =
        dynamodb.containsKey("OldImage") ? toMap(dynamodb.get("OldImage")) : Collections.emptyMap();

    String siteId = newImage.containsKey(PK) || oldImage.containsKey(PK)
        ? getSiteId(getField(newImage, oldImage, PK))
        : null;
    String documentId = getField(newImage, oldImage, "documentId");

    if (documentId != null) {

      try {

        if ("INSERT".equalsIgnoreCase(eventName) || "MODIFY".equalsIgnoreCase(eventName)) {

          logger.debug(
              "processing event " + eventName + " for document " + siteId + " " + documentId);

          boolean s3VersionChanged = isS3VersionChanged(eventName, oldImage, newImage);

          String userId = getUserId(newImage, oldImage);
          ApiAuthorization.login(new ApiAuthorization().username(userId));

          try {
            writeToIndex(siteId, documentId, newImage, s3VersionChanged);
          } finally {
            ApiAuthorization.logout();
          }

        } else if ("REMOVE".equalsIgnoreCase(eventName)) {

          removeDocument(siteId, documentId, oldImage);

        } else {
          logger
              .debug("skipping event " + eventName + " for document " + siteId + " " + documentId);
        }

      } catch (IOException e) {
        logger.error(e);
      }

    } else {
      logger.trace("skipping event " + eventName);
    }
  }

  /**
   * Process Records.
   * 
   * @param logger {@link Logger}
   * @param records {@link List} {@link Map}
   */
  private void processRecords(final Logger logger, final List<Map<String, Object>> records) {
    for (Map<String, Object> record : records) {

      if (record.containsKey("eventName")) {
        processRecord(logger, record);
      }
    }
  }

  /**
   * Remove Document from TypeSense.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param oldImage {@link Map}
   * @throws IOException IOException
   */
  private void removeDocument(final String siteId, final String documentId,
      final Map<String, Object> oldImage) throws IOException {
    boolean isDocument = isDocumentSk(oldImage);
    if (isDocument) {
      TypeSenseService typeSenseService = serviceCache.getExtension(TypeSenseService.class);
      typeSenseService.deleteDocument(siteId, documentId);
      deleteSyncs(siteId, documentId);
    }
  }

  /**
   * Remove DynamoDb Keys.
   * 
   * @param map {@link Map}
   */
  private void removeDynamodbKeys(final Map<String, Object> map) {
    map.remove(PK);
    map.remove(SK);
    map.remove(GSI1_PK);
    map.remove(GSI1_SK);
    map.remove(GSI2_PK);
    map.remove(GSI2_SK);
  }

  private Map<String, Object> toMap(final Object object) {
    return (Map<String, Object>) object;
  }

  /**
   * Write Data to Typesense Index.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param data {@link Map}
   * @param s3VersionChanged boolean
   * @throws IOException IOException
   */
  private void writeToIndex(final String siteId, final String documentId,
      final Map<String, Object> data, final boolean s3VersionChanged) throws IOException {

    boolean isDocument = isDocumentSk(data);

    removeDynamodbKeys(data);

    Logger logger = serviceCache.getLogger();

    if (isDocument) {

      logger.trace("writing to index: " + data);

      Map<String, Object> document = new DocumentMapToDocument().apply(data);
      addOrUpdate(siteId, documentId, document, s3VersionChanged);
    } else {
      logger.trace("skipping dynamodb record");
    }
  }
}
