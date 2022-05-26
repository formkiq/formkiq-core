package com.formkiq.stacks.api;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.AwsServiceCache;
import com.formkiq.aws.services.lambda.services.ConfigService;
import com.formkiq.aws.services.lambda.services.ConfigServiceImpl;
import com.formkiq.aws.services.lambda.services.DynamoDbCacheService;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentCountServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
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
   * @param service {@link AwsServiceCache}
   * @return {@link CoreAwsServiceCache}
   */
  public static CoreAwsServiceCache cast(final AwsServiceCache service) {
    if (service instanceof CoreAwsServiceCache) {
      return (CoreAwsServiceCache) service;
    }
    
    throw new UnsupportedOperationException("expected CoreAwsServiceCache.class");
  }
  
  /** {@link DocumentSearchService}. */
  private DocumentSearchService documentSearchService;
  /** {@link ConfigService}. */
  private ConfigService configService;
  /** {@link DocumentService}. */
  private DocumentService documentService;
  /** {@link WebhooksService}. */
  private WebhooksService webhookService;
  /** {@link DynamoDbCacheService}. */
  private DynamoDbCacheService documentCacheService;
  /** {@link DocumentCountService}. */
  private DocumentCountService documentCountService;
  
  /**
   * Get SiteId Config.
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
      this.configService = new ConfigServiceImpl(dbConnection(), environment("DOCUMENTS_TABLE"));
    }
    return this.configService;
  }
  
  /**
   * Get {@link DynamoDbCacheService}.
   * 
   * @return {@link DynamoDbCacheService}
   */
  public DynamoDbCacheService documentCacheService() {
    if (this.documentCacheService == null) {
      this.documentCacheService =
          new DynamoDbCacheService(dbConnection(), environment("CACHE_TABLE"));
    }
    return this.documentCacheService;
  }
  
  /**
   * Get {@link DocumentCountService}.
   * 
   * @return {@link DocumentCountService}
   */
  public DocumentCountService documentCountService() {
    if (this.documentCountService == null) {
      this.documentCountService =
          new DocumentCountServiceDynamoDb(dbConnection(), environment("DOCUMENTS_TABLE"));
    }
    return this.documentCountService;
  }

  /**
   * Get {@link DocumentSearchService}.
   * 
   * @return {@link DocumentSearchService}
   */
  public DocumentSearchService documentSearchService() {
    if (this.documentSearchService == null) {
      this.documentSearchService = new DocumentSearchServiceImpl(documentService(),
          dbConnection(), environment("DOCUMENTS_TABLE"));
    }
    return this.documentSearchService;
  }

  /**
   * Get {@link DocumentService}.
   * 
   * @return {@link DocumentService}
   */
  public DocumentService documentService() {
    if (this.documentService == null) {
      this.documentService =
          new DocumentServiceImpl(dbConnection(), environment("DOCUMENTS_TABLE"));
    }
    return this.documentService;
  }

  /**
   * Get {@link WebhooksService}.
   * 
   * @return {@link WebhooksService}
   */
  public WebhooksService webhookService() {
    if (this.webhookService == null) {
      this.webhookService =
          new WebhooksServiceImpl(dbConnection(), environment("DOCUMENTS_TABLE"));
    }
    return this.webhookService;
  }
}
