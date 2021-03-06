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
package com.formkiq.stacks.api.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Unit Tests for {@link DocumentsRestrictionsMaxContentLength}.
 *
 * 
 */
public class DocumentsRestrictionsMaxContentLengthTest {

  /** {@link DocumentsRestrictionsMaxContentLength}. */
  private static DocumentsRestrictionsMaxContentLength service;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsservice;

  /**
   * Before Class.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException, InterruptedException {

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    SsmConnectionBuilder ssmConnection = new SsmConnectionBuilder().setCredentials(cred)
        .setRegion(Region.US_EAST_1).setEndpointOverride("http://localhost:4566");

    awsservice = new AwsServiceCache().ssmConnection(ssmConnection).appEnvironment("unittest");
    service = new DocumentsRestrictionsMaxContentLength();
  }

  /**
   * No Max Content Length or Content Length.
   */
  @Test
  public void testEnforced01() {
    // given
    String siteId = UUID.randomUUID().toString();
    Long contentLength = null;

    // when
    String value = service.getSsmValue(awsservice, siteId);
    boolean result = service.enforced(awsservice, siteId, value, contentLength);

    // then
    assertFalse(result);
  }

  /**
   * Max Content Length, no Content Length.
   */
  @Test
  public void testEnforced02() {
    // given
    Long contentLength = null;
    String siteId = UUID.randomUUID().toString();
    String ssmkey = "/formkiq/unittest/siteid/" + siteId + "/MaxContentLengthBytes";
    awsservice.ssmService().putParameter(ssmkey, "10");

    // when
    String value = service.getSsmValue(awsservice, siteId);
    boolean result = service.enforced(awsservice, siteId, value, contentLength);

    // then
    assertTrue(result);
  }

  /**
   * Max Content Length, Content Length less than or equal.
   */
  @Test
  public void testEnforced03() {
    // given
    Long contentLength = Long.valueOf("10");
    String siteId = UUID.randomUUID().toString();
    String ssmkey = "/formkiq/unittest/siteid/" + siteId + "/MaxContentLengthBytes";
    awsservice.ssmService().putParameter(ssmkey, "10");

    // when
    String value = service.getSsmValue(awsservice, siteId);
    boolean result = service.enforced(awsservice, siteId, value, contentLength);

    // then
    assertFalse(result);
  }


  /**
   * Max Content Length, Content Length greater.
   */
  @Test
  public void testEnforced04() {
    // given
    Long contentLength = Long.valueOf("15");
    String siteId = UUID.randomUUID().toString();
    String ssmkey = "/formkiq/unittest/siteid/" + siteId + "/MaxContentLengthBytes";
    awsservice.ssmService().putParameter(ssmkey, "10");

    // when
    String value = service.getSsmValue(awsservice, siteId);
    boolean result = service.enforced(awsservice, siteId, value, contentLength);

    // then
    assertTrue(result);
  }

  /**
   * Max Content Length, Content Length=0.
   */
  @Test
  public void testEnforced05() {
    // given
    Long contentLength = Long.valueOf(0);
    String siteId = UUID.randomUUID().toString();
    String ssmkey = "/formkiq/unittest/siteid/" + siteId + "/MaxContentLengthBytes";
    awsservice.ssmService().putParameter(ssmkey, "10");

    // when
    String value = service.getSsmValue(awsservice, siteId);
    boolean result = service.enforced(awsservice, siteId, value, contentLength);

    // then
    assertTrue(result);
  }
}
