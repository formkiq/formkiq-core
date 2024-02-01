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

import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.cognito.CognitoIdentityProviderService;
import com.formkiq.aws.cognito.CognitoIdentityProviderServiceExtension;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3PresignerServiceExtension;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.services.lambda.AbstractRestApiRequestHandler;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.LambdaInputRecord;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.aws.services.lambda.services.DynamoDbCacheServiceExtension;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.EventServiceSnsExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.ocr.DocumentOcrServiceExtension;
import com.formkiq.module.ocr.DocumentsOcrRequestHandler;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceExtension;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPluginExtension;
import com.formkiq.stacks.api.handler.ConfigurationApiKeyRequestHandler;
import com.formkiq.stacks.api.handler.ConfigurationApiKeysRequestHandler;
import com.formkiq.stacks.api.handler.ConfigurationRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdContentRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdRestoreRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdUrlRequestHandler;
import com.formkiq.stacks.api.handler.DocumentPermissionsKeyRequestHandler;
import com.formkiq.stacks.api.handler.DocumentPermissionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagValueRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentVersionsKeyRequestHandler;
import com.formkiq.stacks.api.handler.DocumentVersionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsActionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsActionsRetryRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsCompressRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsFulltextRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsFulltextRequestTagsKeyHandler;
import com.formkiq.stacks.api.handler.DocumentsFulltextRequestTagsKeyValueHandler;
import com.formkiq.stacks.api.handler.DocumentsIdAccessAttributesRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsIdUploadRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsOptionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsSyncsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsUploadRequestHandler;
import com.formkiq.stacks.api.handler.EsignatureDocusignConfigRequestHandler;
import com.formkiq.stacks.api.handler.EsignatureDocusignDocumentIdRequestHandler;
import com.formkiq.stacks.api.handler.FoldersIndexKeyRequestHandler;
import com.formkiq.stacks.api.handler.FoldersRequestHandler;
import com.formkiq.stacks.api.handler.GroupsRequestHandler;
import com.formkiq.stacks.api.handler.GroupsUsersRequestHandler;
import com.formkiq.stacks.api.handler.IndicesFolderMoveRequestHandler;
import com.formkiq.stacks.api.handler.IndicesRequestHandler;
import com.formkiq.stacks.api.handler.IndicesSearchRequestHandler;
import com.formkiq.stacks.api.handler.OnlyOfficeEditRequestHandler;
import com.formkiq.stacks.api.handler.OnlyOfficeNewRequestHandler;
import com.formkiq.stacks.api.handler.OnlyOfficeSaveRequestHandler;
import com.formkiq.stacks.api.handler.OpaConfigurationRequestHandler;
import com.formkiq.stacks.api.handler.OpaIdConfigurationRequestHandler;
import com.formkiq.stacks.api.handler.PrivateWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.PublicDocumentsRequestHandler;
import com.formkiq.stacks.api.handler.PublicWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.QueueDocumentsRequestHandler;
import com.formkiq.stacks.api.handler.QueueIdRequestHandler;
import com.formkiq.stacks.api.handler.QueuesRequestHandler;
import com.formkiq.stacks.api.handler.RulesetsIdRequestHandler;
import com.formkiq.stacks.api.handler.RulesetsRequestHandler;
import com.formkiq.stacks.api.handler.RulesetsRuleIdRequestHandler;
import com.formkiq.stacks.api.handler.RulesetsRuleRequestHandler;
import com.formkiq.stacks.api.handler.SearchFulltextRequestHandler;
import com.formkiq.stacks.api.handler.SearchRequestHandler;
import com.formkiq.stacks.api.handler.SitesRequestHandler;
import com.formkiq.stacks.api.handler.TagSchemasIdRequestHandler;
import com.formkiq.stacks.api.handler.TagSchemasRequestHandler;
import com.formkiq.stacks.api.handler.UpdateDocumentMatchingRequestHandler;
import com.formkiq.stacks.api.handler.UserActivitiesDocumentIdRequestHandler;
import com.formkiq.stacks.api.handler.UserActivitiesRequestHandler;
import com.formkiq.stacks.api.handler.VersionRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksIdRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksTagsRequestHandler;
import com.formkiq.stacks.api.handler.WorkflowDocumentsRequestHandler;
import com.formkiq.stacks.api.handler.WorkflowsIdRequestHandler;
import com.formkiq.stacks.api.handler.WorkflowsRequestHandler;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.stacks.dynamodb.ApiKeysServiceExtension;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentCountServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.FolderIndexProcessorExtension;
import com.formkiq.stacks.dynamodb.WebhooksService;
import com.formkiq.stacks.dynamodb.WebhooksServiceExtension;

/**
 * 
 * {@link AbstractCoreRequestHandler}.
 *
 */
public abstract class AbstractCoreRequestHandler extends AbstractRestApiRequestHandler {

