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

/**
 * {@link DocumentsRestrictions} for Max Number of Documents.
 *
 */
public class DocumentsRestrictionsMaxContentLength implements DocumentsRestrictions {

  /**
   * constructor.
   * 
   */
  public DocumentsRestrictionsMaxContentLength() {}

  @Override
  public boolean enforced(final AwsServiceCache awsservice, final String siteId, final String value,
      final Object... objs) {

    boolean enforced = false;
    Long contentLength = (Long) objs[0];
    Long maxContentLength = getMaxContentLength(value);

    if (maxContentLength != null) {
      enforced = (contentLength == null || contentLength.longValue() == 0)
          || (contentLength.longValue() > maxContentLength.longValue());
    }

    return enforced;
  }

  /**
   * Get the Max Content Length from SSM.
   * 
   * @param value {@link String}
   * @return {@link Long}
   */
  private Long getMaxContentLength(final String value) {

    Long ret = null;

    try {
      ret = Long.valueOf(value);
    } catch (NumberFormatException e) {
      ret = null;
    }

    return ret;
  }

  @Override
  public String getSsmValue(final AwsServiceCache awsservice, final String siteId) {
    return getSsmValue(awsservice, siteId, "MaxContentLengthBytes");
  }
}
