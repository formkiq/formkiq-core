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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/** {@link ApiGatewayRequestHandler} for "/version". */
public class VersionRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   */
  public VersionRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String version = getVersion(awsservice);
    List<String> modules = awsservice.environment().entrySet().stream()
        .filter(e -> e.getKey().startsWith("MODULE_") && "true".equals(e.getValue()))
        .map(e -> e.getKey().replaceAll("MODULE_", "")).collect(Collectors.toList());

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(Map.of("version", version,
        "type", awsservice.environment("FORMKIQ_TYPE"), "modules", modules)));
  }

  @Override
  public String getRequestUrl() {
    return "/version";
  }

  private String getVersion(final AwsServiceCache awsservice) {
    SsmService ssmService = awsservice.getExtension(SsmService.class);
    String key = "/formkiq/" + awsservice.environment("APP_ENVIRONMENT") + "/version";

    String version = awsservice.environment("VERSION");
    if (version == null) {
      version = ssmService.getParameterValue(key);
    }

    return version;
  }

  @Override
  public boolean isSiteIdRequired() {
    return false;
  }
}
