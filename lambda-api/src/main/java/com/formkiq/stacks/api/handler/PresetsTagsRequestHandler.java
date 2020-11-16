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

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_CREATED;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiMessageResponse;
import com.formkiq.lambda.apigateway.ApiPagination;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.ApiResponse;
import com.formkiq.lambda.apigateway.ApiResponseStatus;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;
import com.formkiq.stacks.dynamodb.Preset;
import com.formkiq.stacks.dynamodb.PresetTag;

/**
 * {@link ApiGatewayRequestHandler} for GET / POST "/presets/{preset}/tags".
 */
public class PresetsTagsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   * 
   */
  public PresetsTagsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {
    ApiPagination pagination = getPagination(awsservice.documentCacheService(), event);

    final int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    final PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    final String siteId = getSiteId(event);
    String presetId = getPathParameter(event, "presetId");

    if (presetId == null) {
      throw new BadException("'presetId' required");
    }

    PaginationResults<PresetTag> results =
        awsservice.documentService().findPresetTags(siteId, presetId, ptoken, limit);

    final ApiPagination current =
        createPagination(awsservice.documentCacheService(), event, pagination, results, limit);

    List<Map<String, String>> items = results.getResults().stream()
        .map(d -> Map.of("key", d.getKey())).collect(Collectors.toList());

    Map<String, Object> map = new HashMap<>();
    map.put("tags", items);

    if (current.hasNext()) {
      map.put("next", current.getNext());
    }

    if (current.getPrevious() != null) {
      map.put("previous", current.getPrevious());
    }

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }


  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {
    return post(logger, event, authorizer, awsservice);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    final boolean debug = awsservice.debug();

    String siteId = getSiteId(event);
    String presetId = getPathParameter(event, "presetId");

    final ApiResponseStatus status = SC_CREATED;

    Map<String, String> item = fromBodyToObject(logger, event, Map.class);

    if (presetId == null || item == null || !item.containsKey("key")) {
      throw new BadException("missing 'presetId' or 'key'");
    }

    DocumentService service = awsservice.documentService();
    Optional<Preset> p = service.findPreset(siteId, presetId);
    if (p.isEmpty()) {
      throw new BadException("invalid 'presetId'");
    }

    String userId = getCallingCognitoUsername(event);

    PresetTag tag = new PresetTag();
    tag.setInsertedDate(new Date());
    tag.setKey(item.get("key"));
    tag.setUserId(userId);

    String id = p.get().getId();
    String type = p.get().getType();

    if (debug) {
      logger.log(MessageFormat.format("adding presettags to preset {0}, type: {1} tags: {2}", id,
          type, GSON.toJson(Arrays.asList(tag))));
    }

    service.savePreset(siteId, id, type, null, Arrays.asList(tag));

    ApiResponse resp = new ApiMessageResponse("Added tags");
    return new ApiRequestHandlerResponse(status, resp);
  }

  @Override
  public String getRequestUrl() {
    return "/presets/{preset}/tags";
  }
}
