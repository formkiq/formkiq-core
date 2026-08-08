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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_ACCEPTED;
import static com.formkiq.strings.Strings.isEmpty;
import static com.formkiq.strings.Strings.trim;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.notification.NotificationTestEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationNotification;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import java.util.regex.Pattern;

/** Handler for {@code POST /sites/{siteId}/configuration/notification/test}. */
public class ConfigurationNotificationTestRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Basic email address format accepted by the test endpoint. */
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  @Override
  public void beforePost(final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {
    checkPermissions(event, authorization);
  }

  private void checkPermissions(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) throws UnauthorizedException {
    String siteId = getPathParameterSiteId(event);
    if (!authorization.getPermissions(siteId).contains(ApiPermission.ADMIN)) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  private String getProvider(final SiteConfiguration configuration) {
    SiteConfigurationNotification notification = configuration.notification();
    if (notification != null && notification.provider() != null) {
      return notification.provider().name();
    }
    return !isEmpty(trim(configuration.notificationEmail())) ? "SES" : null;
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/configuration/notification/test";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {
    String siteId = getPathParameterSiteId(event);
    AddNotificationTestRequest request =
        JsonToObject.fromJson(awsservice, event, AddNotificationTestRequest.class);

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    SiteConfiguration configuration = configService.get(siteId);
    String provider = getProvider(configuration);
    validate(request, provider);

    EventService eventService = awsservice.getExtension(EventService.class);
    eventService.publish(awsservice.getLogger(),
        new NotificationTestEvent(siteId, trim(request.to()), authorization.getUsername()));

    return ApiRequestHandlerResponse.builder().status(SC_ACCEPTED)
        .body("message", "Test notification queued").build();
  }

  private void validate(final AddNotificationTestRequest request, final String provider)
      throws ValidationException {
    ValidationBuilder validation = new ValidationBuilder();
    if (request == null || isEmpty(trim(request.to()))) {
      validation.addError("to", "'to' is required");
    } else if (!EMAIL_PATTERN.matcher(trim(request.to())).matches()) {
      validation.addError("to", "'to' must be a valid email address");
    }
    if (provider == null) {
      validation.addError("notification", "Site notification configuration is not set");
    }
    validation.check();
  }
}
