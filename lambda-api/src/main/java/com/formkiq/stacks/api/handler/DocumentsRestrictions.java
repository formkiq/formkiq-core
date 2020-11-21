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
