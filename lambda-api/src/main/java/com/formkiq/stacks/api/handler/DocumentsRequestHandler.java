/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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

    ZonedDateTime date = transformToDate(logger, dateString, tz);

    final String siteId = getSiteId(event);
    final PaginationResults<DocumentItem> results =
        awsservice.documentService().findDocumentsByDate(siteId, date, ptoken, limit);

    final ApiPagination current =
        createPagination(awsservice.documentCacheService(), event, pagination, results, limit);

    List<DocumentItem> documents = subList(results.getResults(), limit);

    List<DynamicDocumentItem> items = documents.stream()
        .map(m -> new DocumentItemToDynamicDocumentItem().apply(m)).collect(Collectors.toList());
    items.forEach(i -> i.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID));

    // DocumentItemToApiDocumentItemResponse convert =
    // new DocumentItemToApiDocumentItemResponse(siteId != null ? siteId : DEFAULT_SITE_ID);

    // List<ApiDocumentItemResponse> items =
    // documents.stream().map(d -> convert.apply(d)).collect(Collectors.toList());

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
   * @param dateString {@link String}
   * @param tz {@link String}
   * @return {@link Date}
   * @throws BadException BadException
   */
  private ZonedDateTime transformToDate(final LambdaLogger logger, final String dateString,
      final String tz) throws BadException {

    ZonedDateTime date = ZonedDateTime.now();

    if (dateString != null) {
      try {
        date = DateUtil.toDateTimeFromString(dateString, tz);
      } catch (ZoneRulesException e) {
        throw new BadException("Invalid date string: " + dateString);
      }
    }

    return date;
  }

  @Override
  public String getRequestUrl() {
    return "/documents";
  }
}
