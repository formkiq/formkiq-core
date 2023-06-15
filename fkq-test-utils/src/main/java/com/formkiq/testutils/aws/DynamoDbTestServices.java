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
import org.testcontainers.containers.GenericContainer;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * Singleton for Test Services.
 *
 */
public final class DynamoDbTestServices {

  /** Aws Region. */
  private static final Region AWS_REGION = Region.US_EAST_1;
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbConnection;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;
  /** Default DynamoDB Port. */
  private static final int DEFAULT_PORT = 8000;
  /** DynamoDB Local {@link GenericContainer}. */
  private static GenericContainer<?> dynamoDbLocal;

  /**
   * Get Singleton Instance of {@link DynamoDbConnectionBuilder}.
   * 
   * @return {@link DynamoDbConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static DynamoDbConnectionBuilder getDynamoDbConnection() throws URISyntaxException {
    if (dbConnection == null) {
      AwsCredentialsProvider cred =
          StaticCredentialsProvider.create(AwsBasicCredentials.create("ACCESSKEY", "SECRETKEY"));

      dbConnection = new DynamoDbConnectionBuilder(false).setRegion(AWS_REGION).setCredentials(cred)
          .setEndpointOverride(getEndpoint());
    }

    return dbConnection;
  }

  /**
   * Get Singleton Instance of {@link DynamoDbHelper}.
   * 
   * @param dynamoDb {@link GenericContainer}
   * @return {@link DynamoDbConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   * @throws IOException IOException
   */
  public static DynamoDbHelper getDynamoDbHelper(final GenericContainer<?> dynamoDb)
      throws URISyntaxException, IOException {
    if (dbHelper == null) {
      dbHelper = new DynamoDbHelper(getDynamoDbConnection());
    }

    return dbHelper;
  }

  /**
   * Get Singleton Instance of {@link GenericContainer}.
   * 
   * @return {@link GenericContainer}
   */
  @SuppressWarnings("resource")
  public static GenericContainer<?> getDynamoDbLocal() {
    if (dynamoDbLocal == null && isPortAvailable()) {
      final Integer exposedPort = Integer.valueOf(DEFAULT_PORT);
      dynamoDbLocal = new GenericContainer<>("amazon/dynamodb-local:1.21.0")
          .withExposedPorts(exposedPort).withCommand("-jar DynamoDBLocal.jar -sharedDb");
    }

    return dynamoDbLocal;
  }

  /**
   * Get Endpoint.
   * 
   * @return {@link URI}
   */
  @SuppressWarnings("resource")
  public static URI getEndpoint() {
    GenericContainer<?> dynamoDb = getDynamoDbLocal();
    Integer port = dynamoDb != null ? dynamoDb.getFirstMappedPort() : Integer.valueOf(DEFAULT_PORT);
    try {
      return new URI("http://localhost:" + port);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Checks Whether LocalStack is currently running on the default port.
   * 
   * @return boolean
   */
  private static boolean isPortAvailable() {
    boolean available;
    try (ServerSocket ignored = new ServerSocket(DEFAULT_PORT)) {
      available = true;
    } catch (IOException e) {
      available = false;
    }
    return available;
  }

  private DynamoDbTestServices() {}
}
