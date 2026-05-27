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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;

import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/** {@link ApiGatewayRequestHandler} for "/attributes/{key}/allowedValues". */
public class AttributeAllowedValuesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * Get Allowed Values.
   * 
   * @param schemaService {@link SchemaService}
   * @param siteId {@link String}
   * @param classificationId {@link String}
   * @param attributeKey {@link String}
   * @return List String
   */
  public List<String> fetchAllowedValues(final SchemaService schemaService, final String siteId,
      final String classificationId, final String attributeKey) {
    return schemaService.getAttributeAllowedValues(siteId, attributeKey);
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String key = event.getPathParameter("key");
    String classificationId = event.getPathParameter("classificationId");
    String locale = event.getQueryStringParameter("locale");

    SchemaService schemaService = awsServices.getExtension(SchemaService.class);
    List<String> allowedValues =
        notNull(fetchAllowedValues(schemaService, siteId, classificationId, key));
    Map<String, String> localization = schemaService.getAttributeAllowedValuesLocalization(siteId,
        classificationId, key, allowedValues, locale);

    if (allowedValues.isEmpty()) {
      AttributeService service = awsServices.getExtension(AttributeService.class);
      AttributeRecord attribute = service.getAttribute(siteId, key);
      if (attribute == null) {
        throw new NotFoundException("Attribute " + key + " not found");
      }
    }

    Map<String, Object> map =
        Map.of("allowedValues", allowedValues, "localizedAllowedValues", localization);
    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  @Override
  public String getRequestUrl() {
    return "/attributes/{key}/allowedValues";
  }
}
