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

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.GsonUtil;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.ses.SesConnectionBuilder;
import com.formkiq.aws.ses.SesService;
import com.formkiq.aws.ses.SesServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationDocusign;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationGoogle;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/** {@link ApiGatewayRequestHandler} for "/sites/{siteId}/configuration". */
public class ConfigurationRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * Mask Value, must be even number.
   */
  private static final int CHAT_GPT_MASK = 4;
  /**
   * Mask Value, must be even number.
   */
  private static final int RSA_PRIVATE_KEY_MASK = 40;
  /** HMAC Signature Mask. */
  private static final int HMAC_SIG_KEY_MASK = 4;

  /**
   * constructor.
   */
  public ConfigurationRequestHandler() {}

  @Override
  public void beforePatch(final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {
    checkPermissions(event, authorization);
  }

  private void checkPermissions(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) throws UnauthorizedException {
    checkPermission(event, authorization, ApiPermission.ADMIN);
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = getPathParameterSiteId(event);
    ConfigService configService = awsservice.getExtension(ConfigService.class);

    SiteConfiguration obj = configService.get(siteId);
    obj.setChatGptApiKey(mask(obj.getChatGptApiKey(), CHAT_GPT_MASK));

    // hide from API
    obj.setDocumentTimeToLive(null);
    obj.setWebhookTimeToLive(null);

    SiteConfigurationDocusign docusign = obj.getDocusign();
    if (docusign != null) {
      docusign.setHmacSignature(mask(docusign.getHmacSignature(), HMAC_SIG_KEY_MASK));
      docusign.setRsaPrivateKey(mask(docusign.getRsaPrivateKey(), RSA_PRIVATE_KEY_MASK));
    }

    Gson gson = GsonUtil.getInstance();
    String json = gson.toJson(obj);
    Map<String, Object> map = gson.fromJson(json, Map.class);

    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/configuration";
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
   * @param mask int
   * @return {@link String}
   */
  private String mask(final String s, final int mask) {
    final int smallDiv = 3;
    int m = s != null && mask > s.length() ? s.length() / smallDiv : mask;
    return !isEmpty(s) ? s.subSequence(0, m) + "*******" + s.substring(s.length() - m) : s;
  }

  @Override
  public ApiRequestHandlerResponse patch(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = getPathParameterSiteId(event);
    SiteConfiguration config = JsonToObject.fromJson(awsservice, event, SiteConfiguration.class);
    validate(awsservice, config);

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    if (configService.save(siteId, config)) {

      return ApiRequestHandlerResponse.builder().ok().body("message", "Config saved").build();
    }

    throw new BadException("missing required body parameters");
  }

  private void validate(final AwsServiceCache awsservice, final SiteConfiguration config)
      throws ValidationException {

    Collection<ValidationError> errors = new ArrayList<>();

    String notificationEmail = config.getNotificationEmail();
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

    validateGoogle(config, errors);
    validateDocusign(config, errors);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private void validateGoogle(final SiteConfiguration config,
      final Collection<ValidationError> errors) {

    SiteConfigurationGoogle google = config.getGoogle();

    if (google != null) {
      String googleWorkloadIdentityAudience = google.getWorkloadIdentityAudience();
      String googleWorkloadIdentityServiceAccount = google.getWorkloadIdentityServiceAccount();

      if (!Strings.isEmpty(googleWorkloadIdentityAudience, googleWorkloadIdentityServiceAccount)) {
        errors.add(new ValidationErrorImpl().key("google")
            .error("all 'googleWorkloadIdentityAudience', 'googleWorkloadIdentityServiceAccount' "
                + "are required for google setup"));
      }
    }
  }

  private void validateDocusign(final SiteConfiguration config,
      final Collection<ValidationError> errors) {

    SiteConfigurationDocusign docusign = config.getDocusign();

    if (docusign != null) {
      String docusignUserId = docusign.getUserId();
      String docusignIntegrationKey = docusign.getIntegrationKey();
      String docusignRsaPrivateKey = docusign.getRsaPrivateKey();

      if (!Strings.isEmpty(docusignUserId, docusignIntegrationKey, docusignRsaPrivateKey)) {
        errors.add(new ValidationErrorImpl().key("docusign")
            .error("all 'docusignUserId', 'docusignIntegrationKey', 'docusignRsaPrivateKey' "
                + "are required for docusign setup"));
      } else if (docusignRsaPrivateKey != null && !isValidRsaPrivateKey(docusignRsaPrivateKey)) {
        errors.add(new ValidationErrorImpl().key("docusignRsaPrivateKey")
            .error("invalid RSA Private Key"));
      }
    }
  }

  private boolean isValidRsaPrivateKey(final String privateKeyPem) {
    return privateKeyPem.contains("-----BEGIN RSA PRIVATE KEY-----")
        && privateKeyPem.contains("-----END RSA PRIVATE KEY-----");
  }
}
