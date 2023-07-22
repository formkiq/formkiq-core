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

import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static com.formkiq.testutils.aws.TestServices.OCR_BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static com.formkiq.testutils.aws.TypeSenseExtension.API_KEY;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.mockserver.mock.action.ExpectationResponseCallback;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPluginEmpty;
import com.formkiq.stacks.api.AbstractCoreRequestHandler;
import com.formkiq.stacks.api.CoreRequestHandler;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.AbstractFormKiqApiResponseCallback;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.testutils.aws.TypeSenseExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * 
 * FormKiQ implementation of {@link ExpectationResponseCallback}.
 *
 */
public class FormKiQResponseCallback extends AbstractFormKiqApiResponseCallback {

  /** {@link CoreRequestHandler}. */
  private CoreRequestHandler handler = new CoreRequestHandler();

  @Override
  public RequestStreamHandler getHandler() {
    return this.handler;
  }

  @Override
  public Map<String, String> getMapEnvironment() {

    try {
      Map<String, String> map = new HashMap<>();

      map.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());
      map.put("APP_ENVIRONMENT", FORMKIQ_APP_ENVIRONMENT);
      map.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
      map.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
      map.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
      map.put("CACHE_TABLE", CACHE_TABLE);
      map.put("DOCUMENTS_S3_BUCKET", BUCKET_NAME);
      map.put("STAGE_DOCUMENTS_S3_BUCKET", STAGE_BUCKET_NAME);
      map.put("OCR_S3_BUCKET", OCR_BUCKET_NAME);
      map.put("AWS_REGION", AWS_REGION.toString());
      map.put("DEBUG", "true");
      map.put("SQS_DOCUMENT_FORMATS",
          TestServices.getSqsDocumentFormatsQueueUrl(TestServices.getSqsConnection(null)));
      map.put("DISTRIBUTION_BUCKET", "formkiq-distribution-us-east-pro");
      map.put("FORMKIQ_TYPE", "core");
      map.put("USER_AUTHENTICATION", "cognito");
      map.put("WEBSOCKET_SQS_URL",
          TestServices.getSqsWebsocketQueueUrl(TestServices.getSqsConnection(null)));
      map.put("TYPESENSE_HOST", "http://localhost:" + TypeSenseExtension.getMappedPort());
      map.put("TYPESENSE_API_KEY", API_KEY);

      return map;

    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<String> getResourceUrls() {
    return this.handler.getUrlMap().values().stream().map(h -> h.getRequestUrl())
        .collect(Collectors.toList());
  }

  @Override
  public void initHandler() {

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);
    AbstractCoreRequestHandler.configureHandler(getMapEnvironment(), AWS_REGION,
        credentialsProvider, TestServices.getEndpointMap(), new DocumentTagSchemaPluginEmpty());
  }
}
