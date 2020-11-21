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

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.api.ApiDocumentVersion;
import com.formkiq.stacks.api.ApiDocumentVersionsResponse;
import com.formkiq.stacks.dynamodb.DateUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/versions". */
public class DocumentVersionsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentVersionsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    SimpleDateFormat df = new SimpleDateFormat(GsonUtil.DATE_FORMAT);

    String siteId = getSiteId(event);
    String documentId = event.getPathParameters().get("documentId");
    String next = getParameter(event, "next");

    String tz = getParameter(event, "tz");
    ZoneOffset offset = DateUtil.getZoneOffset(tz);
    df.setTimeZone(TimeZone.getTimeZone(offset));

    String s3key = createDatabaseKey(siteId, documentId);

    S3Service s3service = awsservice.s3Service();
    try (S3Client s3 = s3service.buildClient()) {

      ListObjectVersionsResponse response =
          s3service.getObjectVersions(s3, awsservice.documents3bucket(), s3key, next);

      List<ApiDocumentVersion> list = response.versions().stream().map(v -> {

        ApiDocumentVersion dv = new ApiDocumentVersion();
        dv.setVersionId(v.versionId());

        Date date = Date.from(v.lastModified());
        dv.setLastModifiedDate(df.format(date));
        return dv;

      }).collect(Collectors.toList());

      ApiDocumentVersionsResponse resp = new ApiDocumentVersionsResponse();
      resp.setNext(response.nextKeyMarker());
      resp.setVersions(list);

      return new ApiRequestHandlerResponse(SC_OK, resp);
    }
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/versions";
  }
}
