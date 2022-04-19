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

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.stacks.common.objects.DynamicObject;

/** {@link ApiGatewayRequestHandler} for "/sites". */
public class SitesRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link Random}. */
  private Random rnd = new Random();
  
  /**
   * constructor.
   *
   */
  public SitesRequestHandler() {}

  /**
   * Generate Upload Email address.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param mailDomain {@link String}
   * @return {@link String}
   */
  private String generateUploadEmail(final LambdaLogger logger, final AwsServiceCache awsservice,
      final String mailDomain) {
    final int emaillength = 8;
    String email = getSaltString(emaillength) + "@" + mailDomain;

    while (isEmailExists(logger, awsservice, email)) {
      email = getSaltString(emaillength) + "@" + mailDomain;
    }

    return email;
  }

  /**
   * Get Mail Domain.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @return {@link String}
   */
  private String getMailDomain(final AwsServiceCache awsservice) {
    String key = "/formkiq/" + awsservice.appEnvironment() + "/maildomain";
    return awsservice.ssmService().getParameterValue(key);
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
   * Does Email address already exist.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param email {@link String}
   * @return boolean
   */
  private boolean isEmailExists(final LambdaLogger logger, final AwsServiceCache awsservice,
      final String email) {
    String[] strs = email.split("@");
    String key = String.format("/formkiq/ses/%s/%s", strs[1], strs[0]);
    return awsservice.ssmService().getParameterValue(key) != null;
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    List<DynamicObject> sites = authorizer.getSiteIds().stream().map(siteId -> {
      DynamicObject config = awsservice.config(siteId);
      config.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      return config;
    }).collect(Collectors.toList());
    
    sites.forEach(ob -> {
      ob.remove("PK");
      ob.remove("SK");
    });

    updateUploadEmail(logger, awsservice, authorizer, sites);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(Map.of("sites", sites)));
  }

  /**
   * Update Upload Email.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param authorizer {@link ApiAuthorizer}
   * @param sites {@link List} {@link DynamicObject}
   */
  private void updateUploadEmail(final LambdaLogger logger, final AwsServiceCache awsservice,
      final ApiAuthorizer authorizer, final List<DynamicObject> sites) {

    String mailDomain = getMailDomain(awsservice);

    if (mailDomain != null) {

      List<String> writeSiteIds = authorizer.getWriteSiteIds();

      sites.forEach(site -> {

        String siteId = site.getString("siteId");
        if (writeSiteIds.contains(siteId)) {
          String key =
              String.format("/formkiq/%s/siteid/%s/email", awsservice.appEnvironment(), siteId);
          site.put("uploadEmail", awsservice.ssmService().getParameterValue(key));
        }
      });

      sites.forEach(site -> {

        String siteId = site.getString("siteId");
        String uploadEmail = site.getString("uploadEmail");
        
        if (uploadEmail == null && writeSiteIds.contains(siteId)) {

          String email = generateUploadEmail(logger, awsservice, mailDomain);
          site.put("uploadEmail", email);

          String key =
              String.format("/formkiq/%s/siteid/%s/email", awsservice.appEnvironment(), siteId);
          awsservice.ssmService().putParameter(key, email);

          String[] strs = email.split("@");
          key = String.format("/formkiq/ses/%s/%s", strs[1], strs[0]);
          String val = "{\"siteId\":\"" + siteId + "\", \"appEnvironment\":\""
              + awsservice.appEnvironment() + "\"}";
          awsservice.ssmService().putParameter(key, val);
        }

      });
    }
  }

  @Override
  public String getRequestUrl() {
    return "/sites";
  }
}
