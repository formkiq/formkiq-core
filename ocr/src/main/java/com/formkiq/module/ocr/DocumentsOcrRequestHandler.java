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
package com.formkiq.module.ocr;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.validation.ValidationBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.module.ocr.DocumentOcrService.PREFIX_TEMP_FILES;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/ocr". */
public class DocumentsOcrRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link DocumentsOcrRequestHandler} URL. */
  public static final String URL = "/documents/{documentId}/ocr";

  /**
   * constructor.
   *
   */
  public DocumentsOcrRequestHandler() {}

  /**
   * Build Get Response.
   *
   * @param obj {@link DynamicObject}
   * @param documentId {@link String}
   * @return {@link Map}
   */
  private Map<String, Object> buildGetResponse(final Ocr obj, final String documentId) {
    Map<String, Object> map = new HashMap<>();
    map.put("documentId", documentId);

    if (obj != null) {

      map.put("insertedDate", obj.insertedDate());
      map.put("contentType", obj.contentType());
      map.put("userId", obj.userId());
      map.put("ocrEngine", obj.engine().name());
      map.put("ocrStatus", obj.status().name());
    }

    return map;
  }

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    verifyDocument(awsservice, siteId, documentId);

    DocumentOcrService ocrService = awsservice.getExtension(DocumentOcrService.class);
    ocrService.delete(siteId, documentId);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Deleted OCR for DocumentId '" + documentId + "'").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    ApiResponseStatus status = SC_OK;
    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");

    verifyDocument(awsservice, siteId, documentId);

    final boolean contentUrl = isContentUrl(event);
    final boolean textOnly = isTextOnly(event);
    final boolean keyValue = !textOnly && isKeyValue(event);
    final boolean tables = isTables(event);

    DocumentOcrService ocrService = awsservice.getExtension(DocumentOcrService.class);

    Ocr obj = ocrService.get(siteId, documentId);

    Map<String, Object> map = buildGetResponse(obj, documentId);

    if (map.containsKey("ocrStatus")) {

      if (isOcrStatus(map, OcrScanStatus.SUCCESSFUL) || isOcrStatus(map, OcrScanStatus.SKIPPED)) {

        S3Service s3 = awsservice.getExtension(S3Service.class);

        String jobId = obj.jobId();

        List<String> s3Keys = ocrService.getOcrS3Keys(siteId, documentId, jobId);
        if (s3Keys.isEmpty()) {
          throw new NotFoundException("OCR results not found");
        }

        if (contentUrl) {

          List<String> contentUrls = getContentUrls(awsservice, ocrService, s3, s3Keys, textOnly);
          map.put("contentUrls", contentUrls);

        } else {

          List<String> contents = getS3Content(awsservice, s3, s3Keys);
          updateObject(ocrService, map, contents, textOnly, keyValue, tables);
        }
      }

    } else {
      status = SC_NOT_FOUND;
    }

    return ApiRequestHandlerResponse.builder().status(status).body(map).build();
  }

  /**
   * Get S3 Content Urls.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param ocrService {@link DocumentOcrService}
   * @param s3 {@link S3Service}
   * @param s3Keys {@link List} {@link String}
   * @param textOnly boolean
   * @return {@link List} {@link String}
   */
  private List<String> getContentUrls(final AwsServiceCache awsservice,
      final DocumentOcrService ocrService, final S3Service s3, final List<String> s3Keys,
      final boolean textOnly) {


    String ocrBucket = awsservice.environment("OCR_S3_BUCKET");
    List<String> newS3Keys = new ArrayList<>();

    if (textOnly) {

      s3Keys.forEach(s3Key -> {
        String content = s3.getContentAsString(ocrBucket, s3Key, null);
        content = ocrService.toText(List.of(content));

        String newKey = PREFIX_TEMP_FILES + s3Key;
        s3.putObject(ocrBucket, newKey, content.getBytes(StandardCharsets.UTF_8), "text/plain");

        newS3Keys.add(newKey);
      });

    } else {
      newS3Keys.addAll(s3Keys);
    }

    S3PresignerService s3Presigner = awsservice.getExtension(S3PresignerService.class);
    PresignGetUrlConfig config = new PresignGetUrlConfig();
    return newS3Keys
        .stream().map(s3key -> s3Presigner
            .presignGetUrl(ocrBucket, s3key, Duration.ofHours(1), null, config).toString())
        .collect(Collectors.toList());
  }

  private String getOutputType(final ApiGatewayRequestEvent event) {
    return event.getQueryStringParameter("outputType");
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }

  /**
   * Get S3 Content.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param s3 {@link S3Service}
   * @param s3Keys {@link List} {@link String}
   * @return {@link String}
   */
  private List<String> getS3Content(final AwsServiceCache awsservice, final S3Service s3,
      final List<String> s3Keys) {

    String ocrBucket = awsservice.environment("OCR_S3_BUCKET");
    return s3Keys.stream().map(s3Key -> s3.getContentAsString(ocrBucket, s3Key, null)).toList();
  }

  private boolean isContentUrl(final ApiGatewayRequestEvent event) {
    boolean contentUrl = event.getQueryStringParameters() != null
        && event.getQueryStringParameters().containsKey("contentUrl");

    String outputType = getOutputType(event);
    if ("CONTENT_URL".equalsIgnoreCase(outputType)) {
      contentUrl = true;
    }

    return contentUrl;
  }

  private boolean isKeyValue(final ApiGatewayRequestEvent event) {
    boolean keyOnly = false;

    String outputType = getOutputType(event);
    if ("KEY_VALUE".equalsIgnoreCase(outputType)) {
      keyOnly = true;
    }

    return keyOnly;
  }

  private boolean isOcrStatus(final Map<String, Object> map, final OcrScanStatus status) {
    return status.name().equalsIgnoreCase(map.get("ocrStatus").toString());
  }

  private boolean isTables(final ApiGatewayRequestEvent event) {
    boolean tables = false;

    String outputType = getOutputType(event);
    if ("TABLES".equalsIgnoreCase(outputType)) {
      tables = true;
    }

    return tables;
  }

  private boolean isTextOnly(final ApiGatewayRequestEvent event) {
    boolean textOnly = event.getQueryStringParameters() != null
        && event.getQueryStringParameters().containsKey("text");

    String outputType = getOutputType(event);
    if ("text".equalsIgnoreCase(outputType)) {
      textOnly = true;
    }

    return textOnly;
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");

    verifyDocument(awsservice, siteId, documentId);

    OcrRequest request = JsonToObject.fromJson(awsservice, event, OcrRequest.class);
    validate(request);

    String userId = authorization.getUsername();

    DocumentOcrService ocrService = awsservice.getExtension(DocumentOcrService.class);
    if (!ocrService.convert(awsservice, request, siteId, documentId, userId)) {
      throw new BadException("Maximum number of OCRs reached");
    }

    return ApiRequestHandlerResponse.builder().ok().body("message", "OCR request submitted")
        .build();
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");

    verifyDocument(awsservice, siteId, documentId);

    String userId = authorization.getUsername();

    Map<String, Object> map = JsonToObject.fromJson(awsservice, event, Map.class);
    String contentType = (String) map.get("contentType");
    String content = (String) map.get("content");

    if (contentType == null || content == null) {
      throw new BadException("'content' and 'contentType' are required");
    }

    DocumentOcrService ocrService = awsservice.getExtension(DocumentOcrService.class);
    ocrService.set(awsservice, siteId, documentId, userId, content, contentType);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Set OCR for documentId '" + documentId + "'").build();
  }

  private void updateObject(final DocumentOcrService ocrService, final Map<String, Object> map,
      final List<String> contents, final boolean textOnly, final boolean keyValue,
      final boolean tables) {

    if (textOnly) {
      map.put("data", ocrService.toText(contents));
    } else if (keyValue) {
      map.put("keyValues", ocrService.toKeyValue(contents));
    } else {
      map.put("data", String.join("", contents));
    }

    if (tables) {
      map.remove("data");
      map.put("tables", ocrService.toTables(contents));
    }
  }

  private void validate(final OcrRequest request) {
    Optional<String> o =
        notNull(request.getParseTypes()).stream().filter("queries"::equalsIgnoreCase).findFirst();
    if (o.isPresent()) {

      List<AwsTextractQuery> queries = notNull(request.getTextractQueries());
      ValidationBuilder vb = new ValidationBuilder();
      vb.isRequired(null, queries, "'TextractQueries' is required");

      queries.forEach(q -> vb.isRequired("TextractQuery.text", q.text()));
      vb.check();
    }
  }

  private void verifyDocument(final AwsServiceCache awsservice, final String siteId,
      final String documentId) throws Exception {
    DocumentService ds = awsservice.getExtension(DocumentService.class);
    DocumentItem item = ds.findDocument(siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));
  }
}
