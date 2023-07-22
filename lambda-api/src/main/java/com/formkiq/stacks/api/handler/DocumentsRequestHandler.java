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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;

/** {@link ApiGatewayRequestHandler} for "/documents". */
public class DocumentsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link SimpleDateFormat}. */
  private SimpleDateFormat df;
  /** {@link DocumentIdRequestHandler}. */
  private DocumentIdRequestHandler handler = new DocumentIdRequestHandler();

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
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);

    final int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    final PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    String tz = getParameter(event, "tz");
    String dateString = getParameter(event, "date");

    ZonedDateTime date = transformToDate(logger, awsservice, documentService, dateString, tz);

    if (awsservice.debug()) {
      logger.log("search for document using date: " + date);
    }

    String siteId = authorizer.getSiteId();
    final PaginationResults<DocumentItem> results =
        documentService.findDocumentsByDate(siteId, date, ptoken, limit);

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

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

  @Override
  public String getRequestUrl() {
    return "/documents";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {
    return this.handler.patch(logger, event, authorizer, awsservice);
  }

  /**
   * Transform {@link String} to {@link ZonedDateTime}.
   *
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param documentService {@link DocumentService}
   * @param dateString {@link String}
   * @param tz {@link String}
   * @return {@link Date}
   * @throws BadException BadException
   */
  private ZonedDateTime transformToDate(final LambdaLogger logger, final AwsServiceCache awsservice,
      final DocumentService documentService, final String dateString, final String tz)
      throws BadException {

    ZonedDateTime date = null;

    if (dateString != null) {
      try {
        date = DateUtil.toDateTimeFromString(dateString, tz);

        if (awsservice.debug()) {
          logger.log("searching using date parameter: " + dateString + " and tz " + tz);
        }

      } catch (ZoneRulesException e) {
        throw new BadException("Invalid date string: " + dateString);
      }
    } else {

      date = documentService.findMostDocumentDate();

      if (date == null) {
        date = ZonedDateTime.now();

        if (awsservice.debug()) {
          logger.log("searching using default date: " + date);
        }

      } else {
        if (awsservice.debug()) {
          logger.log("searching using Most Recent Document Date: " + date);
        }
      }
    }

    return date;
  }
}
