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
package com.formkiq.stacks.dynamodb;

import java.lang.reflect.InvocationTargetException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceExtension;

/**
 * 
 * {@link AwsServiceExtension} for {@link DocumentVersionService}.
 *
 */
public class DocumentVersionServiceExtension
    implements AwsServiceExtension<DocumentVersionService> {

  /** {@link DocumentVersionService}. */
  private DocumentVersionService service;

  /**
   * constructor.
   */
  public DocumentVersionServiceExtension() {}

  @Override
  public DocumentVersionService loadService(final AwsServiceCache awsServiceCache) {

    if (this.service == null) {

      try {

        this.service = (DocumentVersionService) Class
            .forName(awsServiceCache.environment("DOCUMENT_VERSIONS_PLUGIN")).getConstructor()
            .newInstance();

      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException | SecurityException
          | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    return this.service;
  }
}
