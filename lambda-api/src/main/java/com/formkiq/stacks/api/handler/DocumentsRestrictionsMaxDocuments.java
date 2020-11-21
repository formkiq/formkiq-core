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
public class DocumentsRestrictionsMaxDocuments implements DocumentsRestrictions {

  /**
   * constructor.
   * 
   */
  public DocumentsRestrictionsMaxDocuments() {}

  @Override
  public boolean enforced(final AwsServiceCache awsservice, final String siteId, final String value,
      final Object... objs) {
    boolean enforced = false;

    if (value != null) {
      try {
        long max = Long.parseLong(value);
        long doccount = awsservice.documentCountService().getDocumentCount(siteId);
        enforced = (doccount + 1) > max;

      } catch (NumberFormatException e) {
        enforced = false;
      }
    }
    return enforced;
  }

  @Override
  public String getSsmValue(final AwsServiceCache awsservice, final String siteId) {
    return getSsmValue(awsservice, siteId, "MaxDocuments");
  }
}
