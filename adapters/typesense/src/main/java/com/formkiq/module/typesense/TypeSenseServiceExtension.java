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
package com.formkiq.module.typesense;

import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

/**
 * 
 * {@link AwsServiceExtension} for {@link TypeSenseService}.
 *
 */
public class TypeSenseServiceExtension implements AwsServiceExtension<TypeSenseService> {

  /** {@link TypeSenseService}. */
  private TypeSenseService service;

  /**
   * constructor.
   * 
   */
  public TypeSenseServiceExtension() {}

  @Override
  public TypeSenseService loadService(final AwsServiceCache awsServiceCache) {

    if (this.service == null) {
      AwsCredentials credentials = awsServiceCache.getExtension(AwsCredentials.class);
      this.service = new TypeSenseServiceImpl(awsServiceCache.environment("TYPESENSE_HOST"),
          awsServiceCache.environment("TYPESENSE_API_KEY"), awsServiceCache.region(), credentials);
    }

    return this.service;
  }
}
