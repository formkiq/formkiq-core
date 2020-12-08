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
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiPagination;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.dynamodb.DateUtil;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;
import software.amazon.awssdk.utils.StringUtils;

/** {@link ApiGatewayRequestHandler} for "/documents". */
public class DocumentsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link SimpleDateFormat}. */
  private SimpleDateFormat df;

  /**
   * constructor.
   *
   */
  public DocumentsRequestHandler() {
    this.df = new SimpleDateFormat("yyyy-MM-dd");

    TimeZone tz = TimeZone.getTimeZone("UTC");
    this.df.setTimeZone(tz);
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {
    return new DocumentIdRequestHandler().patch(logger, event, authorizer, awsservice);
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    ApiPagination pagination = getPagination(awsservice.documentCacheService(), event);

    final int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    final PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    String tz = getParameter(event, "tz");
    String dateString = getParameter(event, "date");

    if (StringUtils.isBlank(dateString)) {

      if (StringUtils.isNotBlank(tz)) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        ZoneOffset offset = DateUtil.getZoneOffset(tz);
        sdf.setTimeZone(TimeZone.getTimeZone(offset));
        dateString = sdf.format(new Date());
      } else {
        dateString = this.df.format(new Date());
      }
    }

    ZonedDateTime date = transformToDate(logger, awsservice, dateString, tz);

    final String siteId = getSiteId(event);
    final PaginationResults<DocumentItem> results =
        awsservice.documentService().findDocumentsByDate(siteId, date, ptoken, limit);

    final ApiPagination current =
        createPagination(awsservice.documentCacheService(), event, pagination, results, limit);

    List<DocumentItem> documents = subList(results.getResults(), limit);

    List<DynamicDocumentItem> items = documents.stream()
        .map(m -> new DocumentItemToDynamicDocumentItem().apply(m)).collect(Collectors.toList());
    items.forEach(i -> i.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID));

    Map<String, Object> map = new HashMap<>();
    map.put("documents", items);
    map.put("previous", current.getPrevious());
    map.put("next", current.hasNext() ? current.getNext() : null);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  /**
   * Transform {@link String} to {@link ZonedDateTime}.
   *
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param dateString {@link String}
   * @param tz {@link String}
   * @return {@link Date}
   * @throws BadException BadException
   */
  private ZonedDateTime transformToDate(final LambdaLogger logger, final AwsServiceCache awsservice,
      final String dateString, final String tz) throws BadException {

    ZonedDateTime date = null;

    if (dateString != null) {
      try {
        date = DateUtil.toDateTimeFromString(dateString, tz);
      } catch (ZoneRulesException e) {
        throw new BadException("Invalid date string: " + dateString);
      }
    } else {
      
      date = awsservice.documentService().findMostDocumentDate();
      if (date == null) {
        date = ZonedDateTime.now();
      }
    }

    return date;
  }

  @Override
  public String getRequestUrl() {
    return "/documents";
  }
}
