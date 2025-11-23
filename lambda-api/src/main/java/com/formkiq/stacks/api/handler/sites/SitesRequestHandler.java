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

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationOcr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;

/** {@link ApiGatewayRequestHandler} for "/sites". */
public class SitesRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link Random}. */
  private final Random rnd = new Random();

  /**
   * constructor.
   *
   */
  public SitesRequestHandler() {}

  private void addConfig(final AwsServiceCache awsservice, final String siteId,
      final DynamicObject obj) {

    ConfigService configService = awsservice.getExtension(ConfigService.class);

    SiteConfiguration siteConfig = configService.get(siteId);

    if (siteConfig != null) {

      Map<String, Object> config = new HashMap<>();
      obj.put("config", config);

      config.put("maxContentLengthBytes", siteConfig.maxContentLengthBytes());
      config.put("maxDocuments", siteConfig.maxDocuments());
      config.put("maxWebhooks", siteConfig.maxWebhooks());

      SiteConfigurationOcr ocr = siteConfig.ocr();
      if (ocr != null) {
        Map<String, Object> o = new HashMap<>();
        o.put("maxTransactions", ocr.maxTransactions());
        o.put("maxPagesPerTransaction", ocr.maxPagesPerTransaction());
        config.put("ocr", o);
      }

      Map<String, Long> increments = configService.getIncrements(siteId);
      if (increments != null) {
        Map<String, Object> usage = new HashMap<>();
        obj.put("usage", usage);

        usage.put("documentCount", increments.get(ConfigService.DOCUMENT_COUNT));
        usage.put("ocrTransactionCount", increments.get(DocumentOcrService.CONFIG_OCR_COUNT));
      }
    }
  }

  /**
   * Generate Upload Email address.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param mailDomain {@link String}
   * @return {@link String}
   */
  private String generateUploadEmail(final AwsServiceCache awsservice, final String mailDomain) {
    final int emaillength = 8;
    String email = getSaltString(emaillength) + "@" + mailDomain;

    while (isEmailExists(awsservice, email)) {
      email = getSaltString(emaillength) + "@" + mailDomain;
    }

    return email;
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    List<DynamicObject> sites = authorization.getSiteIds().stream().map(siteId -> {

      DynamicObject obj = new DynamicObject(new HashMap<>());
      addConfig(awsservice, siteId, obj);

      obj.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);

      boolean write = authorization.getPermissions(siteId).contains(ApiPermission.WRITE);
      obj.put("permission", write ? "READ_WRITE" : "READ_ONLY");

      List<String> permissions =
          authorization.getPermissions(siteId).stream().map(Enum::name).sorted().toList();
      obj.put("permissions", permissions);

      return obj;
    }).collect(Collectors.toList());

    updateUploadEmail(awsservice, authorization, sites);

    String userId = authorization.getUsername();

    return ApiRequestHandlerResponse.builder().ok()
        .body(Map.of("username", userId, "roles", authorization.getRoles(), "sites", sites))
        .build();
  }

  /**
   * Get Mail Domain.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @return {@link String}
   */
  private String getMailDomain(final AwsServiceCache awsservice) {
    String key = "/formkiq/" + awsservice.environment("APP_ENVIRONMENT") + "/maildomain";
    return getSsmService(awsservice).getParameterValue(key);
  }

  @Override
  public String getRequestUrl() {
    return "/sites";
  }

  /**
   * Generate random {@link String}.
   * 
   * @param length int
   * @return {@link String}
   */
  private String getSaltString(final int length) {

    String saltchars = "abcdefghijklmnopqrstuvwxyz1234567890";
    StringBuilder salt = new StringBuilder();
    while (salt.length() < length) {
      int index = (int) (this.rnd.nextFloat() * saltchars.length());
      salt.append(saltchars.charAt(index));
    }
    return salt.toString();
  }

  /**
   * Get {@link SsmService}.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @return {@link SsmService}
   */
  private SsmService getSsmService(final AwsServiceCache awsservice) {
    return awsservice.getExtension(SsmService.class);
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsServiceCache, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    return !authorization.getSiteIds().isEmpty() ? Optional.of(Boolean.TRUE) : Optional.empty();
  }

  /**
   * Does Email address already exist.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param email {@link String}
   * @return boolean
   */
  private boolean isEmailExists(final AwsServiceCache awsservice, final String email) {
    String[] strs = email.split("@");
    String key = String.format("/formkiq/ses/%s/%s", strs[1], strs[0]);
    return getSsmService(awsservice).getParameterValue(key) != null;
  }

  /**
   * Update Upload Email.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param authorization {@link ApiAuthorization}
   * @param sites {@link List} {@link DynamicObject}
   */
  private void updateUploadEmail(final AwsServiceCache awsservice,
      final ApiAuthorization authorization, final List<DynamicObject> sites) {

    SsmService ssmService = getSsmService(awsservice);
    String mailDomain = ssmService != null ? getMailDomain(awsservice) : null;

    if (mailDomain != null) {

      List<String> writeSiteIds = authorization.getSiteIds().stream()
          .filter(s -> authorization.getPermissions(s).contains(ApiPermission.WRITE)).toList();

      sites.forEach(site -> {

        String siteId = site.getString("siteId");
        if (writeSiteIds.contains(siteId)) {
          String key = String.format("/formkiq/%s/siteid/%s/email",
              awsservice.environment("APP_ENVIRONMENT"), siteId);
          site.put("uploadEmail", ssmService.getParameterValue(key));
        }
      });

      sites.forEach(site -> {

        String siteId = site.getString("siteId");
        String uploadEmail = site.getString("uploadEmail");

        if (uploadEmail == null && writeSiteIds.contains(siteId)) {

          String email = generateUploadEmail(awsservice, mailDomain);
          site.put("uploadEmail", email);

          String key = String.format("/formkiq/%s/siteid/%s/email",
              awsservice.environment("APP_ENVIRONMENT"), siteId);
          ssmService.putParameter(key, email);

          String[] strs = email.split("@");
          key = String.format("/formkiq/ses/%s/%s", strs[1], strs[0]);
          String val = "{\"siteId\":\"" + siteId + "\", \"appEnvironment\":\""
              + awsservice.environment("APP_ENVIRONMENT") + "\"}";
          ssmService.putParameter(key, val);
        }

      });
    }
  }
}
