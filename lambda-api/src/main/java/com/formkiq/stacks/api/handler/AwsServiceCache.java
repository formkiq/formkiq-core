/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.handler;

import java.util.concurrent.TimeUnit;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentCountServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DynamoDbCacheService;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;

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
  /** {@link String}. */
  private String dbDocumentsTable;
  /** {@link String}. */
  private String dbCacheTable;
  /** {@link DocumentService}. */
  private DocumentService documentService;
  /** {@link DocumentSearchService}. */
  private DocumentSearchService documentSearchService;
  /** {@link SsmService}. */
  private SsmService ssmService;
  /** {@link SqsService}. */
  private SqsService sqsService;
  /** {@link DynamoDbCacheService}. */
  private DynamoDbCacheService documentCacheService;
  /** {@link DocumentCountService}. */
  private DocumentCountService documentCountService;
  /** S3 Staging Bucket. */
  private String stages3bucket;
  /** App Environment. */
  private String appEnvironment;
  /** Is Debug Mode. */
  private boolean debug;
  /** Documents S3 Bucket. */
  private String documentsS3bucket;

  /**
   * constructor.
   */
  public AwsServiceCache() {

  }

  /**
   * Get App Environment.
   * 
   * @return {@link String}
   */
  public String appEnvironment() {
    return this.appEnvironment;
  }

  /**
   * Set App Environment.
   * 
   * @param env {@link String}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache appEnvironment(final String env) {
    this.appEnvironment = env;
    return this;
  }

  /**
   * Set {@link DynamoDbConnectionBuilder}.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   * @param cacheTable {@link String}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache dbConnection(final DynamoDbConnectionBuilder connection,
      final String documentsTable, final String cacheTable) {
    this.dbConnection = connection;
    this.dbDocumentsTable = documentsTable;
    this.dbCacheTable = cacheTable;
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
      this.documentCacheService = new DynamoDbCacheService(this.dbConnection, this.dbCacheTable);
    }
    return this.documentCacheService;
  }

  /**
   * Get {@link DocumentCountService}.
   * 
   * @return {@link DocumentCountService}
   */
  public DocumentCountService documentCountService() {
    if (this.documentCountService == null) {
      this.documentCountService =
          new DocumentCountServiceDynamoDb(this.dbConnection, this.dbDocumentsTable);
    }
    return this.documentCountService;
  }

  /**
   * Get {@link DocumentService}.
   * 
   * @return {@link DocumentService}
   */
  public DocumentService documentService() {
    if (this.documentService == null) {
      this.documentService = new DocumentServiceImpl(this.dbConnection, this.dbDocumentsTable);
    }
    return this.documentService;
  }

  /**
   * Get {@link DocumentSearchService}.
   * 
   * @return {@link DocumentSearchService}
   */
  public DocumentSearchService documentSearchService() {
    if (this.documentSearchService == null) {
      this.documentSearchService = new DocumentSearchServiceImpl(documentService(),
          this.dbConnection, this.dbDocumentsTable);
    }
    return this.documentSearchService;
  }

  /**
   * Build DynamoDbClient/s3 during lambda Initialization stage not Invocation stage.
   */
  public void init() {
    this.dbConnection.initDbClient();
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

  /**
   * Get Staging S3 Bucket.
   * 
   * @return {@link String}
   */
  public String stages3bucket() {
    return this.stages3bucket;
  }

  /**
   * Set Staging S3 Bucket.
   * 
   * @param s3bucket {@link String}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache stages3bucket(final String s3bucket) {
    this.stages3bucket = s3bucket;
    return this;
  }

  /**
   * Get Documents S3 Bucket.
   * 
   * @return {@link String}
   */
  public String documents3bucket() {
    return this.documentsS3bucket;
  }

  /**
   * Set Documents S3 Bucket.
   * 
   * @param s3bucket {@link String}
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache documents3bucket(final String s3bucket) {
    this.documentsS3bucket = s3bucket;
    return this;
  }
}
