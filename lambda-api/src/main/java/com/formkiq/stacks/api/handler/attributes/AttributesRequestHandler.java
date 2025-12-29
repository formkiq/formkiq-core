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
package com.formkiq.stacks.api.handler.attributes;

import java.util.HashMap;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.base64.Pagination;

/** {@link ApiGatewayRequestHandler} for "/attributes". */
public class AttributesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    CacheService cacheService = awsServices.getExtension(CacheService.class);
    ApiPagination pagination = getPagination(cacheService, event);
    int limit =
        pagination != null ? pagination.getLimit() : getLimit(awsServices.getLogger(), event);
    String nextToken = pagination != null ? pagination.getNextToken() : null;

    String siteId = authorization.getSiteId();
    AttributeService service = awsServices.getExtension(AttributeService.class);
    Pagination<AttributeRecord> attributes = service.findAttributes(siteId, nextToken, limit);

    Map<String, Object> map = new HashMap<>();
    map.put("attributes",
        attributes.getResults().stream().map(new AttributeRecordToMap()).toList());

    ApiPagination current =
        createPagination(cacheService, event, pagination, attributes.getNextToken(), limit);

    if (current.hasNext()) {
      map.put("next", current.getNext());
    }

    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  @Override
  public String getRequestUrl() {
    return "/attributes";
  }

  private boolean isWatermark(final AddAttribute attribute) {
    return AttributeDataType.WATERMARK.equals(attribute.getDataType());
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorizer, final AwsServiceCache awsServices) throws Exception {

    AttributeService service = awsServices.getExtension(AttributeService.class);

    String siteId = authorizer.getSiteId();

    AddAttributeRequest addAttribute =
        JsonToObject.fromJson(awsServices, event, AddAttributeRequest.class);
    AddAttribute attribute = addAttribute.getAttribute();

    String key = attribute.getKey();
    AttributeDataType dataType = attribute.getDataType();
    AttributeType type = attribute.getType();
    AttributeValidationAccess access =
        authorizer.isAdminOrGovern(siteId) ? AttributeValidationAccess.ADMIN_CREATE
            : AttributeValidationAccess.CREATE;

    if (isWatermark(attribute)) {
      service.addWatermarkAttribute(siteId, key, attribute.getWatermark());
    } else {
      service.addAttribute(access, siteId, key, dataType, type);
    }

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Attribute '" + key + "' created").build();
  }
}
