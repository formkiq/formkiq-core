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
package com.formkiq.testutils.aws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3PresignerConnectionBuilder;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceImpl;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * 
 * Singleton for Test Services.
 *
 */
public final class TestServices {

  /** Aws Region. */
  public static final Region AWS_REGION = Region.US_EAST_1;
  /** {@link String}. */
  public static final String BUCKET_NAME = "testbucket";
  /** Default LocalStack Port. */
  private static final int DEFAULT_LOCALSTACK_PORT = 4566;
  /** App Environment. */
  public static final String FORMKIQ_APP_ENVIRONMENT = "test";
  /** {@link LocalStackContainer}. */
  private static LocalStackContainer localstack = null;
  /** LocalStack {@link DockerImageName}. */
  private static final DockerImageName LOCALSTACK_IMAGE =
      DockerImageName.parse("localstack/localstack:3.1.0");
  /** {@link String}. */
  public static final String OCR_BUCKET_NAME = "ocrbucket";
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Connection;
  /** {@link S3PresignerConnectionBuilder}. */
  private static S3PresignerConnectionBuilder s3PresignerConnection;
  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsConnection;
  /** SQS Document Formats Queue. */
  public static final String SQS_DOCUMENT_FORMATS_QUEUE = "documentFormats";
  /** SQS Websockets Queue. */
  public static final String SQS_WEBSOCKET_QUEUE = "websockets";
  /** {@link SqsConnectionBuilder}. */
  private static SqsConnectionBuilder sqsConnection;
  /** SQS Sns Create QueueUrl. */
  private static String sqsDocumentFormatsQueueUrl;
  /** {@link SqsService}. */
  private static SqsService sqsservice = null;
  /** SQS Websocket Queue Url. */
  private static String sqsWebsocketQueueUrl;
  /** {@link SsmConnectionBuilder}. */
  private static SsmConnectionBuilder ssmConnection;
  /** {@link String}. */
  public static final String STAGE_BUCKET_NAME = "stagebucket";

  /**
   * Clear SQS Queue.
   * 
   * @param queueUrl {@link String}
   * @throws URISyntaxException URISyntaxException
   */
  public static void clearSqsQueue(final String queueUrl) throws URISyntaxException {
    SqsService sqsService = new SqsServiceImpl(getSqsConnection(null));
    ReceiveMessageResponse response = sqsService.receiveMessages(queueUrl);
    while (!response.messages().isEmpty()) {
      for (Message msg : response.messages()) {
        sqsService.deleteMessage(queueUrl, msg.receiptHandle());
      }

      response = sqsService.receiveMessages(queueUrl);
    }
  }

  /**
   * Create Random Sns Topic.
   * 
   * @return {@link String}
   * @throws URISyntaxException URISyntaxException
   */
  public static String createSnsTopic() throws URISyntaxException {
    SnsService snsService = new SnsService(getSnsConnection(null));
    return snsService.createTopic("sns_" + UUID.randomUUID()).topicArn();
  }

  /**
   * Create a random SQS queue and subscribes to an SNS Topic.
   * 
   * @param snsTopicArn {@link String}
   * @return {@link String}
   * @throws URISyntaxException URISyntaxException
   */
  public static String createSqsSubscriptionToSnsTopic(final String snsTopicArn)
      throws URISyntaxException {
    SqsService sqsService = new SqsServiceImpl(getSqsConnection(null));
    SnsService snsService = new SnsService(getSnsConnection(null));
    String queueUrl = sqsService.createQueue("sqs_" + UUID.randomUUID()).queueUrl();
    String sqsQueueArn = sqsService.getQueueArn(queueUrl);
    snsService.subscribe(snsTopicArn, "sqs", sqsQueueArn);
    return queueUrl;
  }

  private static String getDefaultLocalStackEndpoint(final Service service) {
    String url = "http://localhost:" + DEFAULT_LOCALSTACK_PORT;
    if (Service.S3.equals(service)) {
      url = "http://s3.localhost:" + DEFAULT_LOCALSTACK_PORT;
    } else if (Service.SQS.equals(service)) {
      url = "http://sqs.localhost:" + DEFAULT_LOCALSTACK_PORT;
    }

    return url;
  }