  /** Is Public Urls Enabled. */
  private static boolean isEnablePublicUrls;
  /** Url Class Map. */
  private static final Map<String, ApiGatewayRequestHandler> URL_MAP = new HashMap<>();

  private static void addAccessControlEndpoints() {
    addRequestHandler(new OpaConfigurationRequestHandler());
    addRequestHandler(new OpaIdConfigurationRequestHandler());
    addRequestHandler(new DocumentsIdAccessAttributesRequestHandler());
  }

  private static void addEsignatureEndpoints() {
    addRequestHandler(new EsignatureDocusignDocumentIdRequestHandler());
    addRequestHandler(new EsignatureDocusignConfigRequestHandler());
  }

  private static void addGroupUsersEndpoints() {
    addRequestHandler(new GroupsRequestHandler());
    addRequestHandler(new GroupsUsersRequestHandler());
  }

  private static void addOnlyOfficeEndpoints() {
    addRequestHandler(new OnlyOfficeNewRequestHandler());
    addRequestHandler(new OnlyOfficeSaveRequestHandler());
    addRequestHandler(new OnlyOfficeEditRequestHandler());
  }

  /**
   * Add Url Request Handler Mapping.
   * 
   * @param handler {@link ApiGatewayRequestHandler}
   */
  public static void addRequestHandler(final ApiGatewayRequestHandler handler) {
    URL_MAP.put(handler.getRequestUrl(), handler);
  }

  private static void addRulesetsEndpoints() {
    addRequestHandler(new RulesetsRequestHandler());
    addRequestHandler(new RulesetsIdRequestHandler());
    addRequestHandler(new RulesetsRuleRequestHandler());
    addRequestHandler(new RulesetsRuleIdRequestHandler());
  }

  private static void addUserActivitiesEndpoints() {
    addRequestHandler(new UserActivitiesRequestHandler());
    addRequestHandler(new UserActivitiesDocumentIdRequestHandler());
  }

  private static void addWorkflowEndpoints() {
    addRequestHandler(new WorkflowsRequestHandler());
    addRequestHandler(new WorkflowsIdRequestHandler());
    addRequestHandler(new QueuesRequestHandler());
    addRequestHandler(new QueueIdRequestHandler());
    addRequestHandler(new QueueDocumentsRequestHandler());
    addRequestHandler(new WorkflowDocumentsRequestHandler());
  }

  /**
   * Build Core UrlMap.
   */
  private static void buildUrlMap() {
    URL_MAP.put("options", new DocumentsOptionsRequestHandler());
    addRequestHandler(new VersionRequestHandler());
    addRequestHandler(new SitesRequestHandler());
    addRequestHandler(new ConfigurationRequestHandler());
    addRequestHandler(new ConfigurationApiKeysRequestHandler());
    addRequestHandler(new ConfigurationApiKeyRequestHandler());
    addAccessControlEndpoints();

    addRequestHandler(new DocumentVersionsRequestHandler());
    addRequestHandler(new DocumentVersionsKeyRequestHandler());
    addRequestHandler(new DocumentPermissionsRequestHandler());
    addRequestHandler(new DocumentPermissionsKeyRequestHandler());
    addRequestHandler(new DocumentTagsRequestHandler());
    addRequestHandler(new DocumentsActionsRequestHandler());
    addRequestHandler(new DocumentsActionsRetryRequestHandler());
    addRequestHandler(new FoldersRequestHandler());
    addRequestHandler(new FoldersIndexKeyRequestHandler());
    addRequestHandler(new DocumentTagValueRequestHandler());
    addRequestHandler(new DocumentTagRequestHandler());
    addRequestHandler(new DocumentIdUrlRequestHandler());
    addRequestHandler(new DocumentIdContentRequestHandler());
    addRequestHandler(new SearchRequestHandler());
    addRequestHandler(new SearchFulltextRequestHandler());
    addRequestHandler(new DocumentsFulltextRequestTagsKeyHandler());
    addRequestHandler(new DocumentsFulltextRequestTagsKeyValueHandler());
    addRequestHandler(new DocumentsUploadRequestHandler());
    addRequestHandler(new DocumentsIdUploadRequestHandler());
    addRequestHandler(new DocumentsOcrRequestHandler());
    addRequestHandler(new DocumentsSyncsRequestHandler());
    addRequestHandler(new DocumentsFulltextRequestHandler());
    addRequestHandler(new TagSchemasRequestHandler());
    addRequestHandler(new TagSchemasIdRequestHandler());
    addRequestHandler(new WebhooksTagsRequestHandler());
    addRequestHandler(new WebhooksIdRequestHandler());
    addRequestHandler(new WebhooksRequestHandler());
    addRequestHandler(new DocumentsRequestHandler());
    addRequestHandler(new DocumentIdRequestHandler());
    addRequestHandler(new DocumentIdRestoreRequestHandler());
    addRequestHandler(new DocumentsCompressRequestHandler());
    addRequestHandler(new IndicesFolderMoveRequestHandler());
    addRequestHandler(new IndicesRequestHandler());
    addRequestHandler(new IndicesSearchRequestHandler());
    addEsignatureEndpoints();
    addRequestHandler(new UpdateDocumentMatchingRequestHandler());
    addOnlyOfficeEndpoints();
    addGroupUsersEndpoints();
    addWorkflowEndpoints();
    addUserActivitiesEndpoints();
    addRulesetsEndpoints();
  }

