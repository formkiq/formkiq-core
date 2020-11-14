/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.handler;

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getCallingCognitoUsername;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_CREATED;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createS3Key;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMapResponse;
import com.formkiq.stacks.api.BadException;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.common.objects.DynamicObject;
import software.amazon.awssdk.services.s3.S3Client;

/** {@link RequestHandler} for POST "/documents" or PATCH "/documents". */
public class DocumentIdPostRequestHandler implements RequestHandler {

  /** Default Duration Hours. */
  private static final int DEFAULT_DURATION_HOURS = 2;

  /** Extension for FormKiQ config file. */
  private static final String FORMKIQ_DOC_EXT = ".fkb64";

  /** {@link DocumentsRestrictionsMaxDocuments}. */
  private DocumentsRestrictionsMaxDocuments restrictionMaxDocuments;

  /**
   * constructor.
   * 
   * @param maxDocuments {@link DocumentsRestrictionsMaxDocuments}
   */
  public DocumentIdPostRequestHandler(final DocumentsRestrictionsMaxDocuments maxDocuments) {
    this.restrictionMaxDocuments = maxDocuments;
  }

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
   * Add field to object. s
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
      url = awsservice.s3Service()
          .presignPostUrl(awsservice.documents3bucket(), key, duration, Optional.empty())
          .toString();
    }

    return url;
  }

  @Override
  public boolean isReadonly(final String method) {
    return false;
  }

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    boolean isUpdate = event.getHttpMethod().equalsIgnoreCase("patch")
        && event.getPathParameters().containsKey("documentId");

    String siteId = getSiteId(event);
    String documentId = UUID.randomUUID().toString();

    if (isUpdate) {
      documentId = event.getPathParameters().get("documentId");
      if (awsservice.documentService().findDocument(siteId, documentId) == null) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }
    }

    return process(logger, awsservice, event, isUpdate, documentId, siteId);
  }

  /**
   * Process Document Create/Update Request.
   *
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param event {@link ApiGatewayRequestEvent}
   * @param isUpdate boolean
   * @param documentId {@link String}
   * @param siteId {@link String}
   * @return {@link ApiRequestHandlerResponse}
   * @throws BadException BadException
   * @throws IOException IOException
   */
  private ApiRequestHandlerResponse process(final LambdaLogger logger,
      final AwsServiceCache awsservice, final ApiGatewayRequestEvent event, final boolean isUpdate,
      final String documentId, final String siteId) throws BadException, IOException {

    String maxDocumentCount = null;

    DynamicObject item = fromBodyToDynamicObject(logger, event);

    List<DynamicObject> documents = item.getList("documents");

    if (!isUpdate) {

      if (!item.hasString("content") && item.getList("documents").isEmpty()) {
        throw new BadException("Invalid JSON body.");
      }

      maxDocumentCount = this.restrictionMaxDocuments.getSsmValue(awsservice, siteId);
      if (maxDocumentCount != null
          && this.restrictionMaxDocuments.enforced(awsservice, siteId, maxDocumentCount)) {
        throw new BadException("Max Number of Documents reached");
      }
    }

    addFieldsToObject(event, awsservice, siteId, documentId, item, documents);
    item.put("documents", documents);

    logger.log("setting userId: " + item.getString("userId") + " contentType: "
        + item.getString("contentType"));

    putObjectToStaging(logger, awsservice, maxDocumentCount, siteId, item);

    Map<String, String> uploadUrls =
        generateUploadUrls(awsservice, siteId, documentId, item, documents);
    Map<String, Object> map = buildResponse(siteId, documentId, documents, uploadUrls);

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
    logger.log("s3 putObject " + key + " into bucket " + awsservice.stages3bucket());

    S3Service s3 = awsservice.s3Service();
    try (S3Client client = s3.buildClient()) {
      s3.putObject(client, awsservice.stages3bucket(), key, bytes, null);

      if (maxDocumentCount != null) {
        awsservice.documentCountService().incrementDocumentCount(siteId);
      }
    }
  }
}
