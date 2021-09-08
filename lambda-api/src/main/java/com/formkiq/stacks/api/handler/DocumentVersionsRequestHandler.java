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

    String siteId = authorizer.getSiteId();
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
