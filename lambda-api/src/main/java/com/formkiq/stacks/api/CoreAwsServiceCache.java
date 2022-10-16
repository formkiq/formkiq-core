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
package com.formkiq.stacks.api;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.services.lambda.services.ConfigService;
import com.formkiq.aws.services.lambda.services.ConfigServiceImpl;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.WebhooksService;
import com.formkiq.stacks.dynamodb.WebhooksServiceImpl;

/**
 * 
 * Core {@link AwsServiceCache}.
 *
 */
public class CoreAwsServiceCache extends AwsServiceCache {

  /**
   * Casts {@link AwsServiceCache} to {@link CoreAwsServiceCache}.
   * 
   * @param service {@link AwsServiceCache}
   * @return {@link CoreAwsServiceCache}
   */
  public static CoreAwsServiceCache cast(final AwsServiceCache service) {
    if (service instanceof CoreAwsServiceCache) {
      return (CoreAwsServiceCache) service;
    }

    throw new UnsupportedOperationException("expected CoreAwsServiceCache.class");
  }

  /** {@link ConfigService}. */
  private ConfigService configService;
  /** {@link WebhooksService}. */
  private WebhooksService webhookService;

  /**
   * Get SiteId Config.
   * 
   * @param siteId {@link String}
   * @return {@link DynamicObject}
   */
  public DynamicObject config(final String siteId) {
    return configService().get(siteId);
  }

  /**
   * Get {@link ConfigService}.
   * 
   * @return {@link ConfigService}
   */
  public ConfigService configService() {
    if (this.configService == null) {
      DynamoDbConnectionBuilder connection = getExtension(DynamoDbConnectionBuilder.class);
      this.configService = new ConfigServiceImpl(connection, environment("DOCUMENTS_TABLE"));
    }
    return this.configService;
  }

  /**
   * Get {@link DocumentSearchService}.
   * 
   * @return {@link DocumentSearchService}
   */
  public DocumentSearchService documentSearchService() {
    return getExtension(DocumentSearchService.class);
  }

  /**
   * Get {@link DocumentService}.
   * 
   * @return {@link DocumentService}
   */
  public DocumentService documentService() {
    return getExtension(DocumentService.class);
  }

  /**
   * Get {@link WebhooksService}.
   * 
   * @return {@link WebhooksService}
   */
  public WebhooksService webhookService() {
    if (this.webhookService == null) {
      DynamoDbConnectionBuilder connection = getExtension(DynamoDbConnectionBuilder.class);
      this.webhookService = new WebhooksServiceImpl(connection, environment("DOCUMENTS_TABLE"));
    }
    return this.webhookService;
  }
}
