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
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.dynamodb.Preset;

/**
 * {@link ApiGatewayRequestHandler} for "/presets/{presetId}.
 */
public class PresetsIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   * 
   */
  public PresetsIdRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String presetId = getPathParameter(event, "presetId");
    PresetWithTags item = new PresetWithTags();

    validate(presetId, item);

    String siteId = getSiteId(event);
    Preset preset = createPreset(event, item, presetId);

    awsservice.documentService().deletePreset(siteId, preset.getId());
    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "Removed preset '" + preset.getId() + "'")));
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

    if (presetId == null) {
      throw new BadException("missing 'presetId'");
    }
  }

  @Override
  public String getRequestUrl() {
    return "/presets/{presetId}";
  }
}
