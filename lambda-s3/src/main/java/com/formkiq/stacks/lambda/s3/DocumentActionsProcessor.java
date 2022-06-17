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
package com.formkiq.stacks.lambda.s3;

import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.module.documentevents.DocumentEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** {@link RequestHandler} for handling Document Actions. */
@Reflectable
@ReflectableImport(classes = DocumentEvent.class)
public class DocumentActionsProcessor implements RequestHandler<Map<String, Object>, Void> {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  // /** {@link ActionsService}. */
  // private ActionsService actionsService;
  /** SQS Error Queue. */
  private String sqsErrorQueue;
  /** {@link SqsService}. */
  private SqsService sqsService;
  // /** {@link SsmConnectionBuilder}. */
  // private SsmConnectionBuilder ssmConnection;
  // /** {@link Region}. */
  // private Region region;
  // /** {@link AwsCredentials}. */
  // private AwsCredentials credentials;

  /** constructor. */
  public DocumentActionsProcessor() {
    this(System.getenv(), Region.of(System.getenv("AWS_REGION")),
        EnvironmentVariableCredentialsProvider.create().resolveCredentials(),
        new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new S3ConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SqsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SsmConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param awsRegion {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param dynamoDb {@link DynamoDbConnectionBuilder}
   * @param s3Builder {@link S3ConnectionBuilder}
   * @param sqsBuilder {@link SqsConnectionBuilder}
   * @param ssmConnectionBuilder {@link SsmConnectionBuilder}
   */
  protected DocumentActionsProcessor(final Map<String, String> map, final Region awsRegion,
      final AwsCredentials awsCredentials, final DynamoDbConnectionBuilder dynamoDb,
      final S3ConnectionBuilder s3Builder, final SqsConnectionBuilder sqsBuilder,
      final SsmConnectionBuilder ssmConnectionBuilder) {

    // this.region = awsRegion;
    // this.credentials = awsCredentials;
    // String documentsTable = map.get("DOCUMENTS_TABLE");
    // this.service = new DocumentServiceImpl(dynamoDb, documentsTable);
    // this.searchService =
    // new DocumentSearchServiceImpl(this.service, dynamoDb, documentsTable, null);
    // this.actionsService = new ActionsServiceDynamoDb(dynamoDb, documentsTable);
    // this.s3 = new S3Service(s3Builder);
    this.sqsService = new SqsService(sqsBuilder);
    // this.ssmConnection = ssmConnectionBuilder;

    // this.documentsBucket = map.get("DOCUMENTS_S3_BUCKET");
    this.sqsErrorQueue = map.get("SQS_ERROR_URL");
    // this.appEnvironment = map.get("APP_ENVIRONMENT");
  }

  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json = null;
    // Date date = new Date();

    try {

      LambdaLogger logger = context.getLogger();

      if ("true".equals(System.getenv("DEBUG"))) {
        json = this.gson.toJson(map);
        logger.log(json);
      }

      // List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      // processRecords(logger, date, records);

    } catch (Exception e) {
      e.printStackTrace();

      if (json == null) {
        json = this.gson.toJson(map);
      }

      if (this.sqsErrorQueue != null) {
        this.sqsService.sendMessage(this.sqsErrorQueue, json);
      }
    }

    return null;
  }
}
