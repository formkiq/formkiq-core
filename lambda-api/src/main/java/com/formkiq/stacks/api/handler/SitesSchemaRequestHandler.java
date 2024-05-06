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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.Collection;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.stacks.dynamodb.schemas.SitesSchemaRecord;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;

/** {@link ApiGatewayRequestHandler} for "/sites/{siteId}/schema/document". */
public class SitesSchemaRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link Gson}. */
  private Gson gson = GsonUtil.getInstance();

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    SchemaService service = awsServices.getExtension(SchemaService.class);
    SitesSchemaRecord record = service.getSitesSchemaRecord(siteId);

    if (record == null) {
      throw new NotFoundException("Sites Schema not found");
    }

    String json = record.getSchema();
    Schema schema = this.gson.fromJson(json, Schema.class);

    Map<String, Object> map =
        Map.of("name", schema.getName(), "attributes", schema.getAttributes());
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/schema/document";
  }

  @Override
  public ApiRequestHandlerResponse put(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorizer,
      final AwsServiceCache awsServices) throws Exception {

    SchemaService service = awsServices.getExtension(SchemaService.class);

    String siteId = authorizer.getSiteId();

    Schema schema = fromBodyToObject(event, Schema.class);
    Collection<ValidationError> errors =
        service.setSitesSchema(siteId, schema.getName(), this.gson.toJson(schema), schema);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "Sites Schema set")));
  }
}
