/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.lambda.s3.awstest;

import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.PaginationResults;
import com.formkiq.stacks.dynamodb.SearchQuery;
import com.formkiq.stacks.dynamodb.SearchTagCriteria;
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
   * @param type {@link String}
   * @throws InterruptedException InterruptedException
   */
  @SuppressWarnings("unchecked")
  private static void assertSnsMessage(final String queueUrl, final String type)
      throws InterruptedException {

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
    
    if (type.equals(map.get("type"))) {
    
      if (!"delete".equals(type)) {
        assertNotNull(map.get("userId"));
      }
      
    } else {
      assertSnsMessage(queueUrl, type);
    }
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

    String contentType = "text/plain";
    String createQueue = "createtest-" + UUID.randomUUID();
    String documentEventQueueUrl = createSqsQueue(createQueue).queueUrl();
    String snsDocumentEventArn = subscribeToSns(getSnsDocumentEventArn(), documentEventQueueUrl);

    try {

      try (S3Client s3 = getS3Service().buildClient()) {

        // when
        key = writeToStaging(s3, key, contentType);

        // then
        verifyFileExistsInDocumentsS3(s3, key, contentType);
        verifyFileNotExistInStagingS3(s3, key);
        assertSnsMessage(documentEventQueueUrl, "create");

        // when
        key = writeToStaging(s3, key, contentType);

        // then
        verifyFileExistsInDocumentsS3(s3, key, contentType);
        verifyFileNotExistInStagingS3(s3, key);
        assertSnsMessage(documentEventQueueUrl, "create");

        // when
        getS3Service().deleteObject(s3, getDocumentsbucketname(), key);

        // then
        assertSnsMessage(documentEventQueueUrl, "delete");
      }

    } finally {
      getSnsService().unsubscribe(snsDocumentEventArn);
      getSqsService().deleteQueue(documentEventQueueUrl);
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
    String documentQueueUrl = createSqsQueue(createQueue).queueUrl();
    String subscriptionDocumentArn =
        subscribeToSns(getSnsDocumentEventArn(), documentQueueUrl);

    String contentType = "text/plain";
    String content = "test content";

    try {

      try (S3Client s3 = getS3Service().buildClient()) {

        DynamicDocumentItem doc = new DynamicDocumentItem(
            Map.of("documentId", key, "insertedDate", new Date(), "userId", "joe"));
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
        assertSnsMessage(documentQueueUrl, "create");

        // when
        getS3Service().deleteObject(s3, getDocumentsbucketname(), key);

        // then
        assertSnsMessage(documentQueueUrl, "delete");
      }

    } finally {
      getSnsService().unsubscribe(subscriptionDocumentArn);
      getSqsService().deleteQueue(documentQueueUrl);
    }
  }

  /**
   * Test Updating a file directly to Staging bucket.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testUpdateStagingFile01() throws Exception {
    // given
    final String siteId = DEFAULT_SITE_ID;
    final String txt1 = "this is a test";
    final String txt2 = "this is a another test";

    String createQueue = "createtest-" + UUID.randomUUID();
    String documentQueueUrl = createSqsQueue(createQueue).queueUrl();
    String subDocumentArn = subscribeToSns(getSnsDocumentEventArn(), documentQueueUrl);

    String contentType = "text/plain";
    String path = "user/home/test.txt";

    try {

      // when
      try (S3Client s3 = getS3Service().buildClient()) {
        
        getS3Service().putObject(s3, getStagingdocumentsbucketname(), siteId + "/" + path,
            txt1.getBytes(StandardCharsets.UTF_8), contentType);
        
        SearchQuery q = new SearchQuery().tag(new SearchTagCriteria().key("path").eq(path));
        
        // then
        PaginationResults<DynamicDocumentItem> result =
            new PaginationResults<>(Collections.emptyList(), null);
        
        while (result.getResults().size() != 1) {
          result = getSearchService().search(siteId, q, null, DocumentService.MAX_RESULTS);
          Thread.sleep(SLEEP);
        }
        
        assertEquals(1, result.getResults().size());
        assertSnsMessage(documentQueueUrl, "create");
        
        // given
        String documentId = result.getResults().get(0).getDocumentId();
        Collection<DocumentTag> tags =
            Arrays.asList(new DocumentTag(documentId, "status", "active", new Date(), "testuser"));
        getDocumentService().addTags(siteId, documentId, tags, null);
        
        // when
        getS3Service().putObject(s3, getStagingdocumentsbucketname(), siteId + "/" + path,
            txt2.getBytes(StandardCharsets.UTF_8), contentType);
        
        // then
        while (true) {
          String txt =
              getS3Service().getContentAsString(s3, getDocumentsbucketname(), documentId, null);
          if (txt.equals(txt2)) {
            break;
          }
        }
        
        assertEquals(txt2,
            getS3Service().getContentAsString(s3, getDocumentsbucketname(), documentId, null));
        assertSnsMessage(documentQueueUrl, "create");
        PaginationResults<DocumentTag> list =
            getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
        assertEquals("[path, status, untagged, userId]", list.getResults().stream()
            .map(m -> m.getKey()).collect(Collectors.toList()).toString());
      }

    } finally {
      getSnsService().unsubscribe(subDocumentArn);
      getSqsService().deleteQueue(documentQueueUrl);
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

    assertTrue(
        getDocumentsbucketname().startsWith("formkiq-core-" + appenvironment + "-documents-"));
    assertTrue(
        getStagingdocumentsbucketname().startsWith("formkiq-core-" + appenvironment + "-staging-"));
    assertTrue(getSsmService()
        .getParameterValue("/formkiq/" + appenvironment + "/sns/DocumentEventArn")
        .contains("SnsDocumentEvent"));
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
