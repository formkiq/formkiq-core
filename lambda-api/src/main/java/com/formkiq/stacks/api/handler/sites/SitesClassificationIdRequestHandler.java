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
package com.formkiq.stacks.api.handler.sites;

import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.model.SearchQueryBuilder;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.schemas.ClassificationRecord;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;

import java.util.Map;

/** {@link ApiGatewayRequestHandler} for "/sites/{siteId}/classifications/{classificationId}". */
public class SitesClassificationIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String classificationId = event.getPathParameter("classificationId");

    DocumentSearchService searchService = awsServices.getExtension(DocumentSearchService.class);

    SearchQuery req = new SearchQueryBuilder()
        .attribute(new SearchAttributeCriteria(AttributeKeyReserved.CLASSIFICATION.getKey(), null,
            classificationId, null, null))
        .build();
    Pagination<DynamicDocumentItem> items = searchService.search(siteId, req, null, null, 1);
    if (!items.getResults().isEmpty()) {
      throw new BadException("Classification '" + classificationId + "' in use");
    }

    SchemaService service = awsServices.getExtension(SchemaService.class);
    if (!service.deleteClassification(siteId, classificationId)) {
      throw new NotFoundException("Classification '" + classificationId + "' not found");
    }

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Classification '" + classificationId + "' deleted").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String classificationId = event.getPathParameter("classificationId");

    SchemaService service = awsServices.getExtension(SchemaService.class);
    ClassificationRecord result = service.findClassification(siteId, classificationId);

    if (result == null) {
      throw new NotFoundException("Classification '" + classificationId + "' not found");
    }

    String name = result.getName();
    Schema schema = service.getSchema(result);
    String locale = event.getQueryStringParameter("locale");
    service.updateLocalization(siteId, classificationId, schema.getAttributes(), locale);

    return ApiRequestHandlerResponse.builder().ok()
        .body(Map.of("classification", Map.of("name", name, "attributes", schema.getAttributes())))
        .build();
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/classifications/{classificationId}";
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorizer, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorizer.getSiteId();
    String classificationId = event.getPathParameter("classificationId");

    AddClassificationRequest request =
        JsonToObject.fromJson(awsServices, event, AddClassificationRequest.class);
    Schema schema = request.getClassification();

    SchemaService service = awsServices.getExtension(SchemaService.class);
    service.setClassification(siteId, classificationId, schema.getName(), schema,
        authorizer.getUsername());

    return ApiRequestHandlerResponse.builder().ok().body("message", "Set Classification").build();
  }
}
