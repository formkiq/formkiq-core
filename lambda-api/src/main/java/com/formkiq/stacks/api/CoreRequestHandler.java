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

import java.util.Map;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.lambda.apigateway.AbstractApiRequestHandler;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.exception.NotFoundException;
import com.formkiq.stacks.api.handler.DocumentIdContentRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdUrlRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentVersionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsIdUploadRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsOptionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsUploadRequestHandler;
import com.formkiq.stacks.api.handler.PrivateWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.PublicDocumentsRequestHandler;
import com.formkiq.stacks.api.handler.PublicWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.SearchRequestHandler;
import com.formkiq.stacks.api.handler.SitesRequestHandler;
import com.formkiq.stacks.api.handler.VersionRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksIdRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksTagsRequestHandler;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.Preset;
import com.formkiq.stacks.dynamodb.PresetTag;
import com.formkiq.stacks.dynamodb.SearchQuery;
import com.formkiq.stacks.dynamodb.SearchTagCriteria;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** {@link RequestStreamHandler} for handling API Gateway 'GET' requests. */
@Reflectable
@ReflectableImport(classes = {DocumentItemDynamoDb.class, DocumentTagType.class, DocumentTag.class,
    PaginationMapToken.class, SearchQuery.class, SearchTagCriteria.class, PresetTag.class,
    Preset.class})
public class CoreRequestHandler extends AbstractApiRequestHandler {

  /** Is Public Urls Enabled. */
  private static boolean isEnablePublicUrls;

  static {

    if (System.getenv("AWS_REGION") != null) {
      setUpHandler(System.getenv(),
          new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()),
          new S3ConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()),
          new SsmConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()),
          new SqsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()));
    }
  }

  /**
   * Setup Api Request Handlers.
   *
   * @param map {@link Map}
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param s3 {@link S3ConnectionBuilder}
   * @param ssm {@link SsmConnectionBuilder}
   * @param sqs {@link SqsConnectionBuilder}
   */
  protected static void setUpHandler(final Map<String, String> map,
      final DynamoDbConnectionBuilder builder, final S3ConnectionBuilder s3,
      final SsmConnectionBuilder ssm, final SqsConnectionBuilder sqs) {

    setAwsServiceCache(map, builder, s3, ssm, sqs);

    isEnablePublicUrls = isEnablePublicUrls(map);
  }

  /** constructor. */
  public CoreRequestHandler() {}

  @Override
  @SuppressWarnings("returncount")
  public ApiGatewayRequestHandler findRequestHandler(final String method, final String resource)
      throws NotFoundException {

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
    
    switch (s) {
      case "options":
        return new DocumentsOptionsRequestHandler();

      case "/version":
        return new VersionRequestHandler();

      case "/sites":
        return new SitesRequestHandler();

      case "/documents":
        return new DocumentsRequestHandler();

      case "/documents/{documentId}":
        return new DocumentIdRequestHandler();

      case "/documents/{documentId}/versions":
        return new DocumentVersionsRequestHandler();

      case "/documents/{documentId}/tags":
        return new DocumentTagsRequestHandler();

      case "/documents/{documentId}/tags/{tagKey}":
        return new DocumentTagRequestHandler();

      case "/documents/{documentId}/url":
        return new DocumentIdUrlRequestHandler();
      case "/documents/{documentId}/content":
        return new DocumentIdContentRequestHandler();

      case "/search":
        return new SearchRequestHandler();

      case "/documents/upload":
        return new DocumentsUploadRequestHandler();
      case "/documents/{documentId}/upload":
        return new DocumentsIdUploadRequestHandler();

      case "/webhooks/{webhookId}/tags":
        return new WebhooksTagsRequestHandler();
      case "/webhooks/{webhookId}":
        return new WebhooksIdRequestHandler();
      case "/webhooks":
        return new WebhooksRequestHandler();
        
      default:
        throw new NotFoundException(resource + " not found");
    }
  }

  /**
   * Whether to enable public urls.
   * 
   * @param map {@link Map}
   * @return boolean
   */
  protected static boolean isEnablePublicUrls(final Map<String, String> map) {
    return "true".equals(map.getOrDefault("ENABLE_PUBLIC_URLS", "false"));
  }
}
