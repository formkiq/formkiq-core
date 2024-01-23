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

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.ConfigService.CHATGPT_API_KEY;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.ConfigService.NOTIFICATION_EMAIL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.ses.SesConnectionBuilder;
import com.formkiq.aws.ses.SesService;
import com.formkiq.aws.ses.SesServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/** {@link ApiGatewayRequestHandler} for "/configuration". */
public class ConfigurationRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Mask Value, must be even number. */
  private static final int MASK = 4;

  /**
   * constructor.
   *
   */
  public ConfigurationRequestHandler() {}

  @Override
  public void beforePatch(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(authorization);
  }

  private void checkPermissions(final ApiAuthorization authorization) throws UnauthorizedException {
    if (!authorization.getPermissions().contains(ApiPermission.ADMIN)) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    ConfigService configService = awsservice.getExtension(ConfigService.class);

    DynamicObject obj = configService.get(siteId);

    Map<String, Object> map = new HashMap<>();
    map.put("chatGptApiKey", mask(obj.getOrDefault(CHATGPT_API_KEY, "").toString()));
    map.put("maxContentLengthBytes", obj.getOrDefault(MAX_DOCUMENT_SIZE_BYTES, ""));
    map.put("maxDocuments", obj.getOrDefault(MAX_DOCUMENTS, ""));
    map.put("maxWebhooks", obj.getOrDefault(MAX_WEBHOOKS, ""));
    map.put("notificationEmail", obj.getOrDefault(NOTIFICATION_EMAIL, ""));

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/configuration";
  }

  private void initSes(final AwsServiceCache awsservice) {

    if (!awsservice.containsExtension(SesConnectionBuilder.class)) {

      AwsCredentials cred = awsservice.getExtension(AwsCredentials.class);
      AwsCredentialsProvider credentials = StaticCredentialsProvider.create(cred);
      new SesAwsServiceRegistry().initService(awsservice, Map.of(), credentials);
    }

    if (!awsservice.containsExtension(SesService.class)) {
      awsservice.register(SesService.class, new SesServiceExtension());
    }
  }

  /**
   * Mask {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  private String mask(final String s) {
    return !isEmpty(s) ? s.subSequence(0, MASK) + "*******" + s.substring(s.length() - MASK) : s;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    Map<String, String> body = fromBodyToObject(event, Map.class);

    Map<String, Object> map = new HashMap<>();
    put(map, body, CHATGPT_API_KEY, "chatGptApiKey");
    put(map, body, MAX_DOCUMENT_SIZE_BYTES, "maxContentLengthBytes");
    put(map, body, MAX_DOCUMENTS, "maxDocuments");
    put(map, body, MAX_WEBHOOKS, "maxWebhooks");
    put(map, body, NOTIFICATION_EMAIL, "notificationEmail");

    validate(awsservice, map);

    if (!map.isEmpty()) {
      ConfigService configService = awsservice.getExtension(ConfigService.class);
      configService.save(siteId, new DynamicObject(map));

      return new ApiRequestHandlerResponse(SC_OK,
          new ApiMapResponse(Map.of("message", "Config saved")));
    }

    throw new BadException("missing required body parameters");
  }

  private void put(final Map<String, Object> map, final Map<String, String> body,
      final String mapKey, final String bodyKey) {
    if (body.containsKey(bodyKey)) {
      map.put(mapKey, body.get(bodyKey));
    }
  }

  private void validate(final AwsServiceCache awsservice, final Map<String, Object> map)
      throws ValidationException {

    Collection<ValidationError> errors = new ArrayList<>();

    Object notificationEmail = map.getOrDefault(NOTIFICATION_EMAIL, null);
    if (notificationEmail != null) {

      initSes(awsservice);

      SesService sesService = awsservice.getExtension(SesService.class);
      Optional<String> o = sesService.getEmailAddresses().identities().stream()
          .filter(i -> i.equals(notificationEmail)).findFirst();

      if (o.isEmpty()) {
        errors.add(new ValidationErrorImpl().key("notificationEmail")
            .error("'notificationEmail' is not setup in AWS SES"));
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
