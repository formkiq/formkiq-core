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
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiPagination;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.ApiResponseStatus;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;
import com.formkiq.stacks.dynamodb.Preset;
import com.formkiq.stacks.dynamodb.PresetTag;

/** {@link ApiGatewayRequestHandler} for "/presets". */
public class PresetsRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public PresetsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
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
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String presetId = getPathParameter(event, "presetId");
    PresetWithTags item = fromBodyToObject(logger, event, PresetWithTags.class);

    validate(presetId, item);

    String siteId = getSiteId(event);
    ApiRequestHandlerResponse response = null;
    Preset preset = createPreset(event, item, presetId);

    List<PresetTag> tags = createTags(event, preset, item.getTags());

    awsservice.documentService().savePreset(siteId, preset.getId(), preset.getType(), preset, tags);

    ApiResponseStatus status = SC_CREATED;
    response = new ApiRequestHandlerResponse(status, new ApiMapResponse(
        Map.of("id", preset.getId(), "siteId", siteId != null ? siteId : DEFAULT_SITE_ID)));

    return response;
  }

  /**
   * Create {@link List} {@link PresetTag}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param preset {@link Preset}
   * @param tags {@link List} {@link PresetTag}
   * @return {@link List} {@link PresetTag}
   */
  private List<PresetTag> createTags(final ApiGatewayRequestEvent event, final Preset preset,
      final List<PresetTag> tags) {

    String userId = getCallingCognitoUsername(event);
    List<PresetTag> list = new ArrayList<>();

    if (tags != null) {
      for (PresetTag tag : tags) {
        tag.setInsertedDate(preset.getInsertedDate());
        tag.setUserId(userId);
      }
    }

    return list;
  }

  /**
   * Create Preset.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param item {@link PresetWithTags}
   * @param presetId {@link String}
   * @return {@link Preset}s
   */
  private Preset createPreset(final ApiGatewayRequestEvent event, final PresetWithTags item,
      final String presetId) {

    String type = PresetTypes.TAGGING.name();
    String userId = getCallingCognitoUsername(event);
    Date date = new Date();

    Preset preset = new Preset();
    preset.setInsertedDate(date);
    preset.setUserId(userId);
    preset.setName(item != null ? item.getName() : null);
    preset.setType(type);
    preset.setId(presetId != null ? presetId : UUID.randomUUID().toString());

    return preset;
  }

  /**
   * Validate Request.
   * 
   * @param presetId {@link String}
   * @param item {@link PresetWithTags}
   * @throws BadException BadException
   */
  private void validate(final String presetId, final PresetWithTags item) throws BadException {

    if (item == null || item.getName() == null) {
      throw new BadException("invalid body");
    }
  }

  @Override
  public String getRequestUrl() {
    return "/presets";
  }
}
