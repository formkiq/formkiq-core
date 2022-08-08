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

import java.net.URISyntaxException;
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
  /** App Environment. */
  public static final String FORMKIQ_APP_ENVIRONMENT = "test";
  /** {@link LocalStackContainer}. */
  private static LocalStackContainer localstack = null;
  /** Default Localstack Endpoint. */
  private static final String LOCALSTACK_ENDPOINT = "http://localhost:4566";
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
   * @param endpointOverride {@link String}
   * @return {@link String}
   */
  @SuppressWarnings("resource")
  private static String getEndpoint(final Service service, final String endpointOverride) {
    String endpoint = endpointOverride;

    if (endpoint == null) {
      endpoint = localstack != null ? getLocalStack().getEndpointOverride(service).toString()
          : LOCALSTACK_ENDPOINT;
    }

    return endpoint;
  }

  /**
   * Get Singleton Instance of {@link LocalStackContainer}.
   * 
   * @return {@link LocalStackContainer}
   */
  @SuppressWarnings("resource")
  public static LocalStackContainer getLocalStack() {
    if (localstack == null) {
      localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3, Service.SQS,
          Service.SSM, Service.SNS);
    }

    return localstack;
  }

  /**
   * Get Singleton {@link S3ConnectionBuilder}.
   * 
   * @param endpointOverride {@link String}
   * 
   * @return {@link S3ConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static S3ConnectionBuilder getS3Connection(final String endpointOverride)
      throws URISyntaxException {
    if (s3Connection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      s3Connection = new S3ConnectionBuilder().setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.S3, endpointOverride));
    }

    return s3Connection;
  }

  /**
   * Get Singleton {@link SnsConnectionBuilder}.
   * 
   * @param endpointOverride {@link String}
   * 
   * @return {@link SqsConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static SnsConnectionBuilder getSnsConnection(final String endpointOverride)
      throws URISyntaxException {
    if (snsConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      snsConnection = new SnsConnectionBuilder().setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.SNS, endpointOverride));
    }

    return snsConnection;
  }

  /**
   * Get Singleton {@link SqsConnectionBuilder}.
   * 
   * @param endpointOverride {@link String}
   * 
   * @return {@link SqsConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static SqsConnectionBuilder getSqsConnection(final String endpointOverride)
      throws URISyntaxException {
    if (sqsConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      sqsConnection = new SqsConnectionBuilder().setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.SQS, endpointOverride));
    }

    return sqsConnection;
  }

  /**
   * Get Sqs Documents Formats Queue Url.
   * 
   * @param endpointOverride {@link String}
   * 
   * @return {@link String}
   * @throws URISyntaxException URISyntaxException
   */
  public static String getSqsDocumentFormatsQueueUrl(final String endpointOverride)
      throws URISyntaxException {
    if (sqsDocumentFormatsQueueUrl == null) {
      sqsDocumentFormatsQueueUrl =
          getSqsService(endpointOverride).createQueue(SQS_DOCUMENT_FORMATS_QUEUE).queueUrl();
    }

    return sqsDocumentFormatsQueueUrl;
  }

  /**
   * Get Singleton Instance of {@link SqsService}.
   * 
   * @param endpointOverride {@link String}
   * 
   * @return {@link SqsService}
   * @throws URISyntaxException URISyntaxException
   */
  public static SqsService getSqsService(final String endpointOverride) throws URISyntaxException {
    if (sqsservice == null) {
      sqsservice = new SqsService(getSqsConnection(endpointOverride));
    }

    return sqsservice;
  }

  /**
   * Get Sqs Documents Formats Queue Url.
   * 
   * @param endpointOverride {@link String}
   * 
   * @return {@link String}
   * @throws URISyntaxException URISyntaxException
   */
  public static String getSqsWebsocketQueueUrl(final String endpointOverride)
      throws URISyntaxException {
    if (sqsWebsocketQueueUrl == null) {
      sqsWebsocketQueueUrl =
          getSqsService(endpointOverride).createQueue(SQS_WEBSOCKET_QUEUE).queueUrl();
    }

    return sqsWebsocketQueueUrl;
  }

  /**
   * Get Singleton {@link SsmConnectionBuilder}.
   * 
   * @param endpointOverride {@link String}
   * 
   * @return {@link SsmConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static SsmConnectionBuilder getSsmConnection(final String endpointOverride)
      throws URISyntaxException {
    if (ssmConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

      ssmConnection = new SsmConnectionBuilder().setCredentials(cred).setRegion(AWS_REGION)
          .setEndpointOverride(getEndpoint(Service.SSM, endpointOverride));
    }

    return ssmConnection;
  }

  private TestServices() {}
}
