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

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.createPagination;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getLimit;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getPagination;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMapResponse;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;
import com.formkiq.stacks.dynamodb.Preset;

/** {@link RequestHandler} for GET "/presets". */
public class PresetsGetRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public PresetsGetRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    ApiPagination pagination = getPagination(awsservice.documentCacheService(), event);

    final int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    final PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    final String siteId = getSiteId(event);

    final PaginationResults<Preset> results = awsservice.documentService().findPresets(siteId, null,
        PresetTypes.TAGGING.name(), null, ptoken, limit);

    final ApiPagination current =
        createPagination(awsservice.documentCacheService(), event, pagination, results, limit);

    PresetToMapResponse convert =
        new PresetToMapResponse(siteId != null ? siteId : DEFAULT_SITE_ID);

    List<Map<String, Object>> items =
        results.getResults().stream().map(d -> convert.apply(d)).collect(Collectors.toList());

    Map<String, Object> map = new HashMap<>();
    map.put("presets", items);

    if (current.hasNext()) {
      map.put("next", current.getNext());
    }

    if (current.getPrevious() != null) {
      map.put("previous", current.getPrevious());
    }

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }
}
