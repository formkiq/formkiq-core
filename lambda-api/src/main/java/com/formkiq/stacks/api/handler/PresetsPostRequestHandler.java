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

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getCallingCognitoUsername;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getPathParameter;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_CREATED;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMapResponse;
import com.formkiq.stacks.api.BadException;
import com.formkiq.stacks.dynamodb.Preset;
import com.formkiq.stacks.dynamodb.PresetTag;

/**
 * {@link RequestHandler} for POST "/presets", DELETE "/presets/{presetId}.
 */
public class PresetsPostRequestHandler implements RequestHandler {

  /**
   * constructor.
   * 
   */
  public PresetsPostRequestHandler() {}

  @Override
  public boolean isReadonly(final String method) {
    return false;
  }

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String method = event.getHttpMethod();
    boolean isDelete = "delete".equalsIgnoreCase(method);

    String presetId = getPathParameter(event, "presetId");
    PresetWithTags item =
        !isDelete ? fromBodyToObject(logger, event, PresetWithTags.class) : new PresetWithTags();

    validate(isDelete, presetId, item);

    String siteId = getSiteId(event);
    ApiRequestHandlerResponse response = null;
    Preset preset = createPreset(event, item, presetId);

    if (isDelete) {

      awsservice.documentService().deletePreset(siteId, preset.getId());
      response = new ApiRequestHandlerResponse(SC_OK,
          new ApiMapResponse(Map.of("message", "Removed preset '" + preset.getId() + "'")));

    } else {

      List<PresetTag> tags = createTags(event, preset, item.getTags());

      awsservice.documentService().savePreset(siteId, preset.getId(), preset.getType(), preset,
          tags);

      ApiResponseStatus status = SC_CREATED;
      response = new ApiRequestHandlerResponse(status, new ApiMapResponse(
          Map.of("id", preset.getId(), "siteId", siteId != null ? siteId : DEFAULT_SITE_ID)));
    }

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
   * @param isDelete boolean
   * @param presetId {@link String}
   * @param item {@link PresetWithTags}
   * @throws BadException BadException
   */
  private void validate(final boolean isDelete, final String presetId, final PresetWithTags item)
      throws BadException {

    if (isDelete && presetId == null) {
      throw new BadException("missing 'presetId'");
    } else if (!isDelete) {
      if (item == null || item.getName() == null) {
        throw new BadException("invalid body");
      }
    }
  }
}