  /**
   * Initialize.
   * 
   * @param serviceCache {@link AwsServiceCache}
   * @param plugin {@link DocumentTagSchemaPlugin}
   */
  public static void initialize(final AwsServiceCache serviceCache,
      final DocumentTagSchemaPlugin plugin) {

    registerExtensions(serviceCache, plugin);

    if (serviceCache.hasModule("typesense")) {
      serviceCache.register(TypeSenseService.class, new TypeSenseServiceExtension());
    }

    isEnablePublicUrls = isEnablePublicUrls(serviceCache);

    buildUrlMap();
  }

  /**
   * Whether to enable public urls.
   *
   * @param serviceCache {@link AwsServiceCache}
   * @return boolean
   */
  private static boolean isEnablePublicUrls(final AwsServiceCache serviceCache) {
    return "true".equals(serviceCache.environment().getOrDefault("ENABLE_PUBLIC_URLS", "false"));
  }

  /**
   * Register Extensions.
   *
   * @param serviceCache {@link AwsServiceCache}
   * @param schemaEvents {@link DocumentTagSchemaPlugin}
   */
  private static void registerExtensions(final AwsServiceCache serviceCache,
      final DocumentTagSchemaPlugin schemaEvents) {

    serviceCache.register(CognitoIdentityProviderService.class,
        new CognitoIdentityProviderServiceExtension());
    serviceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    serviceCache.register(EventService.class, new EventServiceSnsExtension());
    serviceCache.register(ActionsNotificationService.class,
        new ActionsNotificationServiceExtension());
    serviceCache.register(ActionsService.class, new ActionsServiceExtension());
    serviceCache.register(SsmService.class, new SsmServiceExtension());
    serviceCache.register(S3Service.class, new S3ServiceExtension());
    serviceCache.register(S3PresignerService.class, new S3PresignerServiceExtension());
    serviceCache.register(SqsService.class, new SqsServiceExtension());
    serviceCache.register(DocumentTagSchemaPlugin.class,
        new DocumentTagSchemaPluginExtension(schemaEvents));
    serviceCache.register(CacheService.class, new DynamoDbCacheServiceExtension());
    serviceCache.register(DocumentService.class, new DocumentServiceExtension());
    serviceCache.register(DocumentSearchService.class, new DocumentSearchServiceExtension());
    serviceCache.register(DocumentCountService.class, new DocumentCountServiceExtension());
    serviceCache.register(FolderIndexProcessor.class, new FolderIndexProcessorExtension());
    serviceCache.register(ConfigService.class, new ConfigServiceExtension());
    serviceCache.register(ApiKeysService.class, new ApiKeysServiceExtension());
    serviceCache.register(DocumentSyncService.class, new DocumentSyncServiceExtension());
    serviceCache.register(DocumentOcrService.class, new DocumentOcrServiceExtension());
    serviceCache.register(DynamoDbService.class, new DynamoDbServiceExtension());
    serviceCache.register(WebhooksService.class, new WebhooksServiceExtension());
  }

  /** constructor. */
  public AbstractCoreRequestHandler() {}

  @Override
  @SuppressWarnings("returncount")
  public ApiGatewayRequestHandler findRequestHandler(
      final Map<String, ApiGatewayRequestHandler> urlMap, final String method,
      final String resource) throws NotFoundException {

    String s = "options".equals(method) ? method : resource;

    if (isEnablePublicUrls && "/public/documents".equals(s)) {
      return new PublicDocumentsRequestHandler();
    }

    if (s.startsWith("/public/webhooks")) {
      return new PublicWebhooksRequestHandler();
    }

    if (s.startsWith("/private/webhooks")) {
      return new PrivateWebhooksRequestHandler();
    }

    ApiGatewayRequestHandler hander = URL_MAP.get(s);
    if (hander != null) {
      return hander;
    }

    throw new NotFoundException(resource + " not found");
  }

  @Override
  public Map<String, ApiGatewayRequestHandler> getUrlMap() {
    return URL_MAP;
  }

  @Override
  public void handleSqsRequest(final LambdaLogger logger, final AwsServiceCache services,
      final LambdaInputRecord record) {
    // empty
  }
}
