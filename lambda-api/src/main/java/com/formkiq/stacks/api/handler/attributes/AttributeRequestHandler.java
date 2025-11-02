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

import java.util.Collection;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/attributes/{key}". */
public class AttributeRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorizer, final AwsServiceCache awsServices) throws Exception {

    AttributeService service = awsServices.getExtension(AttributeService.class);

    String siteId = authorizer.getSiteId();
    String key = event.getPathParameter("key");

    AttributeValidationAccess validationAccess =
        authorizer.isAdminOrGovern(siteId) ? AttributeValidationAccess.ADMIN_DELETE
            : AttributeValidationAccess.DELETE;
    Collection<ValidationError> errors = service.deleteAttribute(validationAccess, siteId, key);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Attribute '" + key + "' deleted").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String key = event.getPathParameter("key");
    AttributeService service = awsServices.getExtension(AttributeService.class);
    AttributeRecord attribute = service.getAttribute(siteId, key);
    if (attribute == null) {
      throw new NotFoundException("Attribute " + key + " not found");
    }

    Map<String, Object> map = Map.of("attribute", new AttributeRecordToMap().apply(attribute));
    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  @Override
  public String getRequestUrl() {
    return "/attributes/{key}";
  }

  @Override
  public ApiRequestHandlerResponse patch(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorizer, final AwsServiceCache awsServices) throws Exception {

    AttributeService service = awsServices.getExtension(AttributeService.class);

    String siteId = authorizer.getSiteId();
    String key = event.getPathParameter("key");

    AddAttributeRequest addAttribute =
        JsonToObject.fromJson(awsServices, event, AddAttributeRequest.class);
    AddAttribute attribute = addAttribute.getAttribute();

    AttributeType type = attribute.getType();
    AttributeValidationAccess access =
        authorizer.isAdminOrGovern(siteId) ? AttributeValidationAccess.ADMIN_CREATE
            : AttributeValidationAccess.CREATE;

    service.updateAttribute(access, siteId, key, type, attribute.getWatermark());

    return ApiRequestHandlerResponse.builder().ok()
        .body(Map.of("message", "Attribute '" + key + "' updated")).build();
  }
}
