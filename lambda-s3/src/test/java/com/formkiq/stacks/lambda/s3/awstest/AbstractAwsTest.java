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
package com.formkiq.stacks.lambda.s3.awstest;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import org.junit.BeforeClass;
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
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Test AWS Tests.
 */
public abstract class AbstractAwsTest {
  /** Sleep Timeout. */
  private static final long SLEEP = 500L;
  /** AWS Region. */
  private static Region awsregion;
  /** App Environment Name. */
  private static String appenvironment;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** {@link S3Service}. */
  private static S3Service s3Service;
  /** {@link SsmService}. */
  private static SsmService ssmService;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** Documents Bucket Name. */
  private static String sesbucketname;
  /** Documents Bucket Name. */
  private static String documentsbucketname;
  /** Documents Bucket Name. */
  private static String stagingdocumentsbucketname;
  /** SNS Document Create Event Topic Arn. */
  private static String snsDocumentsCreateEventTopicArn;
  /** SNS Document Delete Event Topic Arn. */
  private static String snsDocumentsDeleteEventTopicArn;
  /** {@link DocumentService}. */
  private static DocumentService documentService;

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   */
  @BeforeClass
  public static void beforeClass() throws IOException {

    System.setProperty("testregion", "us-east-1");
    System.setProperty("testprofile", "formkiqtest");
    System.setProperty("testappenvironment", "test");

    awsregion = Region.of(System.getProperty("testregion"));
    String awsprofile = System.getProperty("testprofile");
    appenvironment = System.getProperty("testappenvironment");

    final SqsConnectionBuilder sqsConnection =
        new SqsConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);

    final SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);

    final S3ConnectionBuilder s3Builder =
        new S3ConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);

    final SnsConnectionBuilder snsBuilder =
        new SnsConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);

    sqsService = new SqsService(sqsConnection);
    s3Service = new S3Service(s3Builder);
    ssmService = new SsmServiceImpl(ssmBuilder);
    snsService = new SnsService(snsBuilder);

    sesbucketname =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/DocumentsSesS3Bucket");
    documentsbucketname =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/DocumentsS3Bucket");
    stagingdocumentsbucketname =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/DocumentsStageS3Bucket");
    snsDocumentsCreateEventTopicArn = ssmService
        .getParameterValue("/formkiq/" + appenvironment + "/sns/SnsDocumentsCreateEventTopicArn");
    snsDocumentsDeleteEventTopicArn = ssmService
        .getParameterValue("/formkiq/" + appenvironment + "/sns/SnsDocumentsDeleteEventTopicArn");

    String documentsTable =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/DocumentsTableName");
    DynamoDbConnectionBuilder dbConnection =
        new DynamoDbConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);
    documentService = new DocumentServiceImpl(dbConnection, documentsTable);
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
  public static DocumentService getDocumentService() {
    return documentService;
  }

  /**
   * Get S3 Service.
   * 
   * @return {@link S3Service}
   */
  public static S3Service getS3Service() {
    return s3Service;
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
   * Get Documents Create Topic Arn.
   * 
   * @return {@link String}
   */
  public static String getSnsDocumentsCreateEventTopicArn() {
    return snsDocumentsCreateEventTopicArn;
  }

  /**
   * Get Documents Delete Topic Arn.
   * 
   * @return {@link String}
   */
  public static String getSnsDocumentsDeleteEventTopicArn() {
    return snsDocumentsDeleteEventTopicArn;
  }

  /**
   * Get Sns Service.
   * 
   * @return {@link SnsService}
   */
  public static SnsService getSnsService() {
    return snsService;
  }

  /**
   * Get SQS Service.
   * 
   * @return {@link SqsService}
   */
  public static SqsService getSqsService() {
    return sqsService;
  }

  /**
   * Get Ssm Service.
   * 
   * @return {@link SsmService}
   */
  public static SsmService getSsmService() {
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
   * Verify File exists in Documents S3 Bucket.
   * 
   * @param s3 {@link S3Client}
   * @param key {@link String}
   * @param contentType {@link String}
   * @throws InterruptedException InterruptedException
   */
  protected void verifyFileExistsInDocumentsS3(final S3Client s3, final String key,
      final String contentType) throws InterruptedException {

    while (true) {
      S3ObjectMetadata meta = getS3Service().getObjectMetadata(s3, getDocumentsbucketname(), key);
      if (meta.isObjectExists()) {
        assertEquals(contentType, meta.getContentType());
        break;
      }

      Thread.sleep(SLEEP);
    }
  }
}
