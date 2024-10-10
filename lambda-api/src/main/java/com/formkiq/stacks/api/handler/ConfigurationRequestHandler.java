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
import static com.formkiq.stacks.dynamodb.ConfigService.KEY_DOCUSIGN_INTEGRATION_KEY;
import static com.formkiq.stacks.dynamodb.ConfigService.KEY_DOCUSIGN_RSA_PRIVATE_KEY;
import static com.formkiq.stacks.dynamodb.ConfigService.KEY_DOCUSIGN_USER_ID;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.ConfigService.NOTIFICATION_EMAIL;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
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
import com.formkiq.stacks.dynamodb.ConfigService;
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

  /**
   * constructor.
   */
  public ConfigurationRequestHandler() {}

  @Override
  public void beforePatch(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(event, authorization);
  }

  private void checkPermissions(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) throws UnauthorizedException {
    checkPermission(event, authorization, ApiPermission.ADMIN);
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = event.getPathParameters().get("siteId");
    ConfigService configService = awsservice.getExtension(ConfigService.class);

    DynamicObject obj = configService.get(siteId);

    Map<String, Object> map = new HashMap<>();
    map.put("chatGptApiKey", mask(obj.getOrDefault(CHATGPT_API_KEY, "").toString(), CHAT_GPT_MASK));
    map.put("maxContentLengthBytes", obj.getOrDefault(MAX_DOCUMENT_SIZE_BYTES, ""));
    map.put("maxDocuments", obj.getOrDefault(MAX_DOCUMENTS, ""));
    map.put("maxWebhooks", obj.getOrDefault(MAX_WEBHOOKS, ""));
    map.put("notificationEmail", obj.getOrDefault(NOTIFICATION_EMAIL, ""));

    setupGoogle(obj, map);
    setupDocusign(obj, map);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  private void setupGoogle(final DynamicObject obj, final Map<String, Object> map) {
    String workloadIdentityAudience =
        (String) obj.getOrDefault("googleWorkloadIdentityAudience", "");
    String workloadIdentityServiceAccount =
        (String) obj.getOrDefault("googleWorkloadIdentityServiceAccount", "");

    if (!isEmpty(workloadIdentityAudience) && !isEmpty(workloadIdentityServiceAccount)) {
      map.put("google", Map.of("workloadIdentityAudience", workloadIdentityAudience,
          "workloadIdentityServiceAccount", workloadIdentityServiceAccount));
    }
  }

  private void setupDocusign(final DynamicObject obj, final Map<String, Object> map) {
    String docusignUserId = (String) obj.getOrDefault(KEY_DOCUSIGN_USER_ID, "");
    String docusignIntegrationKey = (String) obj.getOrDefault(KEY_DOCUSIGN_INTEGRATION_KEY, "");
    String docusignRsaPrivateKey = (String) obj.getOrDefault(KEY_DOCUSIGN_RSA_PRIVATE_KEY, "");

    if (!isEmpty(docusignUserId) && !isEmpty(docusignIntegrationKey)
        && !isEmpty(docusignRsaPrivateKey)) {
      map.put("docusign", Map.of("userId", docusignUserId, "integrationKey", docusignIntegrationKey,
          "rsaPrivateKey", mask(docusignRsaPrivateKey, RSA_PRIVATE_KEY_MASK)));
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
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = event.getPathParameter("siteId");
    Map<String, Object> body = fromBodyToObject(event, Map.class);

    Map<String, Object> map = new HashMap<>();
    put(map, body, CHATGPT_API_KEY, "chatGptApiKey");
    put(map, body, MAX_DOCUMENT_SIZE_BYTES, "maxContentLengthBytes");
    put(map, body, MAX_DOCUMENTS, "maxDocuments");
    put(map, body, MAX_WEBHOOKS, "maxWebhooks");
    put(map, body, NOTIFICATION_EMAIL, "notificationEmail");

    if (body.containsKey("google")) {

      Map<String, String> google = (Map<String, String>) body.get("google");
      String workloadIdentityAudience = google.getOrDefault("workloadIdentityAudience", "");
      String workloadIdentityServiceAccount =
          google.getOrDefault("workloadIdentityServiceAccount", "");

      map.put("googleWorkloadIdentityAudience", workloadIdentityAudience);
      map.put("googleWorkloadIdentityServiceAccount", workloadIdentityServiceAccount);
    }

    if (body.containsKey("docusign")) {

      Map<String, String> google = (Map<String, String>) body.get("docusign");
      String docusignUserId = google.getOrDefault("userId", "").trim();
      String docusignIntegrationKey = google.getOrDefault("integrationKey", "").trim();
      String docusignRsaPrivateKey = google.getOrDefault("rsaPrivateKey", "").trim();

      map.put(KEY_DOCUSIGN_USER_ID, docusignUserId);
      map.put(KEY_DOCUSIGN_INTEGRATION_KEY, docusignIntegrationKey);
      map.put(KEY_DOCUSIGN_RSA_PRIVATE_KEY, docusignRsaPrivateKey);
    }

    validate(awsservice, map);

    if (!map.isEmpty()) {
      ConfigService configService = awsservice.getExtension(ConfigService.class);
      configService.save(siteId, new DynamicObject(map));

      return new ApiRequestHandlerResponse(SC_OK,
          new ApiMapResponse(Map.of("message", "Config saved")));
    }

    throw new BadException("missing required body parameters");
  }

  private void put(final Map<String, Object> map, final Map<String, Object> body,
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
    try {
      // Remove the PEM header and footer
      String privateKeyPemStripped =
          privateKeyPem.replace("\\u003d", "=").replace("-----BEGIN RSA PRIVATE KEY-----", "")
              .replace("-----END RSA PRIVATE KEY-----", "").replace("\\n", "")
              .replaceAll("\\s+", "").trim();

      byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyPemStripped);

      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

      return privateKey != null;

    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      return false;
    }
  }
}
