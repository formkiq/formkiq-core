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
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

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
  /** Default Localstack Endpoint. */
  private static final String LOCALSTACK_ENDPOINT = "http://localhost:" + DEFAULT_LOCALSTACK_PORT;
  /** LocalStack {@link DockerImageName}. */
  private static final DockerImageName LOCALSTACK_IMAGE =
      DockerImageName.parse("localstack/localstack:0.12.2");
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Connection;
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
            : LOCALSTACK_ENDPOINT);
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
        : new URI(LOCALSTACK_ENDPOINT);
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
      sqsservice = new SqsService(sqs);
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
    boolean available;
    try (ServerSocket ignored = new ServerSocket(DEFAULT_LOCALSTACK_PORT)) {
      available = true;
    } catch (IOException e) {
      available = false;
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

  private TestServices() {}
}
