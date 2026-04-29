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
package com.formkiq.stacks.api.handler.sites;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ReservedSiteId;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationWebUi;
import com.google.gson.Gson;

/** {@link ApiGatewayRequestHandler} for "/system/configuration". */
public class SystemConfigurationRequestHandler implements ApiGatewayRequestHandler {



  @Override
  public void beforePatch(final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {
    checkAdminPermission(authorization);
  }

  private void checkAdminPermission(final ApiAuthorization authorization)
      throws UnauthorizedException {
    if (!authorization.getAllPermissions().contains(ApiPermission.ADMIN)) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get(getSiteId());

    return ApiRequestHandlerResponse.builder().ok().body(toResponse(config.webui())).build();
  }

  @Override
  public String getRequestUrl() {
    return "/system/configuration";
  }

  private String getSiteId() {
    return ReservedSiteId.GLOBAL.getSiteId();
  }

  @Override
  public ApiRequestHandlerResponse patch(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    SiteConfiguration config = JsonToObject.fromJson(awsservice, event, SiteConfiguration.class);

    config = SiteConfiguration.builder().webui(config.webui()).build(getSiteId());

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    if (configService.save(getSiteId(), config)) {
      updateConsoleConfig(awsservice, config.webui());
      return ApiRequestHandlerResponse.builder().ok().body("message", "Config saved").build();
    }

    throw new BadException("missing required body parameters");
  }

  private Map<String, Object> toResponse(final SiteConfigurationWebUi webui) {
    Boolean ssoLoginRedirectEnabled = webui != null ? webui.ssoAutomaticSignIn() : Boolean.FALSE;
    return Map.of("webui", Map.of("ssoAutomaticSignIn", ssoLoginRedirectEnabled));
  }

  private void updateConsoleConfig(final AwsServiceCache awsservice,
      final SiteConfigurationWebUi webui) {

    String appEnvironment = awsservice.environment("APP_ENVIRONMENT");
    SsmService ssmService = awsservice.getExtension(SsmService.class);
    String consoleBucket =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/s3/Console");
    String consoleVersion =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/console/version");

    String key = consoleVersion + "/assets/config.json";
    S3Service s3Service = awsservice.getExtension(S3Service.class);
    Gson gson = GsonUtil.getInstance();
    Map<String, Object> consoleConfig =
        gson.fromJson(s3Service.getContentAsString(consoleBucket, key, null), Map.class);

    boolean ssoLoginRedirectEnabled = webui != null ? webui.ssoAutomaticSignIn() : Boolean.FALSE;
    consoleConfig.put("ssoAutomaticSignIn", ssoLoginRedirectEnabled);
    s3Service.putObject(consoleBucket, key,
        gson.toJson(consoleConfig).getBytes(StandardCharsets.UTF_8), "application/json");
  }
}
