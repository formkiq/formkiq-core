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
package com.formkiq.stacks.lambda.s3.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cognito Custom Message Handler.
 */
@Reflectable
public class CustomMessageHandler
    implements RequestHandler<CognitoCustomMessageEvent, CognitoCustomMessageEvent> {

  /** {@link Map} of Defaults. */
  private static final Map<String, Map<String, String>> DEFAULTS = new HashMap<>();
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;

  static {
    DEFAULTS.put("CustomMessage_SignUp", Map.of("Subject", "Your Verification Link", "Message",
        "Thank you for signing up. <a href=\"${link}\" target=\"_blank\">Click this link to verify</a>"));
    DEFAULTS.put("CustomMessage_ForgotPassword", Map.of("Subject", "Your Reset Password link",
        "Message",
        "You have requested a password reset. <a href=\"${link}\" target=\"_blank\">Click this link to Reset Password</a>"));
    DEFAULTS.put("CustomMessage_AdminCreateUser", Map.of("Subject", "Your Account has been Created",
        "Message",
        "Your account has been created. <a href=\"${link}\" target=\"_blank\">Click this link to finalize your account.</a>"));

    if (System.getenv().containsKey("AWS_REGION")) {
      serviceCache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
              new SnsAwsServiceRegistry(), new SsmAwsServiceRegistry())
          .build();

      initialize(serviceCache);
    }
  }

  private static String getClientId(final CognitoCustomMessageEvent event) {
    return event.callerContext() != null ? event.callerContext().clientId() : null;
  }

  /**
   * Initialize.
   *
   * @param awsServiceCache {@link AwsServiceCache}
   */
  static void initialize(final AwsServiceCache awsServiceCache) {
    awsServiceCache.register(S3Service.class, new S3ServiceExtension());
    awsServiceCache.register(SsmService.class, new SsmServiceExtension());
  }

  /**
   * constructor.
   */
  public CustomMessageHandler() {
    // empty
  }

  /**
   * constructor.
   *
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public CustomMessageHandler(final AwsServiceCache awsServiceCache) {
    this();
    initialize(awsServiceCache);
    serviceCache = awsServiceCache;
  }

  private String defaultFor(final String trigger, final String key) {
    return Optional.ofNullable(DEFAULTS.get(trigger)).map(m -> m.get(key)).orElse("");
  }

  private String generateLink(final CognitoCustomMessageEvent event,
      final CustomMessageConfig config, final String userStatus, final String email) {

    String link = null;
    String trigger = event.triggerSource();
    String cognitoHttpApiUrl = config.cognitoHttpApiUrl();
    String userName = event.userName();
    String region = event.region();
    String clientId = getClientId(event);
    String codeParameter = event.request() != null ? event.request().codeParameter() : null;
    String usernameParameter = event.request() != null ? event.request().usernameParameter() : null;

    if ("CustomMessage_SignUp".equals(trigger)) {

      link = cognitoHttpApiUrl + "/confirmSignUp?userStatus=" + userStatus + "&code="
          + codeParameter + "&username=" + userName + "&clientId=" + clientId + "&region=" + region
          + "&email=" + email;

    } else if ("CustomMessage_ForgotPassword".equals(trigger)) {

      link = config.consoleUrl() + "/change-password?userStatus=" + userStatus + "&code="
          + codeParameter + "&username=" + userName + "&clientId=" + clientId + "&region=" + region
          + "&email=" + email;

    } else if ("CustomMessage_AdminCreateUser".equals(trigger)) {

      // Node uses usernameParameter as the email query param (kept the same)
      link = cognitoHttpApiUrl + "/confirmRegistration?userStatus=" + userStatus + "&code="
          + codeParameter + "&username=" + userName + "&clientId=" + clientId + "&region=" + region
          + "&email=" + usernameParameter;
    }
    return link;
  }

  private String getMessageFromS3(final String domain, final String trigger, final String bucket) {
    String messageKey = "formkiq/cognito/" + domain + "/" + trigger + "/Message";
    String message = getS3Utf8OrEmpty(bucket, messageKey);
    if (message.isBlank()) {
      message = defaultFor(trigger, "Message");
    }
    return message;
  }

  private Map<String, String> getParameters(final String... names) {
    try {
      return serviceCache.getExtension(SsmService.class).getParameterValues(names);
    } catch (SdkException e) {
      throw new RuntimeException("SSM getParameters failed", e);
    }
  }

  private String getS3Utf8OrEmpty(final String bucket, final String key) {
    try {
      var s3 = serviceCache.getExtension(S3Service.class);
      byte[] bytes = s3.getContentAsBytes(bucket, key);
      return new String(bytes, StandardCharsets.UTF_8);

    } catch (SdkException e) {
      return "";
    }
  }

  private CustomMessageConfig getSsmParameters(final CognitoCustomMessageEvent event) {

    String domain = requireEnv("DOMAIN");
    String trigger = event.triggerSource();
    String urlParamName = "/formkiq/cognito/" + domain + "/CognitoHttpApiUrl";
    String subjectParamName = "/formkiq/cognito/" + domain + "/" + trigger + "/Subject";
    String consoleUrlParam =
        "/formkiq/" + serviceCache.environment("APP_ENVIRONMENT") + "/console/Url";

    Map<String, String> parameters = getParameters(urlParamName, subjectParamName, consoleUrlParam);
    String subject = parameters.getOrDefault(subjectParamName, defaultFor(trigger, "Subject"));
    String consoleUrl = parameters.get(consoleUrlParam);
    var config = new CustomMessageConfig(parameters.get(urlParamName), subject, consoleUrl);

    if (config.cognitoHttpApiUrl() == null) {
      throw new IllegalStateException("Missing required SSM parameter: " + urlParamName);
    }

    if (config.consoleUrl() == null) {
      throw new IllegalStateException("Missing required SSM parameter: " + consoleUrlParam);
    }

    return config;
  }

  @Override
  public CognitoCustomMessageEvent handleRequest(final CognitoCustomMessageEvent event,
      final Context context) {

    context.getLogger().log("triggerSource=" + event.triggerSource() + "\n");

    String domain = requireEnv("DOMAIN");
    String bucket = requireEnv("S3_BUCKET");

    String trigger = event.triggerSource();

    CustomMessageConfig config = getSsmParameters(event);

    String message = getMessageFromS3(domain, trigger, bucket);

    // Values used across triggers

    Map<String, String> attrs = event.request() != null && event.request().userAttributes() != null
        ? event.request().userAttributes()
        : Map.of();

    String userStatus = attrs.get("cognito:user_status");
    String email = attrs.get("email");

    String link = generateLink(event, config, userStatus, email);

    if (link == null) {
      // Unknown triggerSource: return unchanged event
      return event;
    }

    String rendered = processMessage(attrs, message, link);

    CognitoCustomMessageEvent updated = event.withEmail(config.subject(), rendered);

    context.getLogger().log("emailSubject=" + updated.response().emailSubject() + "\n");
    return updated;
  }

  private String processMessage(final Map<String, String> userAttributes, final String message,
      final String link) {

    String email = userAttributes.getOrDefault("email", "");
    String emailLocal = "";
    int at = email.indexOf("@");

    if (at > 0) {
      emailLocal = email.substring(0, at);
    }

    return message.replace("${link}", link).replace("${email}", email).replace("${emailLocal}",
        emailLocal);
  }

  private String requireEnv(final String name) {
    String v = serviceCache.environment(name);
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("Missing required env var: " + name);
    }
    return v;
  }
}
