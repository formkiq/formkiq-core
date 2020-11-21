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

import com.formkiq.lambda.apigateway.AwsServiceCache;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

/**
 * Interface for providing restrictions around Document.
 *
 */
public interface DocumentsRestrictions {

  /**
   * Whether Restriction is being enforced.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param value {@link String}
   * @param objs {@link Object}
   * @return boolean
   */
  boolean enforced(AwsServiceCache awsservice, String siteId, String value, Object... objs);

  /**
   * Get SSM Vaoue.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param key {@link String}
   * @return {@link String}
   */
  default String getSsmValue(final AwsServiceCache awsservice, final String siteId,
      final String key) {
    String ssmkey =
        siteId != null ? "/formkiq/" + awsservice.appEnvironment() + "/siteid/" + siteId + "/" + key
            : "/formkiq/" + awsservice.appEnvironment() + "/siteid/default/" + key;

    try {
      return awsservice.ssmService().getParameterValue(ssmkey);
    } catch (ParameterNotFoundException e) {
      return null;
    }
  }

  /**
   * Get Ssm Value.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @return {@link String}
   */
  String getSsmValue(AwsServiceCache awsservice, String siteId);
}
