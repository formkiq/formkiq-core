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
package com.formkiq.stacks.lambda.s3.cognito;

import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.testutils.FileUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link CustomMessageHandler} Unit Tests. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class CustomMessageHandlerTest {

  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link SsmService}. */
  private static SsmService ssm;
  /** {@link CustomMessageHandler}. */
  private static CustomMessageHandler handler;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;

  /**
   * Before Class.
   *
   */
  @BeforeAll
  public static void beforeClass() {

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);
    Map<String, String> map = Map.of("AWS_REGION", "us-east-1", "DOMAIN", "test", "S3_BUCKET",
        "aws-cognito", "APP_ENVIRONMENT", "dev");

    awsServices =
        new AwsServiceCacheBuilder(map, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new S3AwsServiceRegistry(), new SsmAwsServiceRegistry()).build();

    awsServices.register(SsmService.class, new SsmServiceExtension());
    awsServices.register(S3Service.class, new S3ServiceExtension());

    s3 = awsServices.getExtension(S3Service.class);
    s3.createBucket("aws-cognito");

    ssm = awsServices.getExtension(SsmService.class);
    ssm.putParameter("/formkiq/cognito/test/CognitoHttpApiUrl", "http://localhost");
    ssm.putParameter("/formkiq/dev/console/Url", "http://localhost");

    handler = new CustomMessageHandler(awsServices);
  }

  private static CognitoCustomMessageEvent readJson(final String resourcePath) throws IOException {
    byte[] data = FileUtil.loadFileAsBytes(resourcePath);
    String s = new String(data, StandardCharsets.UTF_8);
    return GsonUtil.getInstance().fromJson(s, CognitoCustomMessageEvent.class);
  }

  /** {@link LambdaContextRecorder}. */
  private final LambdaContextRecorder context = new LambdaContextRecorder();

  @Test
  void customMessageAdminCreateUser_customSsmAndS3() throws Exception {
    // Put custom subject in SSM
    ssm.putParameter("/formkiq/cognito/" + awsServices.environment("DOMAIN")
        + "/CustomMessage_AdminCreateUser/Subject", "Test Subject");

    // Put custom message in S3
    String bucket = awsServices.environment("S3_BUCKET");
    String key = "formkiq/cognito/" + awsServices.environment("DOMAIN")
        + "/CustomMessage_AdminCreateUser/Message";

    s3.putObject(bucket, key,
        "Test ${email} ${link} ${emailLocal}".getBytes(StandardCharsets.UTF_8), "text/plain");

    CognitoCustomMessageEvent event = readJson("/cognito/CustomMessage_AdminCreateUser.json");
    CognitoCustomMessageEvent result = handler.handleRequest(event, context);

    assertEquals("Test Subject", result.response().emailSubject());

    assertEquals(
        "Test test@formkiq.com http://localhost/confirmRegistration?userStatus=FORCE_CHANGE_PASSWORD&code={####}&username=db1270e6-d939-4ef0-8f9b-8c5ee47100e3&clientId=CLIENT_ID_NOT_APPLICABLE&region=us-east-1&email={username} test",
        result.response().emailMessage());
  }

  @Test
  void customMessageAdminCreateUser_defaults() throws Exception {
    CognitoCustomMessageEvent event = readJson("/cognito/CustomMessage_AdminCreateUser.json");
    CognitoCustomMessageEvent result = handler.handleRequest(event, context);

    assertEquals("Your Account has been Created", result.response().emailSubject());

    assertEquals(
        "Your account has been created. <a href=\"http://localhost/confirmRegistration?userStatus=FORCE_CHANGE_PASSWORD&code={####}&username=db1270e6-d939-4ef0-8f9b-8c5ee47100e3&clientId=CLIENT_ID_NOT_APPLICABLE&region=us-east-1&email={username}\" target=\"_blank\">Click this link to finalize your account.</a>",
        result.response().emailMessage());
  }

  @Test
  void customMessageForgotPassword() throws Exception {
    CognitoCustomMessageEvent event = readJson("/cognito/forgot01.json");
    CognitoCustomMessageEvent result = handler.handleRequest(event, context);

    assertEquals("Your Reset Password link", result.response().emailSubject());

    // This assertion is copied from your Node test exactly.
    assertEquals(
        "You have requested a password reset. <a href=\"http://localhost/change-password?userStatus=CONFIRMED&code={####}&username=4b2857fe-9376-4d21-bf5c-fdc1bcba291b&clientId=57cqrjqsl9e193m9r2hamagn2k&region=us-east-2&email=test@formkiq.com\" target=\"_blank\">Click this link to Reset Password</a>",
        result.response().emailMessage());
  }

  // ---------- Helpers ----------

  @Test
  void customMessageSignUp() throws Exception {
    CognitoCustomMessageEvent event = readJson("/cognito/signup01.json");
    CognitoCustomMessageEvent result = handler.handleRequest(event, context);

    assertEquals("Your Verification Link", result.response().emailSubject());

    assertEquals(
        "Thank you for signing up. <a href=\"http://localhost/confirmSignUp?userStatus=UNCONFIRMED&code={####}&username=42575cda-2414-4ef5-8679-ed10d219d814&clientId=197cl4eouv0fcbkc60sj0n0tp2&region=us-east-2&email=test@formkiq.com\" target=\"_blank\">Click this link to verify</a>",
        result.response().emailMessage());
  }
}
