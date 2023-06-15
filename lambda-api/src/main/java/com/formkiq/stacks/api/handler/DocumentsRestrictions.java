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
   * Get Value.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param key {@link String}
   * @return {@link String}
   */
  default String getValue(final AwsServiceCache awsservice, final String siteId, final String key) {
    ConfigService configService = awsservice.getExtension(ConfigService.class);
    return configService.get(siteId).getString(key);
  }

  /**
   * Get Value.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @return {@link String}
   */
  String getValue(AwsServiceCache awsservice, String siteId);
}
