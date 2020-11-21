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
package com.formkiq.stacks.lambda.s3.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse;
import software.amazon.awssdk.services.s3.model.LambdaFunctionConfiguration;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * Test CloudFormation.
 */
public class AwsResourceTest extends AbstractAwsTest {
  /** Sleep Timeout. */
  private static final long SLEEP = 500L;
  /** Test Timeout. */
  private static final long TEST_TIMEOUT = 30000L;

  /**
   * Assert {@link LambdaFunctionConfiguration}.
   * 
   * @param c {@link LambdaFunctionConfiguration}
   * @param arn {@link String}
   * @param event {@link String}
   */
  private static void assertLambdaFunctionConfigurations(final LambdaFunctionConfiguration c,
      final String arn, final String event) {
    assertTrue(c.lambdaFunctionArn().contains(arn));
    assertEquals(1, c.events().size());
    Event e = c.events().get(0);
    assertEquals(event, e.toString());
  }

  /**
   * Assert Received Message.
   * 
   * @param queueUrl {@link String}
   * @throws InterruptedException InterruptedException
   */
  @SuppressWarnings("unchecked")
  private static void assertSnsMessage(final String queueUrl) throws InterruptedException {

    List<Message> receiveMessages = getSqsService().receiveMessages(queueUrl).messages();
    while (receiveMessages.size() != 1) {
      Thread.sleep(SLEEP);
      receiveMessages = getSqsService().receiveMessages(queueUrl).messages();
    }

    assertEquals(1, receiveMessages.size());
    String body = receiveMessages.get(0).body();

    Gson gson = new GsonBuilder().create();
    Map<String, String> map = gson.fromJson(body, Map.class);
    String message = map.get("Message");
    map = gson.fromJson(message, Map.class);

    assertNotNull(map.get("documentId"));
    assertNotNull(map.get("type"));
  }

  /**
   * Create SQS Queue.
   * 
   * @param queueName {@link String}
   * @return {@link CreateQueueResponse}
   */
  private CreateQueueResponse createSqsQueue(final String queueName) {
    Map<QueueAttributeName, String> attributes = new HashMap<>();
    attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "20");

