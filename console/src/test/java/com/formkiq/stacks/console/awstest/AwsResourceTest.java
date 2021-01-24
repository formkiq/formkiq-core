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
package com.formkiq.stacks.console.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Test CloudFormation.
 */
public class AwsResourceTest {

  /** App Environment Name. */
  private static String appenvironment;
  /** {@link SsmService}. */
  private static SsmService ssmService;
  /** {@link S3Service}. */
  private static S3Service s3;

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   */
  @BeforeClass
  public static void beforeClass() throws IOException {

    Region region = Region.of(System.getProperty("testregion"));

    String awsprofile = System.getProperty("testprofile");
    appenvironment = System.getProperty("testappenvironment");

    final SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder().setCredentials(awsprofile).setRegion(region);
    ssmService = new SsmServiceImpl(ssmBuilder);

    final S3ConnectionBuilder s3Builder =
        new S3ConnectionBuilder().setCredentials(awsprofile).setRegion(region);
    s3 = new S3Service(s3Builder);
  }

  /**
   * Test S3 Buckets.
   */
  @Test
  public void testS3Buckets() {
    final String version =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/version");
    final String consoleBucket =
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/Console");

    try (S3Client s = s3.buildClient()) {
      S3ObjectMetadata resp = s3.getObjectMetadata(s, consoleBucket, version + "/index.html");
      assertTrue(resp.isObjectExists());

      assertTrue(s3.exists(s, consoleBucket));
    }
  }

  /**
   * Test SSM Parameter Store.
   */
  @Test
  public void testSsmParameters() {
    assertEquals("v1.3.5",
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/version"));
    assertTrue(ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/Console")
        .contains(appenvironment + "-console-"));
  }

  /**
   * Test Console Available.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  @Test
  public void testConsoleAvailable() throws IOException, InterruptedException, URISyntaxException {
    // given
    HttpClient service = HttpClient.newHttpClient();
    String url = ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/Url");

    // when
    HttpResponse<String> response =
        service.send(HttpRequest.newBuilder(new URI(url)).build(), BodyHandlers.ofString());

    // then
    final int statusCode = 200;
    assertEquals(statusCode, response.statusCode());

    String text = response.body();
    assertTrue(text.contains("<title>FormKiQ Stacks Console</title>"));
  }
}
