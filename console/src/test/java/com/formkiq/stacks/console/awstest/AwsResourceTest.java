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
    assertEquals("v1.2",
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
