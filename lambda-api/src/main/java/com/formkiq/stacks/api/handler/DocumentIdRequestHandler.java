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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil.getCallingCognitoUsername;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationResult;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.stacks.dynamodb.DateUtil;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTagToDynamicDocumentTag;
import com.formkiq.stacks.dynamodb.DocumentValidator;
import com.formkiq.stacks.dynamodb.DocumentValidatorImpl;
import com.formkiq.stacks.dynamodb.DynamicDocumentTag;
import com.formkiq.stacks.dynamodb.DynamicObjectToDocumentTag;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}". */
public class DocumentIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Default Duration Hours. */
  private static final int DEFAULT_DURATION_HOURS = 2;

  /** Extension for FormKiQ config file. */
  public static final String FORMKIQ_DOC_EXT = ".fkb64";

  /** {@link DocumentValidator}. */
  private DocumentValidator documentValidator = new DocumentValidatorImpl();
  /** {@link DocumentsRestrictionsMaxDocuments}. */
  private DocumentsRestrictionsMaxDocuments restrictionMaxDocuments =
      new DocumentsRestrictionsMaxDocuments();

  /**
   * constructor.
   *
   */
  public DocumentIdRequestHandler() {}

  /**
   * Add field to object.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param item {@link DynamicObject}
   * @param documents {@link List} {@link DynamicObject}
   */
  private void addFieldsToObject(final ApiGatewayRequestEvent event,
      final AwsServiceCache awsservice, final String siteId, final String documentId,
      final DynamicObject item, final List<DynamicObject> documents) {

    String userId = getCallingCognitoUsername(event);

    item.put("documentId", documentId);
    item.put("userId", userId);

    for (DynamicObject map : documents) {
      map.put("documentId", UUID.randomUUID().toString());
      map.put("userId", userId);
    }

  }

  /**
   * Build Response {@link Map}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param documents {@link List} {@link Map}
   * @param uploadUrls {@link Map}
   * @return {@link Map}
   */
  private Map<String, Object> buildResponse(final String siteId, final String documentId,
      final List<DynamicObject> documents, final Map<String, String> uploadUrls) {

    Map<String, Object> map = new HashMap<>();
    map.put("documentId", documentId);
    map.put("uploadUrl", uploadUrls.get(documentId));

    if (siteId != null) {
      map.put("siteId", siteId);
    }

    List<Map<String, String>> documentsMap = documents.stream().map(d -> {
      Map<String, String> m = new HashMap<>();

      String id = d.getString("documentId");
      m.put("documentId", id);

      if (uploadUrls.containsKey(id)) {
        m.put("uploadUrl", uploadUrls.get(id));
      }

      return m;
    }).collect(Collectors.toList());

    if (!documentsMap.isEmpty()) {
      map.put("documents", documentsMap);
    }

    return map;
  }

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String documentBucket = awsservice.environment("DOCUMENTS_S3_BUCKET");

    String siteId = authorizer.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    logger.log("deleting object " + documentId + " from bucket '" + documentBucket + "'");

    try {

      S3Service s3Service = awsservice.getExtension(S3Service.class);

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      S3ObjectMetadata md = s3Service.getObjectMetadata(documentBucket, s3Key, null);

      if (md.isObjectExists()) {
        s3Service.deleteObject(documentBucket, s3Key, null);

      } else {

        DocumentService service = awsservice.getExtension(DocumentService.class);
        if (!service.deleteDocument(siteId, documentId)) {
          throw new NotFoundException("Document " + documentId + " not found.");
        }
      }

      ApiResponse resp = new ApiMessageResponse("'" + documentId + "' object deleted");
      return new ApiRequestHandlerResponse(SC_OK, resp);

    } catch (S3Exception e) {

      if (e.statusCode() == SC_NOT_FOUND.getStatusCode()) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }

      throw e;
    }
  }

  /**
   * Generate Presigned Upload Url.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link String}
   */
  private String generateUploadUrl(final AwsServiceCache awsservice, final String siteId,
      final String documentId) {

    String url = null;
    if (documentId != null) {
      Duration duration = Duration.ofHours(DEFAULT_DURATION_HOURS);
      String key = createS3Key(siteId, documentId);
      S3Service s3Service = awsservice.getExtension(S3Service.class);

      Map<String, String> map = Map.of("checksum", UUID.randomUUID().toString());
      url = s3Service.presignPutUrl(awsservice.environment("DOCUMENTS_S3_BUCKET"), key, duration,
          Optional.empty(), map).toString();
    }

    return url;
  }

  /**
   * Add field to object.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param item {@link DynamicObject}
   * @param documents {@link List} {@link DynamicObject}
   * @return {@link Map}
   */
  private Map<String, String> generateUploadUrls(final AwsServiceCache awsservice,
      final String siteId, final String documentId, final DynamicObject item,
      final List<DynamicObject> documents) {

    Map<String, String> map = new HashMap<>();

    if (!item.hasString("content")) {
      map.put(documentId, generateUploadUrl(awsservice, siteId, documentId));
    }

    for (DynamicObject o : documents) {
      if (!o.hasString("content")) {
        String docid = o.getString("documentId");
        map.put(docid, generateUploadUrl(awsservice, siteId, docid));
      }
    }

    return map;
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();
    int limit = getLimit(logger, event);

    CacheService cacheService = awsservice.getExtension(CacheService.class);
    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    ApiPagination token = getPagination(cacheService, event);
    String documentId = event.getPathParameters().get("documentId");
    ApiPagination pagination = getPagination(cacheService, event);

    PaginationResult<DocumentItem> presult = documentService.findDocument(siteId, documentId, true,
        token != null ? token.getStartkey() : null, limit);
    DocumentItem result = presult.getResult();

    if (result == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    ApiPagination current =
        createPagination(cacheService, event, pagination, presult.getToken(), limit);

    DynamicDocumentItem item = new DocumentItemToDynamicDocumentItem().apply(result);
    item.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
    item.put("previous", current.getPrevious());
    item.put("next", current.hasNext() ? current.getNext() : null);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(item));
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}";
  }

  private boolean isFolder(final DynamicDocumentItem item) {
    boolean isFolder = item.hasString("path") && item.getString("path").endsWith("/");
    return isFolder;
  }

  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    boolean isUpdate = event.getHttpMethod().equalsIgnoreCase("patch")
        && event.getPathParameters().containsKey("documentId");

    String siteId = authorizer.getSiteId();
    String documentId = UUID.randomUUID().toString();

    DynamicDocumentItem item = new DynamicDocumentItem(fromBodyToMap(logger, event));

    if (isUpdate) {
      documentId = event.getPathParameters().get("documentId");
      validatePatch(awsservice, siteId, documentId, item);
    } else {
      updateContentType(event, item);
    }

    List<DynamicObject> documents = item.getList("documents");

    String maxDocumentCount = null;

    if (!isUpdate) {
      maxDocumentCount = validatePost(awsservice, siteId, item);
    }

    Map<String, Object> map = null;

    if (!isUpdate && isFolder(item)) {

      DocumentService docService = awsservice.getExtension(DocumentService.class);

      if (!docService.isFolderExists(siteId, item)) {
        docService.addFolderIndex(siteId, item);
        map = Map.of("message", "folder created");
      } else {
        throw new ValidationException(
            Arrays.asList(new ValidationErrorImpl().key("folder").error("already exists")));
      }

    } else {
      addFieldsToObject(event, awsservice, siteId, documentId, item, documents);
      item.put("documents", documents);

      logger.log("setting userId: " + item.getString("userId") + " contentType: "
          + item.getString("contentType"));

      validateTagSchema(awsservice, siteId, item, item.getUserId(), isUpdate);
      putObjectToStaging(logger, awsservice, maxDocumentCount, siteId, item);

      Map<String, String> uploadUrls =
          generateUploadUrls(awsservice, siteId, documentId, item, documents);
      map = buildResponse(siteId, documentId, documents, uploadUrls);
    }

    ApiResponseStatus status = isUpdate ? SC_OK : SC_CREATED;
    return new ApiRequestHandlerResponse(status, new ApiMapResponse(map));
  }

  /**
   * Put Object to Staging Bucket.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param maxDocumentCount {@link String}
   * @param siteId {@link String}
   * @param item {@link DynamicObject}
   */
  private void putObjectToStaging(final LambdaLogger logger, final AwsServiceCache awsservice,
      final String maxDocumentCount, final String siteId, final DynamicObject item) {

    List<DynamicObject> documents = item.getList("documents");
    item.put("documents", documents);

    String s = GSON.toJson(item);

    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

    String key = createDatabaseKey(siteId, item.getString("documentId") + FORMKIQ_DOC_EXT);
    String stageS3Bucket = awsservice.environment("STAGE_DOCUMENTS_S3_BUCKET");
    logger.log("s3 putObject " + key + " into bucket " + stageS3Bucket);

    S3Service s3 = awsservice.getExtension(S3Service.class);

    s3.putObject(stageS3Bucket, key, bytes, item.getString("contentType"));

    if (maxDocumentCount != null) {
      DocumentCountService countService = awsservice.getExtension(DocumentCountService.class);
      countService.incrementDocumentCount(siteId);
    }
  }

  /**
   * Update Content-Type on {@link DynamicObject} based on {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param item {@link DynamicObject}
   */
  private void updateContentType(final ApiGatewayRequestEvent event, final DynamicObject item) {

    if (!item.containsKey("contentType")) {
      item.put("contentType", "application/octet-stream");
    }
  }

  /**
   * Validate Patch Request.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param doc {@link DocumentItem}
   * @throws NotFoundException NotFoundException
   * @throws ValidationException ValidationException
   */
  private void validatePatch(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final DocumentItem doc)
      throws NotFoundException, ValidationException {
    DocumentService docService = awsservice.getExtension(DocumentService.class);
    DocumentItem item = docService.findDocument(siteId, documentId);
    if (item == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    Collection<DocumentMetadata> metadata =
        item.getMetadata() != null ? new ArrayList<>(item.getMetadata()) : new ArrayList<>();
    if (doc.getMetadata() != null) {
      metadata.addAll(doc.getMetadata());
    }

    Collection<ValidationError> errors = this.documentValidator.validate(metadata);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private String validatePost(final AwsServiceCache awsservice, final String siteId,
      final DynamicDocumentItem item) throws BadException, ValidationException {

    boolean isFolder = isFolder(item);

    if (!isFolder && !item.hasString("content") && item.getList("documents").isEmpty()) {
      throw new BadException("Invalid JSON body.");
    }

    Collection<ValidationError> errors = this.documentValidator.validate(item.getMetadata());
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    String maxDocumentCount = this.restrictionMaxDocuments.getValue(awsservice, siteId);
    if (maxDocumentCount != null
        && this.restrictionMaxDocuments.enforced(awsservice, siteId, maxDocumentCount)) {
      throw new BadException("Max Number of Documents reached");
    }

    return maxDocumentCount;
  }

  /**
   * Validate {@link DynamicDocumentItem} against a TagSchema.
   * 
   * @param cacheService {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param item {@link DynamicDocumentItem}
   * @param userId {@link String}
   * @param isUpdate boolean
   * @throws ValidationException ValidationException
   * @throws BadException BadException
   */
  private void validateTagSchema(final AwsServiceCache cacheService, final String siteId,
      final DynamicDocumentItem item, final String userId, final boolean isUpdate)
      throws ValidationException, BadException {

    List<DynamicObject> doctags = item.getList("tags");
    DynamicObjectToDocumentTag transform =
        new DynamicObjectToDocumentTag(DateUtil.getIsoDateFormatter());
    List<DocumentTag> tags = doctags.stream().map(t -> {
      return transform.apply(t);
    }).collect(Collectors.toList());

    DocumentTagSchemaPlugin plugin = cacheService.getExtension(DocumentTagSchemaPlugin.class);

    Collection<ValidationError> errors = new ArrayList<>();

    List<DocumentTag> compositeTags =
        plugin.addCompositeKeys(siteId, item, tags, userId, !isUpdate, errors).stream().map(t -> t)
            .collect(Collectors.toList());

    final boolean newCompositeTags = !compositeTags.isEmpty();

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    tags.addAll(compositeTags);

    DocumentTagToDynamicDocumentTag tf = new DocumentTagToDynamicDocumentTag();
    List<DynamicDocumentTag> objs = tags.stream().map(tf).collect(Collectors.toList());
    item.put("tags", objs);
    item.put("newCompositeTags", Boolean.valueOf(newCompositeTags));
  }
}
