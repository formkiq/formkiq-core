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
package com.formkiq.aws.services.lambda;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.services.DynamoDbCacheService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;

/**
 * Get Aws Services from Cache.
 *
 */
public class AwsServiceCache {

  /** The number of minutes to hold in cache. */
  private static final int CACHE_MINUTES = 15;
  /** {@link DynamoDbConnectionBuilder}. */
  private DynamoDbConnectionBuilder dbConnection;
  /** {@link S3ConnectionBuilder}. */
  private S3ConnectionBuilder s3Connection;
  /** {@link SsmConnectionBuilder}. */
  private SsmConnectionBuilder ssmConnection;
  /** {@link SqsConnectionBuilder}. */
  private SqsConnectionBuilder sqsConnection;
  /** {@link S3Service}. */
  private S3Service s3Service;
  /** {@link SsmService}. */
  private SsmService ssmService;
  /** {@link SqsService}. */
  private SqsService sqsService;
  /** Is Debug Mode. */
  private boolean debug;
  /** Environment {@link Map}. */
  private Map<String, String> environment;
  /** {@link DynamoDbCacheService}. */
  private DynamoDbCacheService documentCacheService;

  /**
   * constructor.
   */
  public AwsServiceCache() {
  }
  
  /**
   * Get {@link DynamoDbConnectionBuilder}.
   * @return {@link DynamoDbConnectionBuilder}
   */
  public DynamoDbConnectionBuilder dbConnection() {
    return this.dbConnection;
  }

  /**
   * Set {@link DynamoDbConnectionBuilder}.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache dbConnection(final DynamoDbConnectionBuilder connection) {
    this.dbConnection = connection;
    return this;
  }

  /**
   * Is Debug.
   * 
   * @return boolean
   */
  public boolean debug() {
    return this.debug;
  }
  
  /**
   * Set Debug Mode.
   * 
   * @param isDebug boolean
   * @return {@link AwsServiceCache}s
   */
  public AwsServiceCache debug(final boolean isDebug) {
    this.debug = isDebug;
    return this;
  }

  /**
   * Get {@link DynamoDbCacheService}.
   * 
   * @return {@link DynamoDbCacheService}
   */
  public DynamoDbCacheService documentCacheService() {
    if (this.documentCacheService == null) {
      this.documentCacheService =
          new DynamoDbCacheService(dbConnection(), environment("CACHE_TABLE"));
    }
    return this.documentCacheService;
  }

  /**
   * Set Environment {@link Map} parameters.
   * @param map {@link Map}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache environment(final Map<String, String> map) {
    this.environment = map;
    return this;
  }

  /**
   * Get Environment {@link Map} parameters.
   * @param key {@link String}
   * @return {@link String}
   */
  public String environment(final String key) {
    return this.environment.get(key);
  }

  /**
   * Build DynamoDbClient/s3 during lambda Initialization stage not Invocation stage.
   */
  public void init() {
    this.dbConnection.initDbClient();
  }

  /**
   * Get {@link S3ConnectionBuilder}.
   * @return {@link S3ConnectionBuilder}
   */
  public S3ConnectionBuilder s3Connection() {
    return this.s3Connection;
  }

  /**
   * Set {@link S3ConnectionBuilder}.
   * 
   * @param connection {@link S3ConnectionBuilder}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache s3Connection(final S3ConnectionBuilder connection) {
    this.s3Connection = connection;
    return this;
  }

  /**
   * Get {@link S3Service}.
   * 
   * @return {@link S3Service}
   */
  public S3Service s3Service() {
    if (this.s3Service == null) {
      this.s3Service = new S3Service(this.s3Connection);
    }
    return this.s3Service;
  }

  /**
   * Get {@link SqsConnectionBuilder}.
   * @return {@link SqsConnectionBuilder}
   */
  public SqsConnectionBuilder sqsConnection() {
    return this.sqsConnection;
  }

  /**
   * Set {@link SqsConnectionBuilder}.
   * 
   * @param connection {@link SqsConnectionBuilder}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache sqsConnection(final SqsConnectionBuilder connection) {
    this.sqsConnection = connection;
    return this;
  }

  /**
   * Get {@link SqsService}.
   * 
   * @return {@link SqsService}
   */
  public SqsService sqsService() {
    if (this.sqsService == null) {
      this.sqsService = new SqsService(this.sqsConnection);
    }
    return this.sqsService;
  }

  /**
   * Get {@link SsmConnectionBuilder}.
   * @return {@link SsmConnectionBuilder}
   */
  public SsmConnectionBuilder ssmConnection() {
    return this.ssmConnection;
  }
  
  /**
   * Set {@link SsmConnectionBuilder}.
   * 
   * @param connection {@link SsmConnectionBuilder}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache ssmConnection(final SsmConnectionBuilder connection) {
    this.ssmConnection = connection;
    return this;
  }

  /**
   * Get {@link SsmService}.
   * 
   * @return {@link SsmService}
   */
  public SsmService ssmService() {
    if (this.ssmService == null) {
      this.ssmService = new SsmServiceCache(this.ssmConnection, CACHE_MINUTES, TimeUnit.MINUTES);
    }
    return this.ssmService;
  }
}