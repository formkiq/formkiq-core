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
package com.formkiq.stacks.dynamodb.awstest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;

/**
 * Test CloudFormation.
 */
public class AwsResourceTest {

  /** AWS Region. */
  private static Region awsregion = Region.US_EAST_1;
  /** App Environment Name. */
  private static String appenvironment;
  /** {@link SsmService}. */
  private static SsmService ssmService;
  /** {@link DynamoDbClient}. */
  private static DynamoDbClient dynamoDB;

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   */
  @BeforeClass
  public static void beforeClass() throws IOException {

    String awsprofile = System.getProperty("testprofile");
    appenvironment = System.getProperty("testappenvironment");

    final SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);
    ssmService = new SsmServiceImpl(ssmBuilder);

    dynamoDB =
        new DynamoDbConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion).build();
  }

  /**
   * Test DynamoDB Tables.
   */
  @Test
  public void testDynamoDbTables() {
    final String documentTable =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/DocumentsTableName");
    final String cacheTable =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/CacheTableName");

    DescribeTableRequest req = DescribeTableRequest.builder().tableName(documentTable).build();
    DescribeTableResponse resp = dynamoDB.describeTable(req);
    assertNotNull(resp.table().latestStreamArn());

    req = DescribeTableRequest.builder().tableName(cacheTable).build();
    assertNotNull(dynamoDB.describeTable(req).table());
  }

  /**
   * Test SSM Parameter Store.
   */
  @Test
  public void testSsmParameters() {
    assertTrue(
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/DocumentsTableName")
            .contains("-Documents-"));
    assertTrue(
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/CacheTableName")
            .contains("-Cache-"));
  }
}
