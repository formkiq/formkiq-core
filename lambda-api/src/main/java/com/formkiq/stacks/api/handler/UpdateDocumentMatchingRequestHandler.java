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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequest;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequestValidator;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequestValidatorImpl;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/documents/tags". */
public class UpdateDocumentMatchingRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Extension for FormKiQ config file. */
  public static final String FORMKIQ_DOC_EXT = ".fkb64";

  /**
   * constructor.
   *
   */
  public UpdateDocumentMatchingRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return "/documents/tags";
  }

  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    UpdateMatchingDocumentTagsRequest o =
        fromBodyToObject(event, UpdateMatchingDocumentTagsRequest.class);

    validate(o);

    String siteId = authorization.getSiteId();

    String key = createS3Key(siteId, "patch_documents_tags_" + UUID.randomUUID() + FORMKIQ_DOC_EXT);
    String stageS3Bucket = awsservice.environment("STAGE_DOCUMENTS_S3_BUCKET");

    S3Service s3 = awsservice.getExtension(S3Service.class);

    String body = ApiGatewayRequestEventUtil.getBodyAsString(event);
    s3.putObject(stageS3Bucket, key, body.getBytes(StandardCharsets.UTF_8), "application/json");
    s3.setObjectTag(stageS3Bucket, key, "userId", authorization.getUsername());

    ApiMapResponse resp = new ApiMapResponse(Map.of("message", "received update tags request"));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  /**
   * Validate {@link UpdateMatchingDocumentTagsRequest}.
   * 
   * @param o {@link UpdateMatchingDocumentTagsRequest}
   * @throws ValidationException ValidationException
   */
  private void validate(final UpdateMatchingDocumentTagsRequest o) throws ValidationException {
    UpdateMatchingDocumentTagsRequestValidator validator =
        new UpdateMatchingDocumentTagsRequestValidatorImpl();

    Collection<ValidationError> errors = validator.validate(o);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
