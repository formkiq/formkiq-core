/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.handler;

import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiSitesResponse;
import com.formkiq.stacks.api.Site;

/** {@link RequestHandler} for GET "/version". */
public class SitesRequestHandler implements RequestHandler {

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
    Random rnd = new Random();
    while (salt.length() < length) {
      int index = (int) (rnd.nextFloat() * saltchars.length());
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
  public boolean isReadonly(final String method) {
    return true;
  }

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    List<Site> sites = authorizer.getSiteIds().stream().map(s -> {
      Site site = new Site();
      site.setSiteId(s);
      return site;
    }).collect(Collectors.toList());

    updateUploadEmail(logger, awsservice, authorizer, sites);

    ApiSitesResponse resp = new ApiSitesResponse();
    resp.setSites(sites);

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  /**
   * Update Upload Email.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param authorizer {@link ApiAuthorizer}
   * @param sites {@link List} {@link Site}
   */
  private void updateUploadEmail(final LambdaLogger logger, final AwsServiceCache awsservice,
      final ApiAuthorizer authorizer, final List<Site> sites) {

    String mailDomain = getMailDomain(awsservice);

    if (mailDomain != null) {

      List<String> writeSiteIds = authorizer.getWriteSiteIds();

      sites.forEach(site -> {

        if (writeSiteIds.contains(site.getSiteId())) {
          String key = String.format("/formkiq/%s/siteid/%s/email", awsservice.appEnvironment(),
              site.getSiteId());
          site.setUploadEmail(awsservice.ssmService().getParameterValue(key));
        }
      });

      sites.forEach(site -> {

        if (site.getUploadEmail() == null && writeSiteIds.contains(site.getSiteId())) {

          String email = generateUploadEmail(logger, awsservice, mailDomain);
          site.setUploadEmail(email);

          String key = String.format("/formkiq/%s/siteid/%s/email", awsservice.appEnvironment(),
              site.getSiteId());
          awsservice.ssmService().putParameter(key, email);

          String[] strs = email.split("@");
          key = String.format("/formkiq/ses/%s/%s", strs[1], strs[0]);
          String val = "{\"siteId\":\"" + site.getSiteId() + "\", \"appEnvironment\":\""
              + awsservice.appEnvironment() + "\"}";
          awsservice.ssmService().putParameter(key, val);
        }

      });
    }
  }
}
