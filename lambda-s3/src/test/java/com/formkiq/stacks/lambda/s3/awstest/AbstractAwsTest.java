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
import java.io.IOException;
import org.junit.BeforeClass;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * Test AWS Tests.
 */
public abstract class AbstractAwsTest {
  /** App Environment Name. */
  private static String appenvironment;
  /** AWS Region. */
  private static Region awsregion;
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbConnection;
  /** Documents Bucket Name. */
  private static String documentsbucketname;
  /** {@link DocumentService}. */
  private static DocumentService documentService;
  /** App Edition Name. */
  private static String edition;
  /** {@link S3Service}. */
  private static S3Service s3Service;
  /** {@link DocumentSearchService}. */
  private static DocumentSearchService searchService;
  /** Documents Bucket Name. */
  private static String sesbucketname;
  /** Sleep Timeout. */
  private static final long SLEEP = 500L;
  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsBuilder;
  /** SNS Document Event Topic Arn. */
  private static String snsDocumentEventArn;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** {@link SqsConnectionBuilder}. */
  private static SqsConnectionBuilder sqsBuilder;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** {@link SsmConnectionBuilder}. */
  private static SsmConnectionBuilder ssmBuilder;
  /** {@link SsmService}. */
  private static SsmService ssmService;
  /** Documents Bucket Name. */
  private static String stagingdocumentsbucketname;

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   */
  @BeforeClass
  public static void beforeClass() throws IOException {

    awsregion = Region.of(System.getProperty("testregion"));
    String awsprofile = System.getProperty("testprofile");
    appenvironment = System.getProperty("testappenvironment");

    boolean enableAwsXray = false;
    sqsBuilder =
        new SqsConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);

    ssmBuilder =
        new SsmConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);

    final S3ConnectionBuilder s3Builder =
        new S3ConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);

    snsBuilder =
        new SnsConnectionBuilder(enableAwsXray).setCredentials(awsprofile).setRegion(awsregion);

    sqsService = new SqsService(sqsBuilder);
    s3Service = new S3Service(s3Builder);
    ssmService = new SsmServiceImpl(ssmBuilder);
    snsService = new SnsService(snsBuilder);

    edition = ssmService.getParameterValue("/formkiq/" + appenvironment + "/edition");
    sesbucketname =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/DocumentsSesS3Bucket");
    documentsbucketname =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/DocumentsS3Bucket");
    stagingdocumentsbucketname =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/DocumentsStageS3Bucket");
    snsDocumentEventArn =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/sns/DocumentEventArn");

    String documentsTable =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/DocumentsTableName");

    dbConnection =
        new DynamoDbConnectionBuilder(false).setCredentials(awsprofile).setRegion(awsregion);
    documentService = new DocumentServiceImpl(dbConnection, documentsTable,
        new DocumentVersionServiceNoVersioning());
    searchService =
        new DocumentSearchServiceImpl(dbConnection, documentService, documentsTable, null);
  }

  /**
   * Get App Environment.
   * 
   * @return {@link String}
   */
  public static String getAppenvironment() {
    return appenvironment;
  }

  /**
   * Get AWS Region.
   * 
   * @return {@link Region}
   */
  public static Region getAwsregion() {
    return awsregion;
  }

  /**
   * Get Documents Bucket.
   * 
   * @return {@link String}
   */
  public static String getDocumentsbucketname() {
    return documentsbucketname;
  }

  /**
   * Get {@link DocumentService}.
   * 
   * @return {@link DocumentService}
   */
  protected static DocumentService getDocumentService() {
    return documentService;
  }

  /**
   * Get App Edition.
   * 
   * @return {@link String}
   */
  public static String getEdition() {
    return edition;
  }

  /**
   * Get S3 Service.
   * 
   * @return {@link S3Service}
   */
  protected static S3Service getS3Service() {
    return s3Service;
  }

  /**
   * Get {@link DocumentSearchService}.
   * 
   * @return {@link DocumentSearchService}
   */
  public static DocumentSearchService getSearchService() {
    return searchService;
  }

  /**
   * Get Ses S3 Bucket.
   * 
   * @return {@link String}
   */
  public static String getSesbucketname() {
    return sesbucketname;
  }

  /**
   * Get {@link SnsClient}.
   * 
   * @return {@link SnsClient}
   */
  public static SnsClient getSnsClient() {
    return snsBuilder.build();
  }

  /**
   * Get Documents Create Topic Arn.
   * 
   * @return {@link String}
   */
  public static String getSnsDocumentEventArn() {
    return snsDocumentEventArn;
  }

  /**
   * Get Sns Service.
   * 
   * @return {@link SnsService}
   */
  protected static SnsService getSnsService() {
    return snsService;
  }

  /**
   * Get {@link SqsClient}.
   * 
   * @return {@link SqsClient}
   */
  public static SqsClient getSqsClient() {
    return sqsBuilder.build();
  }

  /**
   * Get SQS Service.
   * 
   * @return {@link SqsService}
   */
  protected static SqsService getSqsService() {
    return sqsService;
  }

  /**
   * Get {@link SsmClient}.
   * 
   * @return {@link SsmClient}
   */
  public static SsmClient getSsmClient() {
    return ssmBuilder.build();
  }

  /**
   * Get Ssm Service.
   * 
   * @return {@link SsmService}
   */
  protected static SsmService getSsmService() {
    return ssmService;
  }

  /**
   * Get Staging Bucket.
   * 
   * @return {@link String}
   */
  public static String getStagingdocumentsbucketname() {
    return stagingdocumentsbucketname;
  }

  /**
   * Get {@link DynamoDbClient}.
   * 
   * @return {@link DynamoDbClient}
   */
  public DynamoDbClient getDynamodbClient() {
    return dbConnection.build();
  }

  /**
   * Verify File exists in Documents S3 Bucket.
   * 
   * @param key {@link String}
   * @param contentType {@link String}
   * @throws InterruptedException InterruptedException
   */
  protected void verifyFileExistsInDocumentsS3(final String key, final String contentType)
      throws InterruptedException {

    while (true) {
      S3ObjectMetadata meta = getS3Service().getObjectMetadata(getDocumentsbucketname(), key, null);
      if (meta.isObjectExists()) {
        assertEquals(contentType, meta.getContentType());
        break;
      }

      Thread.sleep(SLEEP);
    }
  }
}
