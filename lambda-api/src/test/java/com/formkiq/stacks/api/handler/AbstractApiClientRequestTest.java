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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.services.lambda.ApiAuthorizationBuilder.COGNITO_READ_SUFFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.client.api.CustomIndexApi;
import com.formkiq.client.api.EntityApi;
import com.formkiq.client.api.MappingsApi;
import com.formkiq.client.api.ReindexApi;
import com.formkiq.client.api.UserManagementApi;
import com.formkiq.client.api.WebhooksApi;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.WebhooksService;
import com.formkiq.stacks.dynamodb.WebhooksServiceExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentAttributesApi;
import com.formkiq.client.api.DocumentFoldersApi;
import com.formkiq.client.api.DocumentSearchApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.SchemasApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.invoker.Configuration;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.FormKiqApiExtension;
import com.formkiq.testutils.api.JwtTokenEncoder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.testutils.aws.TypesenseExtension;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Abstract Request Test using {@link ApiClient}.
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public abstract class AbstractApiClientRequestTest {

  /** {@link LocalStackExtension}. */
  @RegisterExtension
  @Order(0)
  static LocalStackExtension localstack = new LocalStackExtension();

  /** {@link TypesenseExtension}. */
  @RegisterExtension
  @Order(1)
  static TypesenseExtension typeSense = new TypesenseExtension();
  /** FormKiQ Server. */
  @RegisterExtension
  @Order(2)
  static FormKiqApiExtension server = new FormKiqApiExtension(localstack, typeSense,
      () -> Map.of("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName()),
      new FormKiQResponseCallback());

  /** 500 Milliseconds. */
  private static final long SLEEP = 500L;
  /** Time out. */
  private static final int TIMEOUT = 30000;

  private static boolean isMatch(final List<Map<String, Object>> msgs, final String eventType,
      final String documentId) {
    return msgs.stream().anyMatch(m -> isMatch(m, eventType, documentId));
  }

  private static boolean isMatch(final Map<String, Object> m, final String eventType,
      final String documentId) {
    return eventType.equals(m.get("type")) && documentId.equals(m.get("documentId"));
  }

  /** {@link ApiClient}. */
  protected ApiClient client =
      Configuration.getDefaultApiClient().setReadTimeout(TIMEOUT).setBasePath(server.getBasePath());
  /** {@link DocumentActionsApi}. */
  protected DocumentActionsApi documentActionsApi = new DocumentActionsApi(this.client);
  /** {@link DocumentsApi}. */
  protected DocumentsApi documentsApi = new DocumentsApi(this.client);
  /** {@link DocumentFoldersApi}. */
  protected DocumentFoldersApi foldersApi = new DocumentFoldersApi(this.client);
  /** {@link SystemManagementApi}. */
  protected SystemManagementApi systemApi = new SystemManagementApi(this.client);
  /** {@link DocumentTagsApi}. */
  protected DocumentTagsApi tagsApi = new DocumentTagsApi(this.client);
  /** {@link DocumentSearchApi}. */
  protected DocumentSearchApi searchApi = new DocumentSearchApi(this.client);
  /** {@link AttributesApi}. */
  protected AttributesApi attributesApi = new AttributesApi(this.client);
  /** {@link DocumentAttributesApi}. */
  protected DocumentAttributesApi documentAttributesApi = new DocumentAttributesApi(this.client);
  /** {@link SchemasApi}. */
  protected SchemasApi schemasApi = new SchemasApi(this.client);
  /** {@link AdvancedDocumentSearchApi}. */
  protected AdvancedDocumentSearchApi advancedSearchApi =
      new AdvancedDocumentSearchApi(this.client);
  /** {@link MappingsApi}. */
  protected MappingsApi mappingsApi = new MappingsApi(this.client);
  /** {@link ReindexApi}. */
  protected ReindexApi reindexApi = new ReindexApi(this.client);
  /** {@link UserManagementApi}. */
  protected UserManagementApi userManagementApi = new UserManagementApi(this.client);
  /** {@link CustomIndexApi}. */
  protected CustomIndexApi indexApi = new CustomIndexApi(this.client);
  /** {@link WebhooksApi}. */
  protected WebhooksApi webhooksApi = new WebhooksApi(this.client);

  /** {@link EntityApi}. */
  protected EntityApi entityApi = new EntityApi(this.client);

  /** Sqs Messages. */
  private final List<Map<String, Object>> sqsMessages = new ArrayList<>();

  /**
   * Clear Sqs Messages.
   *
   */
  public void clearSqsMessages() {

    String sqsDocumentEventUrl = localstack.getSqsDocumentEventUrl();

    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(SqsService.class, new SqsServiceExtension());
    SqsService sqsService = awsServices.getExtension(SqsService.class);

    List<Message> msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    for (Message msg : msgs) {
      sqsService.deleteMessage(sqsDocumentEventUrl, msg.receiptHandle());
    }
  }

  /**
   * Convert JSON to Object.
   * 
   * @param <T> Class Type
   * @param json {@link String}
   * @param clazz {@link Class}
   * @return {@link Object}
   */
  protected <T> T fromJson(final String json, final Class<T> clazz) {
    return GsonUtil.getInstance().fromJson(json, clazz);
  }

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache getAwsServices() {

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    AwsServiceCache services = new AwsServiceCacheBuilder(server.getEnvironmentMap(),
        TestServices.getEndpointMap(), credentialsProvider)
        .addService(new DynamoDbAwsServiceRegistry()).addService(new S3AwsServiceRegistry())
        .addService(new SsmAwsServiceRegistry()).addService(new SqsAwsServiceRegistry()).build();

    services.register(S3Service.class, new S3ServiceExtension());
    services.register(SsmService.class, new SsmServiceExtension());
    services.register(WebhooksService.class, new WebhooksServiceExtension());
    services.register(ConfigService.class, new ConfigServiceExtension());

    return services;
  }

  /**
   * Get Sqs Messages.
   *
   * @param eventType {@link String}
   * @param documentId {@link String}
   *
   * @return {@link Map} {@link Message}
   * @throws InterruptedException InterruptedException
   */
  public Map<String, Object> getSqsMessages(final String eventType, final String documentId)
      throws InterruptedException {

    String sqsDocumentEventUrl = localstack.getSqsDocumentEventUrl();

    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(SqsService.class, new SqsServiceExtension());
    SqsService sqsService = awsServices.getExtension(SqsService.class);

    List<Message> msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    List<Map<String, Object>> list = msgs.stream().map(this::transform).toList();
    sqsMessages.addAll(list);

    while (sqsMessages.isEmpty() || !isMatch(sqsMessages, eventType, documentId)) {
      Thread.sleep(SLEEP);
      msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
      sqsMessages.addAll(msgs.stream().map(this::transform).toList());
    }

    return sqsMessages.stream().filter(m -> isMatch(m, eventType, documentId)).findAny().get();
  }

  /**
   * Get Validation Errors.
   * 
   * @param e {@link ApiException}
   * @return {@link ValidationException}
   */
  public Collection<Map<String, Object>> getValidationErrors(final ApiException e) {
    Gson gson = new GsonBuilder().create();

    Map<String, Object> map = gson.fromJson(e.getResponseBody(), Map.class);
    return (Collection<Map<String, Object>>) map.get("errors");
  }

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   */
  public void setBearerToken(final String siteId) {
    String jwt = JwtTokenEncoder
        .encodeCognito(new String[] {siteId != null ? siteId : DEFAULT_SITE_ID}, "joesmith");
    this.client.addDefaultHeader("Authorization", jwt);
  }

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   * @param readonly boolean
   */
  public void setBearerToken(final String siteId, final boolean readonly) {
    String permission = siteId != null ? siteId : DEFAULT_SITE_ID;
    permission = readonly ? permission + COGNITO_READ_SUFFIX : permission;

    String jwt = JwtTokenEncoder.encodeCognito(new String[] {permission}, "joesmith");
    this.client.addDefaultHeader("Authorization", jwt);
  }

  /**
   * Set BearerToken.
   * 
   * @param groups {@link String}
   */
  public void setBearerToken(final String[] groups) {
    String jwt = JwtTokenEncoder.encodeCognito(groups, "joesmith");
    this.client.addDefaultHeader("Authorization", jwt);
  }

  private Map<String, Object> transform(final Message msg) {
    Map<String, Object> map = fromJson(msg.body(), Map.class);
    return fromJson((String) map.get("Message"), Map.class);
  }
}