    CreateQueueRequest request =
        CreateQueueRequest.builder().queueName(queueName).attributes(attributes).build();
    return getSqsService().createQueue(request);
  }

  /**
   * Subscribe Sqs to Sns.
   * 
   * @param topicArn {@link String}
   * @param queueUrl {@link String}
   * @return {@link String}
   */
  private String subscribeToSns(final String topicArn, final String queueUrl) {
    String queueArn = getSqsService().getQueueArn(queueUrl);

    Map<QueueAttributeName, String> attributes = new HashMap<>();
    attributes.put(QueueAttributeName.POLICY, "{\"Version\":\"2012-10-17\",\"Id\":\"Queue_Policy\","
        + "\"Statement\":{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"sqs:SendMessage\","
        + "\"Resource\":\"*\"}}");

    SetQueueAttributesRequest setAttributes =
        SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(attributes).build();
    getSqsService().setQueueAttributes(setAttributes);

    String subscriptionArn = getSnsService().subscribe(topicArn, "sqs", queueArn).subscriptionArn();
    return subscriptionArn;
  }

  /**
   * Test Adding a file and then deleting it.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT * 2)
  public void testAddDeleteFile01() throws Exception {
    // given
    String key = UUID.randomUUID().toString();

    String createQueue = "createtest-" + UUID.randomUUID();
    String createQueueUrl = createSqsQueue(createQueue).queueUrl();
    String subscriptionCreateArn =
        subscribeToSns(getSnsDocumentsCreateEventTopicArn(), createQueueUrl);

    String deleteQueue = "deletetest-" + UUID.randomUUID();
    String deleteQueueUrl = createSqsQueue(deleteQueue).queueUrl();
    String subscriptionDeleteArn =
        subscribeToSns(getSnsDocumentsDeleteEventTopicArn(), deleteQueueUrl);
    String contentType = "text/plain";

    try {

      try (S3Client s3 = getS3Service().buildClient()) {

        // when
        key = writeToStaging(s3, key, contentType);

        // then
        verifyFileExistsInDocumentsS3(s3, key, contentType);
        verifyFileNotExistInStagingS3(s3, key);
        assertSnsMessage(createQueueUrl);

        // when
        key = writeToStaging(s3, key, contentType);

        // then
        verifyFileExistsInDocumentsS3(s3, key, contentType);
        verifyFileNotExistInStagingS3(s3, key);
        assertSnsMessage(createQueueUrl);

        // when
        getS3Service().deleteObject(s3, getDocumentsbucketname(), key);

        // then
        assertSnsMessage(deleteQueueUrl);
      }

    } finally {
      getSnsService().unsubscribe(subscriptionCreateArn);
      getSqsService().deleteQueue(createQueueUrl);

      getSnsService().unsubscribe(subscriptionDeleteArn);
      getSqsService().deleteQueue(deleteQueueUrl);
    }
  }

  /**
   * Test using a Presigned URL to Adding a file and then deleting it.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testAddDeleteFile02() throws Exception {
    // given
    final Long contentLength = Long.valueOf(36);
    String key = UUID.randomUUID().toString();
    String contentType = "text/plain";
    DocumentItem item = new DocumentItemDynamoDb(key, new Date(), "test");

    try (S3Client s3 = getS3Service().buildClient()) {

      // when
      getDocumentService().saveDocument(null, item, null);
      key = writeToDocuments(s3, key, contentType);

      // then
      verifyFileExistsInDocumentsS3(s3, key, contentType);

      item = getDocumentService().findDocument(null, key);

      while (true) {
        if (contentType.equals(item.getContentType())) {
          assertEquals(contentType, item.getContentType());
          assertEquals(contentLength, item.getContentLength());

          break;
        }

        item = getDocumentService().findDocument(null, key);
      }

      getS3Service().deleteObject(s3, getDocumentsbucketname(), key);
    }
  }

  /**
   * Test Adding a file directly to Documents Buckekt and then deleting it.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testAddDeleteFile03() throws Exception {
    // given
    final int statusCode = 200;
    HttpClient http = HttpClient.newHttpClient();
    String key = UUID.randomUUID().toString();

    String createQueue = "createtest-" + UUID.randomUUID();
    String createQueueUrl = createSqsQueue(createQueue).queueUrl();
    String subscriptionCreateArn =
        subscribeToSns(getSnsDocumentsCreateEventTopicArn(), createQueueUrl);

    String deleteQueue = "deletetest-" + UUID.randomUUID();
    String deleteQueueUrl = createSqsQueue(deleteQueue).queueUrl();
    String subscriptionDeleteArn =
        subscribeToSns(getSnsDocumentsDeleteEventTopicArn(), deleteQueueUrl);
    String contentType = "text/plain";
    String content = "test content";

    try {

      try (S3Client s3 = getS3Service().buildClient()) {

        DynamicDocumentItem doc =
            new DynamicDocumentItem(Map.of("documentId", key, "insertedDate", new Date()));
        getDocumentService().saveDocumentItemWithTag(null, doc);

        // when
        URL url = getS3Service().presignPutUrl(getDocumentsbucketname(), key, Duration.ofHours(1));
        HttpResponse<String> put =
            http.send(
                HttpRequest.newBuilder(url.toURI()).header("Content-Type", contentType)
                    .method("PUT", BodyPublishers.ofString(content)).build(),
                BodyHandlers.ofString());

        // then
        assertEquals(statusCode, put.statusCode());
        verifyFileExistsInDocumentsS3(s3, key, contentType);
        assertSnsMessage(createQueueUrl);

        // when
        getS3Service().deleteObject(s3, getDocumentsbucketname(), key);

        // then
        assertSnsMessage(deleteQueueUrl);
      }

    } finally {
      getSnsService().unsubscribe(subscriptionCreateArn);
      getSqsService().deleteQueue(createQueueUrl);

      getSnsService().unsubscribe(subscriptionDeleteArn);
      getSqsService().deleteQueue(deleteQueueUrl);
    }
  }

  /**
   * Test Document Update Lambda Sns.
   */
  @Test
  public void testDocumentUpdateLambdaSns() {
    // given
    // when
    try (S3Client s3 = getS3Service().buildClient()) {
      final GetBucketNotificationConfigurationResponse response0 =
          getS3Service().getNotifications(s3, getDocumentsbucketname());
      final GetBucketNotificationConfigurationResponse response1 =
          getS3Service().getNotifications(s3, getStagingdocumentsbucketname());

      // then
      List<LambdaFunctionConfiguration> list =
          new ArrayList<>(response0.lambdaFunctionConfigurations());
      Collections.sort(list, new LambdaFunctionConfigurationComparator());

      assertEquals(0, response0.queueConfigurations().size());
      assertEquals(2, list.size());

      assertLambdaFunctionConfigurations(list.get(0), "DocumentsS3Update", "s3:ObjectCreated:*");
      assertLambdaFunctionConfigurations(list.get(1), "DocumentsS3Update", "s3:ObjectRemoved:*");

      assertEquals(0, response1.queueConfigurations().size());
      assertEquals(1, response1.lambdaFunctionConfigurations().size());

      assertLambdaFunctionConfigurations(response1.lambdaFunctionConfigurations().get(0),
          "StagingS3Create", "s3:ObjectCreated:*");
    }
  }

  /**
   * Test SSM Parameter Store.
   */
  @Test
  public void testSsmParameters() {
    String appenvironment = getAppenvironment();

    assertEquals("formkiq-core-" + appenvironment + "-documents-622653865277",
        getDocumentsbucketname());
    assertEquals("formkiq-core-" + appenvironment + "-staging-622653865277",
        getStagingdocumentsbucketname());
    assertTrue(getSsmService()
        .getParameterValue("/formkiq/" + appenvironment + "/sns/SnsDocumentsCreateEventTopicArn")
        .contains("SnsDocumentsCreateEventTopic"));
    assertTrue(getSsmService()
        .getParameterValue("/formkiq/" + appenvironment + "/sns/SnsDocumentsUpdateEventTopicArn")
        .contains("SnsDocumentsUpdateEventTopic"));
    assertTrue(getSsmService()
        .getParameterValue("/formkiq/" + appenvironment + "/lambda/StagingCreateObject")
        .contains("StagingS3Create"));
    assertTrue(getSsmService()
        .getParameterValue("/formkiq/" + appenvironment + "/lambda/DocumentsUpdateObject")
        .contains("DocumentsS3Update"));

    String documentsUpdateUrl =
        getSsmService().getParameterValue("/formkiq/" + appenvironment + "/sqs/DocumentsUpdateUrl");
    assertTrue(documentsUpdateUrl.contains("DocumentsUpdateQueue"));
    assertTrue(documentsUpdateUrl.contains("https://"));

    String documentsUpdateArn =
        getSsmService().getParameterValue("/formkiq/" + appenvironment + "/sqs/DocumentsUpdateArn");
    assertTrue(documentsUpdateArn.contains("DocumentsUpdateQueue"));
    assertTrue(documentsUpdateArn.contains("arn:aws:sqs"));
  }

  /**
   * Test S3 Buckets Notifications.
   */
  @Test
  public void testStageDocumentNotifications() {
    // given
    // when
    try (S3Client s3 = getS3Service().buildClient()) {
      final GetBucketNotificationConfigurationResponse response0 =
          getS3Service().getNotifications(s3, getDocumentsbucketname());
      final GetBucketNotificationConfigurationResponse response1 =
          getS3Service().getNotifications(s3, getStagingdocumentsbucketname());

      // then
      List<LambdaFunctionConfiguration> list =
          new ArrayList<>(response0.lambdaFunctionConfigurations());
      Collections.sort(list, new LambdaFunctionConfigurationComparator());

      assertEquals(0, response0.queueConfigurations().size());
      assertEquals(2, list.size());

      assertLambdaFunctionConfigurations(list.get(0), "DocumentsS3Update", "s3:ObjectCreated:*");
      assertLambdaFunctionConfigurations(list.get(1), "DocumentsS3Update", "s3:ObjectRemoved:*");

      assertEquals(0, response1.queueConfigurations().size());
      assertEquals(1, response1.lambdaFunctionConfigurations().size());

      assertLambdaFunctionConfigurations(response1.lambdaFunctionConfigurations().get(0),
          "StagingS3Create", "s3:ObjectCreated:*");
    }
  }

  /**
   * Verify File does NOT exist in Staging S3 Bucket.
   * 
   * @param s3 {@link S3Client}
   * @param key {@link String}
   * @throws InterruptedException InterruptedException
   */
  private void verifyFileNotExistInStagingS3(final S3Client s3, final String key)
      throws InterruptedException {
    while (true) {
      S3ObjectMetadata meta =
          getS3Service().getObjectMetadata(s3, getStagingdocumentsbucketname(), key);

      if (!meta.isObjectExists()) {
        assertFalse(meta.isObjectExists());
        break;
      }
      Thread.sleep(SLEEP);
    }
  }

  /**
   * Write File to Staging S3.
   * 
   * @param s3 {@link S3Client}
   * @param key {@link String}
   * @param contentType {@link String}
   * @return {@link String}
   */
  private String writeToStaging(final S3Client s3, final String key, final String contentType) {
    String data = UUID.randomUUID().toString();

    getS3Service().putObject(s3, getStagingdocumentsbucketname(), key,
        data.getBytes(StandardCharsets.UTF_8), contentType);

    return key;
  }

  /**
   * Write File to Documents S3.
   * 
   * @param s3 {@link S3Client}
   * @param key {@link String}
   * @param contentType {@link String}
   * @return {@link String}
   */
  private String writeToDocuments(final S3Client s3, final String key, final String contentType) {
    String data = UUID.randomUUID().toString();

    getS3Service().putObject(s3, getDocumentsbucketname(), key,
        data.getBytes(StandardCharsets.UTF_8), contentType);

    return key;
  }
}
