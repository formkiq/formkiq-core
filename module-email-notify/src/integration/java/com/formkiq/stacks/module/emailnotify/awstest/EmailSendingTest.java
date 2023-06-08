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
package com.formkiq.stacks.module.emailnotify.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * 
 * Test Sending Emails.
 *
 */
public class EmailSendingTest {
  /** S3 Service. */
  private static S3Service s3Service;
  /** Sleep Timeout. */
  private static final long SLEEP = 500L;
  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsBuilder;
  /** SNS Document Email Topic Arn. */
  private static String snsDocumentEmailArn;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** S3 Staging Bucket. */
  private static String stagingdocumentsbucketname;
  /** Test Timeout. */
  private static final long TIMEOUT = 60000L;

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

    List<Message> receiveMessages = sqsService.receiveMessages(queueUrl).messages();
    while (receiveMessages.size() != 1) {
      Thread.sleep(SLEEP);
      receiveMessages = sqsService.receiveMessages(queueUrl).messages();
    }

    assertEquals(1, receiveMessages.size());
    String body = receiveMessages.get(0).body();

    Gson gson = new GsonBuilder().create();
    Map<String, String> map = gson.fromJson(body, Map.class);
    assertEquals("A document has been created in FormKiQ", map.get("Subject"));
    assertNotNull(map.get("Message"));
  }

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   */
  @BeforeClass
  public static void beforeClass() throws IOException {

    Region awsregion = Region.of(System.getProperty("testregion"));
    String awsprofile = System.getProperty("testprofile");

    final boolean enableAwsXray = false;
    final SqsConnectionBuilder sqsConnection =
        new SqsConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);
    final SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);
    snsBuilder =
        new SnsConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);
    final S3ConnectionBuilder s3Builder =
        new S3ConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);

    s3Service = new S3Service(s3Builder);
    sqsService = new SqsService(sqsConnection);
    SsmService ssmService = new SsmServiceImpl(ssmBuilder);
    snsService = new SnsService(snsBuilder);

    String app = System.getProperty("testappenvironment");
    snsDocumentEmailArn =
        ssmService.getParameterValue("/formkiq/" + app + "/sns/DocumentsEmailNotificationArn");
    stagingdocumentsbucketname =
        ssmService.getParameterValue("/formkiq/" + app + "/s3/DocumentsStageS3Bucket");
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
    return sqsService.createQueue(request);
  }

  /**
   * Subscribe Sqs to Sns.
   * 
   * @param topicArn {@link String}
   * @param queueUrl {@link String}
   * @return {@link String}
   */
  private String subscribeToSns(final String topicArn, final String queueUrl) {
    String queueArn = SqsService.getQueueArn(queueUrl);

    Map<QueueAttributeName, String> attributes = new HashMap<>();
    attributes.put(QueueAttributeName.POLICY, "{\"Version\":\"2012-10-17\",\"Id\":\"Queue_Policy\","
        + "\"Statement\":{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"sqs:SendMessage\","
        + "\"Resource\":\"*\"}}");

    SetQueueAttributesRequest setAttributes =
        SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(attributes).build();
    sqsService.setQueueAttributes(setAttributes);

    String subscriptionArn = snsService.subscribe(topicArn, "sqs", queueArn).subscriptionArn();
    return subscriptionArn;
  }

  /**
   * Test Sending Email.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TIMEOUT)
  @Ignore
  public void testSendingEmail01() throws Exception {
    // given
    String key = UUID.randomUUID().toString();

    String contentType = "text/plain";
    String createQueue = "createtest-" + UUID.randomUUID();
    String documentEmailQueueUrl = createSqsQueue(createQueue).queueUrl();

    String snsDocumentEventArn = subscribeToSns(snsDocumentEmailArn, documentEmailQueueUrl);

    try {

      // when
      writeToStaging(key, contentType);

      // then
      assertSnsMessage(documentEmailQueueUrl, "create");

    } finally {
      snsService.unsubscribe(snsDocumentEventArn);
      sqsService.deleteQueue(documentEmailQueueUrl);
    }
  }

  /**
   * Write File to Staging S3.
   * 
   * @param key {@link String}
   * @param contentType {@link String}
   * @return {@link String}
   */
  private String writeToStaging(final String key, final String contentType) {
    String data = UUID.randomUUID().toString();

    s3Service.putObject(stagingdocumentsbucketname, key, data.getBytes(StandardCharsets.UTF_8),
        contentType);

    return key;
  }
}
