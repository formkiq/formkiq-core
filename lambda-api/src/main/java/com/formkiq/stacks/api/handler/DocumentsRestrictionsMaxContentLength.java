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

import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;

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
  public String getValue(final AwsServiceCache awsservice, final String siteId) {
    return getValue(awsservice, siteId, ConfigService.MAX_DOCUMENT_SIZE_BYTES);
  }
}
