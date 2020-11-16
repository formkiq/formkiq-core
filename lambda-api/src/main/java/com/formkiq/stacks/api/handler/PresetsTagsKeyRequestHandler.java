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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Optional;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMessageResponse;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.ApiResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.lambda.apigateway.exception.NotFoundException;
import com.formkiq.stacks.dynamodb.PresetTag;

/**
 * {@link ApiGatewayRequestHandler} for /presets/{preset}/tags/{tagKey}.
 */
public class PresetsTagsKeyRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   * 
   */
  public PresetsTagsKeyRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
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

  @Override
  public String getRequestUrl() {
    return "/presets/{preset}/tags/{tagKey}";
  }
}
