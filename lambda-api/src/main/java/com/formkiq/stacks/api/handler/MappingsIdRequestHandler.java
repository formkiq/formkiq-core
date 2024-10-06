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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.transformers.MappingRecordToMap;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.stacks.dynamodb.mappings.MappingService;

import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/mappings/{mappingId}". */
public class MappingsIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String mappingId = event.getPathParameters().get("mappingId");

    MappingService service = awsServices.getExtension(MappingService.class);
    MappingRecord mapping = service.getMapping(siteId, mappingId);

    if (mapping == null) {
      throw new NotFoundException("Mapping '" + mappingId + "' not found");
    }

    Map<String, Object> map = Map.of("mapping", new MappingRecordToMap(service).apply(mapping));
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/mappings/{mappingId}";
  }

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorizer,
      final AwsServiceCache awsServices) throws Exception {

    MappingService service = awsServices.getExtension(MappingService.class);

    String siteId = authorizer.getSiteId();
    String mappingId = event.getPathParameters().get("mappingId");

    if (!service.deleteMapping(siteId, mappingId)) {
      throw new NotFoundException("Mapping '" + mappingId + "' not found");
    }

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "Mapping '" + mappingId + "' deleted")));
  }

  @Override
  public ApiRequestHandlerResponse put(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorizer,
      final AwsServiceCache awsServices) throws Exception {

    MappingService service = awsServices.getExtension(MappingService.class);

    String siteId = authorizer.getSiteId();
    String mappingId = event.getPathParameters().get("mappingId");

    AddMappingRequest req = fromBodyToObject(event, AddMappingRequest.class);
    service.saveMapping(siteId, mappingId, req.getMapping());

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "Mapping set")));
  }
}
