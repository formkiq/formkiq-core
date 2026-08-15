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

import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.numbering.NumberingSequence;
import com.formkiq.stacks.dynamodb.numbering.NumberingSequenceRecord;
import com.formkiq.stacks.dynamodb.numbering.NumberingSequenceService;
import com.formkiq.validation.ValidationException;

/** Handler for {@code /sites/{siteId}/numberingSequences/{attributeKey}}. */
public class NumberingSequenceRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Constructor. */
  public NumberingSequenceRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    String siteId = getPathParameterSiteId(event);
    String attributeKey = getPathParameter(event, "attributeKey");
    NumberingSequence sequence =
        awsServices.getExtension(NumberingSequenceService.class).find(siteId, attributeKey);
    if (sequence == null) {
      throw new NotFoundException("Numbering sequence '" + attributeKey + "' not found");
    }
    return response(sequence);
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/numberingSequences/{attributeKey}";
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    String siteId = getPathParameterSiteId(event);
    String attributeKey = getPathParameter(event, "attributeKey");
    validateAttribute(awsServices, siteId, attributeKey);

    SetNumberingSequenceRequest request =
        JsonToObject.fromJson(awsServices, event, SetNumberingSequenceRequest.class);
    String timezone = request.timezone() != null ? request.timezone() : "UTC";
    NumberingSequenceRecord record = NumberingSequenceRecord.builder().attributeKey(attributeKey)
        .pattern(request.pattern()).startAt(request.startAt()).padding(request.padding())
        .reset(request.reset()).timezone(timezone).build(siteId);
    NumberingSequence sequence =
        awsServices.getExtension(NumberingSequenceService.class).save(siteId, record);
    return response(sequence);
  }

  private ApiRequestHandlerResponse response(final NumberingSequence sequence) {
    return ApiRequestHandlerResponse.builder().ok()
        .body(Map.of("numberingSequence", sequence.toMap())).build();
  }

  private void validateAttribute(final AwsServiceCache awsServices, final String siteId,
      final String attributeKey) {
    AttributeRecord attribute =
        awsServices.getExtension(AttributeService.class).getAttribute(siteId, attributeKey);
    if (attribute == null) {
      throw ValidationException.builder()
          .error("attributeKey", "attribute '" + attributeKey + "' not found").build();
    }
    if (!AttributeDataType.STRING.equals(attribute.getDataType())) {
      throw ValidationException.builder()
          .error("attributeKey", "attribute '" + attributeKey + "' must use dataType 'STRING'")
          .build();
    }
  }
}
