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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.strings.Strings.getNotNullOrDefault;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.ses.SesConnectionBuilder;
import com.formkiq.aws.ses.SesService;
import com.formkiq.aws.ses.SesServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationDocument;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationDocumentContentTypes;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationDocusign;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationGoogle;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationOcr;
import com.formkiq.validation.ValidationBuilder;
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
    String chatApiKey = mask(obj.chatGptApiKey(), CHAT_GPT_MASK);

    SiteConfigurationDocusign docusign = obj.docusign();
    if (docusign != null) {
      docusign = new SiteConfigurationDocusign(docusign.userId(), docusign.integrationKey(),
          mask(docusign.rsaPrivateKey(), RSA_PRIVATE_KEY_MASK),
          mask(docusign.hmacSignature(), HMAC_SIG_KEY_MASK));
    }

    obj = new SiteConfiguration(null, getNotNullOrDefault(chatApiKey, ""),
        getNotNullOrDefault(obj.maxContentLengthBytes(), ""),
        getNotNullOrDefault(obj.maxDocuments(), ""), getNotNullOrDefault(obj.maxWebhooks(), ""),
        getNotNullOrDefault(obj.notificationEmail(), ""),
        Objects.getNotNullOrDefault(obj.document(), new SiteConfigurationDocument(null)),
        Objects.getNotNullOrDefault(obj.ocr(), new SiteConfigurationOcr(-1, -1)),
        Objects.getNotNullOrDefault(obj.google(), new SiteConfigurationGoogle(null, null)), Objects
            .getNotNullOrDefault(docusign, new SiteConfigurationDocusign(null, null, null, null)),
        null, null);

    Gson gson = GsonUtil.getInstance();
    String json = gson.toJson(obj);
    Map<String, Object> map = gson.fromJson(json, Map.class);

    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  private List<Object> getConfigValues(final SiteConfiguration config) {
    return Arrays.asList(config.chatGptApiKey(), config.maxContentLengthBytes(),
        config.maxDocuments(), config.maxWebhooks(), config.notificationEmail(), config.document(),
        config.ocr(), config.google(), config.docusign(), config.documentTimeToLive(),
        config.webhookTimeToLive());
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

  private boolean isValidRsaPrivateKey(final String privateKeyPem) {
    return privateKeyPem.contains("-----BEGIN RSA PRIVATE KEY-----")
        && privateKeyPem.contains("-----END RSA PRIVATE KEY-----");
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
    config = SiteConfiguration.builder().configuration(config).build(siteId);
    validate(awsservice, config);

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    if (configService.save(siteId, config)) {
      return ApiRequestHandlerResponse.builder().ok().body("message", "Config saved").build();
    }

    throw new BadException("missing required body parameters");
  }

  private void validate(final AwsServiceCache awsservice, final SiteConfiguration config)
      throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired(null, getConfigValues(config), "missing required body parameters");

    String notificationEmail = config.notificationEmail();
    if (notificationEmail != null) {

      initSes(awsservice);

      SesService sesService = awsservice.getExtension(SesService.class);
      Optional<String> o = sesService.getEmailAddresses().identities().stream()
          .filter(i -> i.equals(notificationEmail)).findFirst();

      if (o.isEmpty()) {
        vb.addError("notificationEmail", "'notificationEmail' is not setup in AWS SES");
      }
    }

    validateGoogle(config, vb);
    validateDocument(config, vb);
    validateDocusign(config, vb);

    vb.check();
  }

  private void validateDocument(final SiteConfiguration config, final ValidationBuilder vb) {

    SiteConfigurationDocument document = config.document();

    if (document != null) {

      SiteConfigurationDocumentContentTypes contentTypes = document.contentTypes();
      if (contentTypes != null) {
        boolean hasAllowlist = !notNull(contentTypes.allowlist()).isEmpty();
        boolean hasDenylist = !notNull(contentTypes.denylist()).isEmpty();

        if (hasAllowlist == hasDenylist) {
          vb.addError("document.contentTypes", "Only set either 'allowlist' or 'denylist'");
        }
      }
    }
  }

  private void validateDocusign(final SiteConfiguration config, final ValidationBuilder vb) {

    SiteConfigurationDocusign docusign = config.docusign();

    if (docusign != null) {
      String docusignUserId = docusign.userId();
      String docusignIntegrationKey = docusign.integrationKey();
      String docusignRsaPrivateKey = docusign.rsaPrivateKey();

      if (!Strings.isEmpty(docusignUserId, docusignIntegrationKey, docusignRsaPrivateKey)) {
        vb.addError("docusign",
            "all 'docusignUserId', 'docusignIntegrationKey', 'docusignRsaPrivateKey' "
                + "are required for docusign setup");
      } else if (docusignRsaPrivateKey != null && !isValidRsaPrivateKey(docusignRsaPrivateKey)) {
        vb.addError("docusignRsaPrivateKey", "invalid RSA Private Key");
      }
    }
  }

  private void validateGoogle(final SiteConfiguration config, final ValidationBuilder vb) {

    SiteConfigurationGoogle google = config.google();

    if (google != null) {
      String googleWorkloadIdentityAudience = google.workloadIdentityAudience();
      String googleWorkloadIdentityServiceAccount = google.workloadIdentityServiceAccount();

      if (!Strings.isEmpty(googleWorkloadIdentityAudience, googleWorkloadIdentityServiceAccount)) {
        vb.addError("google",
            "all 'googleWorkloadIdentityAudience', 'googleWorkloadIdentityServiceAccount' "
                + "are required for google setup");
      }
    }
  }
}
