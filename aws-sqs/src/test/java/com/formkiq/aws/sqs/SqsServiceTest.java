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
