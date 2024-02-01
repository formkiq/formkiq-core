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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentFoldersApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentWorkflowsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.RulesetsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.api.UserActivitiesApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.invoker.Configuration;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.AbstractFormKiqApiResponseCallback;
import com.formkiq.testutils.aws.FormKiqApiExtension;
import com.formkiq.testutils.aws.JwtTokenEncoder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Abstract Request Test using {@link ApiClient}.
 */
public abstract class AbstractApiClientRequestTest {

  /** FormKiQ Server. */
  @RegisterExtension
  static FormKiqApiExtension server = new FormKiqApiExtension(generateCallback());

  /** 500 Milliseconds. */
  private static final long SLEEP = 500L;
  /** Time out. */
  private static final int TIMEOUT = 30000;

  /**
   * Get Generate Callback.
   * 
   * @return {@link AbstractFormKiqApiResponseCallback}
   */
  private static AbstractFormKiqApiResponseCallback generateCallback() {
    return new FormKiQResponseCallback();
  }

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public static AwsServiceCache getAwsServices() {
    AwsServiceCache awsServiceCache = LocalStackExtension.getAwsServiceCache();

    Map<String, String> environment = new HashMap<>(awsServiceCache.environment());
    environment.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());
    awsServiceCache.environment(environment);

    awsServiceCache.register(SqsService.class, new SqsServiceExtension());
    awsServiceCache.register(ActionsService.class, new ActionsServiceExtension());
    awsServiceCache.register(ConfigService.class, new ConfigServiceExtension());
    awsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    awsServiceCache.register(DocumentSearchService.class, new DocumentSearchServiceExtension());
    awsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());

    return awsServiceCache;
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
  /** {@link RulesetsApi}. */
  protected RulesetsApi rulesetApi = new RulesetsApi(this.client);
  /** {@link SystemManagementApi}. */
  protected SystemManagementApi systemApi = new SystemManagementApi(this.client);
  /** {@link DocumentTagsApi}. */
  protected DocumentTagsApi tagsApi = new DocumentTagsApi(this.client);
  /** {@link UserActivitiesApi}. */
  protected UserActivitiesApi userActivitiesApi = new UserActivitiesApi(this.client);
  /** {@link DocumentWorkflowsApi}. */
  protected DocumentWorkflowsApi workflowApi = new DocumentWorkflowsApi(this.client);

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
   * Get Sqs Messages.
   * 
   * @return {@link List} {@link Message}
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  public List<Message> getSqsMessages() throws InterruptedException, URISyntaxException {

    String sqsDocumentEventUrl =
        server.getCallback().getMapEnvironment().get("SQS_DOCUMENT_EVENT_URL");
    SqsService sqsService = getAwsServices().getExtension(SqsService.class);

    List<Message> msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    while (msgs.isEmpty()) {
      Thread.sleep(SLEEP);
      msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    }

    for (Message msg : msgs) {
      sqsService.deleteMessage(sqsDocumentEventUrl, msg.receiptHandle());
    }

    return msgs;
  }

  /**
   * Get Validation Errors.
   * 
   * @param e {@link ApiException}
   * @return {@link ValidationException}
   */
  @SuppressWarnings("unchecked")
  public Collection<Map<String, Object>> getValidationErrors(final ApiException e) {
    Gson gson = new GsonBuilder().create();

    Map<String, Object> map = gson.fromJson(e.getResponseBody(), Map.class);
    Collection<Map<String, Object>> errors = (Collection<Map<String, Object>>) map.get("errors");
    return errors;
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
}
