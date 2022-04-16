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
package com.formkiq.stacks.dynamodb;

import java.io.IOException;
import java.net.URISyntaxException;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
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
  /** DynamoDB Local {@link GenericContainer}. */
  private static GenericContainer<?> dynamoDbLocal;
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbConnection;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;

  /**
   * Get Singleton Instance of {@link DynamoDbConnectionBuilder}.
   * @param dynamoDb {@link GenericContainer}
   * @return {@link DynamoDbConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public static DynamoDbConnectionBuilder getDynamoDbConnection(final GenericContainer<?> dynamoDb)
      throws URISyntaxException {
    if (dbConnection == null) {
      AwsCredentialsProvider cred = StaticCredentialsProvider
          .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));
      
      dbConnection = new DynamoDbConnectionBuilder().setRegion(AWS_REGION).setCredentials(cred)
          .setEndpointOverride("http://localhost:" + dynamoDb.getFirstMappedPort());
    }

    return dbConnection;
  }
  
  /**
   * Get Singleton Instance of {@link DynamoDbHelper}.
   * @param dynamoDb {@link GenericContainer}
   * @return {@link DynamoDbConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   * @throws IOException IOException
   */
  public static DynamoDbHelper getDynamoDbHelper(final GenericContainer<?> dynamoDb)
      throws URISyntaxException, IOException {
    if (dbHelper == null) {      
      dbHelper = new DynamoDbHelper(getDynamoDbConnection(dynamoDb));
    }

    return dbHelper;
  }
  
  /**
   * Get Singleton Instance of {@link GenericContainer}.
   * @return {@link GenericContainer}
   */
  @SuppressWarnings("resource")
  public static GenericContainer<?> getDynamoDbLocal() {
    if (dynamoDbLocal == null) {
      final Integer exposedPort = Integer.valueOf(8000);
      dynamoDbLocal = new GenericContainer<>("amazon/dynamodb-local:1.13.5")
        .withExposedPorts(exposedPort);
    }
    
    return dynamoDbLocal;
  }

  private DynamoDbTestServices() {
  }
}
