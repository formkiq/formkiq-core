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
package com.formkiq.stacks.dynamodb.awstest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;

/**
 * Test CloudFormation.
 */
public class AwsResourceTest {

  /** App Environment Name. */
  private static String appenvironment;
  /** AWS Region. */
  private static Region awsregion;
  /** {@link DynamoDbClient}. */
  private static DynamoDbClient dynamoDB;
  /** {@link SsmService}. */
  private static SsmService ssmService;

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   */
  @BeforeClass
  public static void beforeClass() throws IOException {

    String awsprofile = System.getProperty("testprofile");
    appenvironment = System.getProperty("testappenvironment");
    awsregion = Region.of(System.getProperty("testregion"));

    final SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder(false).setCredentials(awsprofile).setRegion(awsregion);
    ssmService = new SsmServiceImpl(ssmBuilder);

    dynamoDB = new DynamoDbConnectionBuilder(false).setCredentials(awsprofile).setRegion(awsregion)
        .build();
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
            .contains("-documents"));
    assertTrue(
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/dynamodb/CacheTableName")
            .contains("-cache"));
  }
}
