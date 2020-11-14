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
package com.formkiq.aws.sqs;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * Unit Tests for {@link SqsService}.
 *
 */
public class SqsServiceTest {

  /** {@link SqsService}. */
  private static SqsService sqs;
  /** {@link SqsConnectionBuilder}. */
  private static SqsConnectionBuilder sqsConnection;

  /**
   * Before Class.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException, InterruptedException {
    sqsConnection = new SqsConnectionBuilder().setRegion(Region.US_EAST_1);
    sqs = new SqsService(sqsConnection);
  }

  /**
   * Test converting QueueUrl to QueueArn.
   */
  @Test
  public void testGetQueueArn01() {
    assertEquals(
        "arn:aws:sqs:us-east-1:123456789000:updatetest-3df09234-1987-4f24-84d0-d77934ff8a80",
        sqs.getQueueArn(
            "https://sqs.us-east-1.amazonaws.com/123456789000/updatetest-3df09234-1987-4f24-84d0-d77934ff8a80"));
  }
}
