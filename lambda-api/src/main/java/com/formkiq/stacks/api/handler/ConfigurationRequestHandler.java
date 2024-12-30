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
import static com.formkiq.stacks.dynamodb.config.ConfigService.CHATGPT_API_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_HMAC_SIGNATURE;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_INTEGRATION_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_RSA_PRIVATE_KEY;
import static com.formkiq.stacks.dynamodb.config.ConfigService.KEY_DOCUSIGN_USER_ID;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.NOTIFICATION_EMAIL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.ses.SesConnectionBuilder;
import com.formkiq.aws.ses.SesService;
import com.formkiq.aws.ses.SesServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
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

    String siteId = event.getPathParameters().get("siteId");
    ConfigService configService = awsservice.getExtension(ConfigService.class);

    Map<String, Object> obj = configService.get(siteId);

    Map<String, Object> map = new HashMap<>();
    map.put("chatGptApiKey", mask(obj.getOrDefault(CHATGPT_API_KEY, "").toString(), CHAT_GPT_MASK));
    map.put("maxContentLengthBytes", obj.getOrDefault(MAX_DOCUMENT_SIZE_BYTES, ""));
    map.put("maxDocuments", obj.getOrDefault(MAX_DOCUMENTS, ""));
    map.put("maxWebhooks", obj.getOrDefault(MAX_WEBHOOKS, ""));
    map.put("notificationEmail", obj.getOrDefault(NOTIFICATION_EMAIL, ""));

    setupGoogle(obj, map);
    setupDocusign(obj, map);
    setupOcr(obj, map);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  private void setupOcr(final Map<String, Object> obj, final Map<String, Object> map) {

    Double maxTransactions = (Double) obj.getOrDefault("maxTransactions", (double) -1);
    Double maxPagesPerTransaction =
        (Double) obj.getOrDefault("maxPagesPerTransaction", (double) -1);

    map.put("ocr", Map.of("maxPagesPerTransaction", maxPagesPerTransaction, "maxTransactions",
        maxTransactions));
  }

  private void setupGoogle(final Map<String, Object> obj, final Map<String, Object> map) {
    String workloadIdentityAudience =
        (String) obj.getOrDefault("googleWorkloadIdentityAudience", "");
    String workloadIdentityServiceAccount =
        (String) obj.getOrDefault("googleWorkloadIdentityServiceAccount", "");

    if (!isEmpty(workloadIdentityAudience) && !isEmpty(workloadIdentityServiceAccount)) {
      map.put("google", Map.of("workloadIdentityAudience", workloadIdentityAudience,
          "workloadIdentityServiceAccount", workloadIdentityServiceAccount));
    }
  }

  private void setupDocusign(final Map<String, Object> obj, final Map<String, Object> map) {
    String docusignUserId = (String) obj.getOrDefault(KEY_DOCUSIGN_USER_ID, "");
    String docusignIntegrationKey = (String) obj.getOrDefault(KEY_DOCUSIGN_INTEGRATION_KEY, "");
    String docusignRsaPrivateKey = (String) obj.getOrDefault(KEY_DOCUSIGN_RSA_PRIVATE_KEY, "");
    String docusignHmacSignature = (String) obj.getOrDefault(KEY_DOCUSIGN_HMAC_SIGNATURE, "");

    if (!isEmpty(docusignUserId) && !isEmpty(docusignIntegrationKey)
        && !isEmpty(docusignRsaPrivateKey)) {
      map.put("docusign",
          Map.of("userId", docusignUserId, "integrationKey", docusignIntegrationKey,
              "rsaPrivateKey", mask(docusignRsaPrivateKey, RSA_PRIVATE_KEY_MASK), "hmacSignature",
              mask(docusignHmacSignature, HMAC_SIG_KEY_MASK)));
    }
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
    int m = mask > s.length() ? s.length() / smallDiv : mask;
    return !isEmpty(s) ? s.subSequence(0, m) + "*******" + s.substring(s.length() - m) : s;
  }

  @Override
  public ApiRequestHandlerResponse patch(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = event.getPathParameter("siteId");
    SiteConfiguration config = fromBodyToObject(event, SiteConfiguration.class);

    Map<String, Object> map = new HashMap<>();
    put(map, CHATGPT_API_KEY, config.getChatGptApiKey());
    put(map, MAX_DOCUMENT_SIZE_BYTES, config.getMaxContentLengthBytes());
    put(map, MAX_DOCUMENTS, config.getMaxDocuments());
    put(map, MAX_WEBHOOKS, config.getMaxWebhooks());
    put(map, NOTIFICATION_EMAIL, config.getNotificationEmail());

    updateGoogle(config, map);

    updateOcr(config, map);

    updateDocusign(config, map);

    validate(awsservice, map);

    if (!map.isEmpty()) {
      ConfigService configService = awsservice.getExtension(ConfigService.class);
      configService.save(siteId, new DynamicObject(map));

      return new ApiRequestHandlerResponse(SC_OK,
          new ApiMapResponse(Map.of("message", "Config saved")));
    }

    throw new BadException("missing required body parameters");
  }

  private void updateDocusign(final SiteConfiguration config, final Map<String, Object> map) {

    SiteConfigurationDocusign docusign = config.getDocusign();
    if (docusign != null) {

      String docusignUserId = docusign.getUserId();
      if (!Strings.isEmpty(docusignUserId)) {
        map.put(KEY_DOCUSIGN_USER_ID, docusignUserId.trim());
      }

      String docusignIntegrationKey = docusign.getIntegrationKey();
      if (!Strings.isEmpty(docusignIntegrationKey)) {
        map.put(KEY_DOCUSIGN_INTEGRATION_KEY, docusignIntegrationKey.trim());
      }

      String docusignRsaPrivateKey = docusign.getRsaPrivateKey();
      if (!Strings.isEmpty(docusignRsaPrivateKey)) {
        map.put(KEY_DOCUSIGN_RSA_PRIVATE_KEY, docusignRsaPrivateKey.trim());
      }

      String docusignHmacSignature = docusign.getHmacSignature();
      if (!Strings.isEmpty(docusignHmacSignature)) {
        map.put(KEY_DOCUSIGN_HMAC_SIGNATURE, docusignHmacSignature.trim());
      }
    }
  }

  private void updateOcr(final SiteConfiguration config, final Map<String, Object> map) {
    SiteConfigurationOcr ocr = config.getOcr();

    if (ocr != null) {
      long maxTransactions = ocr.getMaxTransactions();
      long maxPagesPerTransaction = ocr.getMaxPagesPerTransaction();
      map.put("maxTransactions", maxTransactions != 0 ? maxTransactions : -1);
      map.put("maxPagesPerTransaction", maxPagesPerTransaction != 0 ? maxPagesPerTransaction : -1);
    }
  }

  private void updateGoogle(final SiteConfiguration config, final Map<String, Object> map) {
    SiteConfigurationGoogle google = config.getGoogle();
    if (google != null) {

      String workloadIdentityAudience = google.getWorkloadIdentityAudience();
      String workloadIdentityServiceAccount = google.getWorkloadIdentityServiceAccount();

      if (!Strings.isEmpty(workloadIdentityAudience)) {
        map.put("googleWorkloadIdentityAudience", workloadIdentityAudience);
      }

      if (!Strings.isEmpty(workloadIdentityServiceAccount)) {
        map.put("googleWorkloadIdentityServiceAccount", workloadIdentityServiceAccount);
      }
    }
  }

  private void put(final Map<String, Object> map, final String mapKey, final String value) {
    if (value != null) {
      map.put(mapKey, value);
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

    validateGoogle(map, errors);
    validateDocusign(map, errors);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private void validateGoogle(final Map<String, Object> map,
      final Collection<ValidationError> errors) {
    String googleWorkloadIdentityAudience =
        (String) map.getOrDefault("googleWorkloadIdentityAudience", null);
    String googleWorkloadIdentityServiceAccount =
        (String) map.getOrDefault("googleWorkloadIdentityServiceAccount", null);

    if (!Strings.isEmptyOrHasValues(googleWorkloadIdentityAudience,
        googleWorkloadIdentityServiceAccount)) {
      errors.add(new ValidationErrorImpl().key("google")
          .error("all 'googleWorkloadIdentityAudience', 'googleWorkloadIdentityServiceAccount' "
              + "are required for google setup"));
    }
  }

  private void validateDocusign(final Map<String, Object> map,
      final Collection<ValidationError> errors) {

    String docusignUserId = (String) map.getOrDefault(KEY_DOCUSIGN_USER_ID, null);
    String docusignIntegrationKey = (String) map.getOrDefault(KEY_DOCUSIGN_INTEGRATION_KEY, null);
    String docusignRsaPrivateKey = (String) map.getOrDefault(KEY_DOCUSIGN_RSA_PRIVATE_KEY, null);

    if (!Strings.isEmptyOrHasValues(docusignUserId, docusignIntegrationKey,
        docusignRsaPrivateKey)) {
      errors.add(new ValidationErrorImpl().key("docusign")
          .error("all 'docusignUserId', 'docusignIntegrationKey', 'docusignRsaPrivateKey' "
              + "are required for docusign setup"));
    } else if (docusignRsaPrivateKey != null && !isValidRsaPrivateKey(docusignRsaPrivateKey)) {
      errors.add(
          new ValidationErrorImpl().key("docusignRsaPrivateKey").error("invalid RSA Private Key"));
    }
  }

  private boolean isValidRsaPrivateKey(final String privateKeyPem) {
    return privateKeyPem.contains("-----BEGIN RSA PRIVATE KEY-----")
        && privateKeyPem.contains("-----END RSA PRIVATE KEY-----");
  }
}
