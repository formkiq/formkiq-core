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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.LogMessageBuilder;
import com.formkiq.module.lambdaservices.logger.LogType;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.module.lambdaservices.logger.LoggerImpl;

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

  /** Network timeout. */
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  /** Retest evidence logger. */
  private static final Logger LOGGER = new LoggerImpl(LogLevel.INFO, LogType.TEXT);

  /**
   * beforeclass.
   *
   */
  @BeforeAll
  public static void beforeClass() {

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

  private String consoleUrl() {
    return ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/Url");
  }

  private HttpResponse<String> get(final HttpClient client, final URI uri)
      throws IOException, InterruptedException {
    return client.send(HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private URI origin(final URI uri) {
    assertTrue(Set.of("http", "https").contains(uri.getScheme()), "HTTP(S) URL required");
    assertTrue(uri.getHost() != null && uri.getUserInfo() == null,
        "Host without credentials required");
    return URI.create(uri.getScheme() + "://" + uri.getRawAuthority() + "/");
  }

  private void recordHeaders(final String owner, final HttpResponse<String> response) {
    URI uri = response.uri();
    // Do not record query parameters, OAuth codes, cookies, or response bodies.
    LOGGER.info(LogMessageBuilder.title("Framing response").property("owner", owner)
        .property("url", origin(uri).resolve(uri.getPath()))
        .property("status", response.statusCode())
        .property("csp", response.headers().allValues("Content-Security-Policy"))
        .property("xfo", response.headers().allValues("X-Frame-Options")).build());
  }

  /**
   * Record and check Cognito separately; this distribution is managed by AWS.
   *
   * @throws IOException network failure
   * @throws InterruptedException interrupted request
   */
  @Test
  public void testCognitoHeadersSeparately() throws IOException, InterruptedException {
    try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL).build()) {
      HttpResponse<String> configResponse =
          get(client, URI.create(consoleUrl()).resolve("/assets/config.json"));
      assertEquals(200, configResponse.statusCode());
      Map<String, Object> config =
          new GsonBuilder().create().fromJson(configResponse.body(), Map.class);
      String ssoUrl = (String) config.getOrDefault("cognitoSingleSignOnUrl", "");
      assumeTrue(ssoUrl != null && !ssoUrl.isBlank(),
          "Managed Cognito SSO is not configured in the deployed Console");
      URI authorize = URI.create(ssoUrl);
      assertNotNull(authorize.getRawQuery(),
          "Configured SSO URL must include its OAuth parameters");
      // Inspect Cognito's login page separately from automatic redirects to an external IdP.
      String query = Arrays.stream(authorize.getRawQuery().split("&"))
          .filter(parameter -> !parameter.startsWith("identity_provider=")
              && !parameter.startsWith("idp_identifier="))
          .collect(Collectors.joining("&"));
      URI target = origin(authorize).resolve("/login?" + query);
      HttpResponse<String> response = get(client, target);
      recordHeaders("AWS-managed-Cognito-or-final-identity-provider", response);
      assertEquals(200, response.statusCode());
      assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
      assertEquals(origin(target), origin(response.uri()),
          "Login changed origin; inspect Cognito and the final identity provider separately");
      List<String> ancestors = response.headers().allValues("Content-Security-Policy").stream()
          .flatMap(policy -> Arrays.stream(policy.split(";")).map(String::trim)
              .filter(directive -> directive.matches("(?i)frame-ancestors(?:\\s.*)?")).limit(1))
          .toList();
      if (!ancestors.isEmpty()) {
        assertTrue(
            ancestors.stream()
                .anyMatch(directive -> directive.matches("frame-ancestors\\s+'(self|none)'\\s*")),
            "Cognito must enforce same-origin framing or deny framing");
      } else {
        List<String> options = response.headers().allValues("X-Frame-Options");
        assertEquals(1, options.size(), "Cognito needs an enforcing framing header");
        assertTrue(
            Set.of("SAMEORIGIN", "DENY")
                .contains(options.getFirst().trim().toUpperCase(java.util.Locale.ROOT)),
            "Cognito X-Frame-Options must block cross-origin framing");
      }
    }
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
    try (HttpClient service = HttpClient.newHttpClient()) {
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
  }

  /**
   * Test Logging into console.
   *
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @Test
  public void testLogin() throws URISyntaxException, IOException, InterruptedException {
    String url = ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/Url");
    String configUrl = url + "/assets/config.json";

    HttpRequest request = HttpRequest.newBuilder().uri(new URI(configUrl)).GET().build();
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      Map<String, Object> map = new GsonBuilder().create().fromJson(response.body(), Map.class);
      String userAuthentication = map.get("userAuthentication").toString();

      LaunchOptions options = new LaunchOptions().setHeadless(true);

      try (Playwright playwright = Playwright.create()) {
        try (Browser browser = playwright.chromium().launch(options)) {

          try (Page page = browser.newPage()) {
            page.navigate(url);

            page.waitForSelector("text=Sign In");

            assertEquals("Sign In", page.title());

            if ("saml".equals(userAuthentication)) {

              page.waitForSelector("text=Sign In");
              Locator element = page.locator("text=Sign In");
              assertEquals(1, element.count());

            } else {

              page.click("[placeholder=\"me@mycompany.com\"]");
              page.fill("[placeholder=\"me@mycompany.com\"]", USER);
              page.click("[placeholder=\"******\"]");
              page.fill("[placeholder=\"******\"]", PASSWORD);

              page.locator("button:has-text(\"Sign In\")").click();

              page.waitForSelector("button:has-text(\"New\")");
              page.waitForSelector("text=Documents & Folders");
            }
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
    assertEquals("v4.2.13",
        ssmService.getParameterValue("/formkiq/" + appenvironment + "/console/version"));
    assertTrue(ssmService.getParameterValue("/formkiq/" + appenvironment + "/s3/Console")
        .contains(appenvironment + "-web-ui-"));
  }
}
