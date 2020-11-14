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
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getCallingCognitoUsername;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getLimit;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getPagination;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getPathParameter;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_CREATED;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMapResponse;
import com.formkiq.stacks.api.ApiMessageResponse;
import com.formkiq.stacks.api.ApiResponse;
import com.formkiq.stacks.api.BadException;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;
import com.formkiq.stacks.dynamodb.Preset;
import com.formkiq.stacks.dynamodb.PresetTag;

/**
 * {@link RequestHandler} for GET / POST "/presets/{preset}/tags" & DELETE
 * /presets/{preset}/tags/{tagKey}.
 */
public class PresetsTagsRequestHandler implements RequestHandler {

  /**
   * constructor.
   * 
   */
  public PresetsTagsRequestHandler() {}

  @Override
  public boolean isReadonly(final String method) {
    return "get".equalsIgnoreCase(method);
  }

  /**
   * Processes GET {@link ApiGatewayRequestEvent} request.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsservice {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse get(final LambdaLogger logger,
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

  /**
   * Processes DELETE {@link ApiGatewayRequestEvent} request.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsservice {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = getSiteId(event);
    String presetId = getPathParameter(event, "presetId");
    String tagKey = getPathParameter(event, "tagKey");

    if (presetId == null || tagKey == null) {
      throw new BadException("missing 'presetId' or 'tagKey'");
    }

    String decodedTagKey = URLDecoder.decode(tagKey, StandardCharsets.UTF_8);
    Optional<PresetTag> o =
        awsservice.documentService().findPresetTag(siteId, presetId, decodedTagKey);

    if (!o.isPresent()) {
      throw new NotFoundException(
          MessageFormat.format("{0} is not found on preset {1}", tagKey, presetId));
    }

    awsservice.documentService().deletePresetTag(siteId, presetId, decodedTagKey);

    ApiResponse resp = new ApiMessageResponse("Removed '" + tagKey + "'");

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  /**
   * Processes POST {@link ApiGatewayRequestEvent} request.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsservice {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  private ApiRequestHandlerResponse post(final LambdaLogger logger,
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
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    ApiRequestHandlerResponse response = null;
    String method = event.getHttpMethod().toLowerCase();

    switch (method) {
      case "get":
        response = get(logger, event, authorizer, awsservice);
        break;

      case "delete":
        response = delete(logger, event, authorizer, awsservice);
        break;

      case "post":
      case "patch":
        response = post(logger, event, authorizer, awsservice);
        break;
      default:
        throw new BadException("Unsupport method " + method);
    }

    return response;
  }
}
