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

import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMessageResponse;
import com.formkiq.stacks.api.ApiResponse;
import com.formkiq.stacks.api.NotFoundException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** {@link RequestHandler} for DELETE "/documents". */
public class DocumentsDeleteRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentsDeleteRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String documentBucket = awsservice.documents3bucket();
    String documentId = event.getPathParameters().get("documentId");
    logger.log("deleting object " + documentId + " from bucket '" + documentBucket + "'");

    try {

      S3Service s3Service = awsservice.s3Service();
      try (S3Client s3 = s3Service.buildClient()) {
        S3ObjectMetadata md = s3Service.getObjectMetadata(s3, documentBucket, documentId);

        if (md.isObjectExists()) {
          s3Service.deleteObject(s3, documentBucket, documentId);

          ApiResponse resp = new ApiMessageResponse("'" + documentId + "' object deleted");
          return new ApiRequestHandlerResponse(SC_OK, resp);
        }
      }

      throw new NotFoundException("Document " + documentId + " not found.");

    } catch (S3Exception e) {

      if (e.statusCode() == SC_NOT_FOUND.getStatusCode()) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }

      throw e;
    }
  }

  @Override
  public boolean isReadonly(final String method) {
    return false;
  }
}
