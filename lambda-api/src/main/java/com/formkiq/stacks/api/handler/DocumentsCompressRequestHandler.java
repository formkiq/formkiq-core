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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static java.util.Map.entry;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/documents/compress". */
public class DocumentsCompressRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  private String getArchiveDownloadUrl(final S3PresignerService s3, final String stagingBucket,
      final String objectPath) {
    final String zipContentType = "application/zip";
    Duration duration = Duration.ofHours(1);
    PresignGetUrlConfig config = new PresignGetUrlConfig()
        .contentDispositionByPath(objectPath, false).contentType(zipContentType);
    URL url = s3.presignGetUrl(stagingBucket, objectPath, duration, null, config);
    return url.toString();
  }

  @Override
  public String getRequestUrl() {
    return "/documents/compress";
  }

  private String getS3Key(final String siteId, final String compressionId, final boolean isZip) {
    final String key = String.format("tempfiles/%s", createS3Key(siteId, compressionId));
    final String fileType = isZip ? ".zip" : ".json";
    return key + fileType;
  }

  /**
   * Create S3 Object that is processed in the StagingS3Create lambda.
   * 
   * @param requestBodyObject {@link DynamicObject}
   * @param siteId {@link String}
   * @param compressionId {@link String}
   * @param downloadUrl {@link String}
   * @return {@link DynamicObject}
   */
  private DynamicObject getS3TaskObject(final DynamicObject requestBodyObject, final String siteId,
      final String compressionId, final String downloadUrl) {
    final String documentIdsKey = "documentIds";
    final Object documentIds = requestBodyObject.get(documentIdsKey);
    final String compressionIdKey = "compressionId";
    final String downloadUrlKey = "downloadUrl";
    final String siteIdKey = "siteId";
    return new DynamicObject(Map.ofEntries(entry(documentIdsKey, documentIds),
        entry(compressionIdKey, compressionId), entry(downloadUrlKey, downloadUrl),
        entry(siteIdKey, siteId == null ? DEFAULT_SITE_ID : siteId)));
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    boolean access = authorization.getPermissions().contains(ApiPermission.READ);
    return Optional.of(Boolean.valueOf(access));
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = UUID.randomUUID().toString();
    String compressionTaskS3Key = getS3Key(siteId, documentId, false);

    S3Service s3 = awsServices.getExtension(S3Service.class);
    S3PresignerService s3Presigner = awsServices.getExtension(S3PresignerService.class);
    DynamicObject requestBodyObject = fromBodyToDynamicObject(event);

    DocumentService documentService = awsServices.getExtension(DocumentService.class);
    validateRequestBody(documentService, requestBodyObject, siteId);

    String stagingBucket = awsServices.environment("STAGE_DOCUMENTS_S3_BUCKET");
    String downloadUrl =
        getArchiveDownloadUrl(s3Presigner, stagingBucket, getS3Key(siteId, documentId, true));

    DynamicObject taskObject = getS3TaskObject(requestBodyObject, siteId, documentId, downloadUrl);

    putObjectToStaging(s3, stagingBucket, compressionTaskS3Key, GSON.toJson(taskObject));
    ApiMapResponse response = new ApiMapResponse(Map.of("downloadUrl", downloadUrl));

    return new ApiRequestHandlerResponse(SC_CREATED, response);
  }

  /**
   * Write document compression request to S3 Staging bucket.
   * 
   * @param s3 {@link S3Service}
   * @param bucket {@link String}
   * @param key {@link String}
   * @param content {@link String}
   */
  private void putObjectToStaging(final S3Service s3, final String bucket, final String key,
      final String content) {
    final String jsonContentType = "application/json";
    final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    s3.putObject(bucket, key, bytes, jsonContentType);
  }

  /**
   * Validate Request body.
   * 
   * @param documentService {@link DocumentService}
   * @param requestBody {@link DynamicObject}
   * @param siteId {@link String}
   * @throws ValidationException ValidationException
   */
  private void validateRequestBody(final DocumentService documentService,
      final DynamicObject requestBody, final String siteId) throws ValidationException {

    Collection<ValidationError> errors = new ArrayList<>();

    try {
      List<String> documentIds = requestBody.getStringList("documentIds");

      if (documentIds.isEmpty()) {
        errors.add(new ValidationErrorImpl().key("documentIds").error("is required"));
      } else {
        for (String documentId : documentIds) {
          if (!documentService.exists(siteId, documentId)) {
            errors.add(new ValidationErrorImpl().key("documentId")
                .error(String.format("Document '%s' does not exist", documentId)));
          }
        }
      }

    } catch (Exception e) {
      errors.add(new ValidationErrorImpl().key("documentIds").error("is required"));
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
