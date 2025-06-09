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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.locale.LocaleTypeRecord;
import com.formkiq.stacks.dynamodb.locale.LocaleRecordToMap;
import com.formkiq.stacks.dynamodb.locale.LocaleService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

import java.util.List;
import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/**
 * {@link ApiGatewayRequestHandler} for "/sites/{siteId}/locales/{locale}/resourceItems/{itemKey}".
 */
public class SitesLocaleResourceItemRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   */
  public SitesLocaleResourceItemRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    AddLocaleResourceItemRequest record =
        fromBodyToObject(event, AddLocaleResourceItemRequest.class);

    String siteId = authorization.getSiteId();
    String locale = event.getPathParameter("locale");
    String itemKey = event.getPathParameter("itemKey");
    LocaleService service = awsservice.getExtension(LocaleService.class);

    LocaleTypeRecord item = record.getResourceItem();
    item.setLocale(locale);
    if (!itemKey.equals(item.getItemKey())) {
      throw new NotFoundException("itemKey '" + itemKey + "' not found");
    }

    List<ValidationError> errors = service.save(siteId, List.of(item));

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "set item '" + item.getItemKey() + "' successfully")));
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String locale = event.getPathParameter("locale");
    String itemKey = event.getPathParameter("itemKey");

    LocaleService service = awsservice.getExtension(LocaleService.class);

    LocaleTypeRecord item = service.find(siteId, locale, itemKey);
    if (item == null) {
      throw new NotFoundException("ItemKey '" + itemKey + "' not found");
    }

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("resourceItem", new LocaleRecordToMap().apply(item))));
  }

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String locale = event.getPathParameter("locale");
    String itemKey = event.getPathParameter("itemKey");

    LocaleService service = awsservice.getExtension(LocaleService.class);

    boolean deleted = service.delete(siteId, locale, itemKey);
    if (!deleted) {
      throw new NotFoundException("ItemKey '" + itemKey + "' not found");
    }

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "ItemKey '" + itemKey + "' successfully deleted")));
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/locales/{locale}/resourceItems/{itemKey}";
  }
}
