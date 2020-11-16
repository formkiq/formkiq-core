/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
import com.formkiq.stacks.api.handler.PresetsIdRequestHandler;
import com.formkiq.stacks.api.handler.PresetsRequestHandler;
import com.formkiq.stacks.api.handler.PresetsTagsKeyRequestHandler;
import com.formkiq.stacks.api.handler.PresetsTagsRequestHandler;
import com.formkiq.stacks.api.handler.PublicDocumentsRequestHandler;
import com.formkiq.stacks.api.handler.SearchRequestHandler;
import com.formkiq.stacks.api.handler.SitesRequestHandler;
import com.formkiq.stacks.api.handler.VersionRequestHandler;
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

      case "/presets":
        return new PresetsRequestHandler();

      case "/presets/{presetId}":
        return new PresetsIdRequestHandler();

      case "/presets/{presetId}/tags":
        return new PresetsTagsRequestHandler();

      case "/presets/{presetId}/tags/{tagKey}":
        return new PresetsTagsKeyRequestHandler();

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
