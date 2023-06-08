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
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import com.formkiq.testutils.aws.FkqCognitoService;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import software.amazon.awssdk.regions.Region;

/**
 * Test CloudFormation.
 */
public class AwsResourceTest {

  /** App Environment Name. */
  private static String appenvironment;
  /** Console Page Title. */
  private static final String PAGE_TITLE = "FormKiQ Document Console";
  /** Cognito Password. */
  private static final String PASSWORD = "uae82nj23njd!@";
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link SsmService}. */
  private static SsmService ssmService;
  /** Cognito User. */
  private static final String USER = "test12384@formkiq.com";

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

    SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder(false).setCredentials(awsprofile).setRegion(region);
    ssmService = new SsmServiceImpl(ssmBuilder);

    final S3ConnectionBuilder s3Builder =
        new S3ConnectionBuilder(false).setCredentials(awsprofile).setRegion(region);
    s3 = new S3Service(s3Builder);

    FkqCognitoService cognito = new FkqCognitoService(awsprofile, region, appenvironment);
    cognito.addUser(USER, PASSWORD);
    cognito.addUserToGroup(USER, "default");
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
    assertTrue(text.contains("<title>" + PAGE_TITLE + "</title>"));
  }

  /**
   * Test Logging into console.
   * 
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testLogin() throws URISyntaxException, IOException, InterruptedException {
    String url = ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/Url");
    String configUrl = url + "/assets/config.json";

    HttpRequest request = HttpRequest.newBuilder().uri(new URI(configUrl)).GET().build();
    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    Map<String, Object> map = new GsonBuilder().create().fromJson(response.body(), Map.class);
    String userAuthentication = map.get("userAuthentication").toString();

    LaunchOptions options = new LaunchOptions().setHeadless(true);

    try (Playwright playwright = Playwright.create()) {
      try (Browser browser = playwright.chromium().launch(options)) {

        try (Page page = browser.newPage()) {
          page.navigate(url);

          page.waitForSelector("input:has-text(\"Sign In\")");

          assertEquals("Sign In", page.title());

          if ("saml".equals(userAuthentication)) {

            page.waitForNavigation(() -> {
              page.waitForSelector("text=Sign In");
              Locator element = page.locator("text=Sign In");
              assertEquals(1, element.count());
            });

          } else {

            page.click("[placeholder=\"me@mycompany.com\"]");
            page.fill("[placeholder=\"me@mycompany.com\"]", USER);
            page.click("[placeholder=\"******\"]");
            page.fill("[placeholder=\"******\"]", PASSWORD);
            page.waitForNavigation(() -> {
              page.locator("input:has-text(\"Sign In\")").click();
            });

            page.waitForSelector("button:has-text(\"New\")");
            page.waitForSelector("text=Documents & Folders");
          }
        }
      }
    }
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

    S3ObjectMetadata resp = s3.getObjectMetadata(consoleBucket, version + "/index.html", null);
    assertTrue(resp.isObjectExists());

    assertTrue(s3.exists(consoleBucket));
  }

  /**
   * Test SSM Parameter Store.
   */
  @Test
  public void testSsmParameters() {
    assertEquals("v3.1.0",
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/version"));
    assertTrue(ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/Console")
        .contains(appenvironment + "-console-"));
  }
}