  /**
   * Get Local Stack Endpoint.
   * 
   * @param service {@link Service}
   * @param endpointOverride {@link URI}
   * @return {@link URI}
   */
  public static URI getEndpoint(final Service service, final URI endpointOverride) {
    URI endpoint = endpointOverride;

    if (endpoint == null) {
      try {
        endpoint = new URI(localstack != null ? localstack.getEndpointOverride(service).toString()
            : getDefaultLocalStackEndpoint(service));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    return endpoint;
  }

  /**
   * Get AWS Services Endpoint Map.
   * 
   * @return {@link Map}
   */
  public static Map<String, URI> getEndpointMap() {
    Map<String, URI> endpoints = Map.of("dynamodb", DynamoDbTestServices.getEndpoint(), "s3",
        TestServices.getEndpoint(Service.S3, null), "s3presigner",
        TestServices.getEndpoint(Service.S3, null), "ssm",
        TestServices.getEndpoint(Service.SSM, null), "sqs",
        TestServices.getEndpoint(Service.SQS, null), "sns",
        TestServices.getEndpoint(Service.SNS, null));
    return endpoints;
  }

  /**
   * Get LocalStack {@link Service}.
   * 
   * @param service {@link Service}
   * @return {@link URI}
   * @throws URISyntaxException URISyntaxException
   */
  public static URI getEndpointOverride(final Service service) throws URISyntaxException {
    return localstack != null ? localstack.getEndpointOverride(service)
        : new URI(getDefaultLocalStackEndpoint(service));
  }

  /**
   * Get Messages from SQS queue.
   * 
   * @param queueUrl {@link String}
   * @return {@link List}
   * @throws URISyntaxException URISyntaxException
   */
  public static List<Message> getMessagesFromSqs(final String queueUrl) throws URISyntaxException {
    SqsService sqsService = new SqsServiceImpl(getSqsConnection(null));
    List<Message> msgs = sqsService.receiveMessages(queueUrl).messages();
    return msgs;
  }

  /**
   * Get Singleton {@link S3ConnectionBuilder}.
   * 
   * @param endpointOverride {@link URI}
   * 
   * @return {@link S3ConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static synchronized S3ConnectionBuilder getS3Connection(final URI endpointOverride)
      throws URISyntaxException {
    if (s3Connection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      s3Connection = new S3ConnectionBuilder(false).setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.S3, endpointOverride));
    }

    return s3Connection;
  }

  /**
   * Get Singleton {@link S3PresignerConnectionBuilder}.
   * 
   * @param endpointOverride {@link URI}
   * 
   * @return {@link S3ConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static synchronized S3PresignerConnectionBuilder getS3PresignerConnection(
      final URI endpointOverride) throws URISyntaxException {
    if (s3PresignerConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      s3PresignerConnection = new S3PresignerConnectionBuilder().setCredentials(cred)
          .setRegion(AWS_REGION).setEndpointOverride(getEndpoint(Service.S3, endpointOverride));
    }

    return s3PresignerConnection;
  }

  /**
   * Get Singleton {@link SnsConnectionBuilder}.
   * 
   * @param endpointOverride {@link URI}
   * 
   * @return {@link SqsConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static synchronized SnsConnectionBuilder getSnsConnection(final URI endpointOverride)
      throws URISyntaxException {
    if (snsConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      snsConnection = new SnsConnectionBuilder(false).setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.SNS, endpointOverride));
    }

    return snsConnection;
  }

  /**
   * Get Singleton {@link SqsConnectionBuilder}.
   * 
   * @param endpointOverride {@link URI}
   * 
   * @return {@link SqsConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static synchronized SqsConnectionBuilder getSqsConnection(final URI endpointOverride)
      throws URISyntaxException {
    if (sqsConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      sqsConnection = new SqsConnectionBuilder(false).setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.SQS, endpointOverride));
    }

    return sqsConnection;
  }

  /**
   * Get Sqs Documents Formats Queue Url.
   * 
   * @param sqs {@link SqsConnectionBuilder}
   * @return {@link String}
   * @throws URISyntaxException URISyntaxException
   */
  public static String getSqsDocumentFormatsQueueUrl(final SqsConnectionBuilder sqs)
      throws URISyntaxException {
    if (sqsDocumentFormatsQueueUrl == null) {

      sqsDocumentFormatsQueueUrl =
          getSqsService(sqs).createQueue(SQS_DOCUMENT_FORMATS_QUEUE).queueUrl();
    }

    return sqsDocumentFormatsQueueUrl;
  }

  /**
   * Get Singleton Instance of {@link SqsService}.
   * 
   * @param sqs {@link SqsConnectionBuilder}
   * @return {@link SqsService}
   * @throws URISyntaxException URISyntaxException
   */
  public static synchronized SqsService getSqsService(final SqsConnectionBuilder sqs)
      throws URISyntaxException {
    if (sqsservice == null) {
      sqsservice = new SqsServiceImpl(sqs);
    }

    return sqsservice;
  }

  /**
   * Get Sqs Documents Formats Queue Url.
   * 
   * @param sqs {@link SqsConnectionBuilder}
   * 
   * @return {@link String}
   * @throws URISyntaxException URISyntaxException
   */
  public static String getSqsWebsocketQueueUrl(final SqsConnectionBuilder sqs)
      throws URISyntaxException {
    if (sqsWebsocketQueueUrl == null) {
      sqsWebsocketQueueUrl = getSqsService(sqs).createQueue(SQS_WEBSOCKET_QUEUE).queueUrl();
    }

    return sqsWebsocketQueueUrl;
  }

  /**
   * Get Singleton {@link SsmConnectionBuilder}.
   * 
   * @param endpointOverride {@link URI}
   * 
   * @return {@link SsmConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static synchronized SsmConnectionBuilder getSsmConnection(final URI endpointOverride)
      throws URISyntaxException {
    if (ssmConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      ssmConnection = new SsmConnectionBuilder(false).setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.SSM, endpointOverride));
    }

    return ssmConnection;
  }

  /**
   * Checks Whether LocalStack is currently running on the default port.
   * 
   * @return boolean
   */
  private static boolean isPortAvailable() {

    boolean available = false;
    final int status200 = 200;

    HttpClient client = HttpClient.newHttpClient();
    try {
      HttpRequest request = HttpRequest.newBuilder().timeout(Duration.ofSeconds(2))
          .uri(new URI("http://localhost:" + DEFAULT_LOCALSTACK_PORT + "/_localstack/health")).GET()
          .build();
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      available = response.statusCode() != status200;
    } catch (URISyntaxException | IOException | InterruptedException e) {
      available = true;
    }

    return available;
  }

  /**
   * Start LocalStack.
   */
  @SuppressWarnings("resource")
  public static synchronized void startLocalStack() {

    if (localstack == null && isPortAvailable()) {
      localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3, Service.SQS,
          Service.SSM, Service.SNS);
      localstack.start();
    }
  }

  /**
   * Stop LocalStack.
   */
  public static void stopLocalStack() {
    if (localstack != null) {
      localstack.stop();
    }
  }

  /**
   * Get Messages from SQS queue.
   * 
   * @param queueUrl {@link String}
   * @return {@link List}
   * @throws URISyntaxException URISyntaxException
   */
  public static List<Message> waitForMessagesFromSqs(final String queueUrl)
      throws URISyntaxException {

    SqsService sqsService = new SqsServiceImpl(getSqsConnection(null));
    List<Message> msgs = Collections.emptyList();

    while (msgs.isEmpty()) {
      msgs = sqsService.receiveMessages(queueUrl).messages();
      if (msgs.isEmpty()) {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    return msgs;
  }

  private TestServices() {}
}
