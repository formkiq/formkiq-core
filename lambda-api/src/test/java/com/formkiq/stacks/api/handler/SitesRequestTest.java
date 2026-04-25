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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.formkiq.client.model.SiteConfig;
import com.formkiq.client.model.SitePermission;
import com.formkiq.client.model.SiteUsage;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationOcr;
import com.formkiq.testutils.api.JwtTokenBuilder;
import com.formkiq.testutils.api.SetBearerPermissionMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetSitesResponse;
import com.formkiq.client.model.GetVersionResponse;
import com.formkiq.client.model.Site;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceExtension;

/** Unit Tests for request /sites. */
public class SitesRequestTest extends AbstractApiClientRequestTest {

  /** {@link ConfigService}. */
  private static ConfigService configService;
  /** Email Pattern. */
  private static final String EMAIL = "[abcdefghijklmnopqrstuvwxyz0123456789]{8}";
  /** {@link SsmService}. */
  private static SsmService ssm;
  /** {@link HttpService}. */
  private final HttpService http = new HttpServiceJdk11();

  /**
   * BeforeEach.
   */
  @BeforeEach
  public void beforeEach() {
    AwsServiceCache awsServices = getAwsServices();

    awsServices.register(ConfigService.class, new ConfigServiceExtension());
    awsServices.register(SsmService.class, new SsmServiceExtension());

    configService = awsServices.getExtension(ConfigService.class);
    ssm = awsServices.getExtension(SsmService.class);
  }

  @Test
  void testGetSitesWithEmptyPermissionsMap() throws ApiException {
    // given
    new SetBearerPermissionMap().apply(this.client, List.of("default", "qa"), Map.of(), Map.of());

    // when
    GetSitesResponse response = this.systemApi.getSites(null);

    // then
    assertNotNull(response);
    assertEquals("joesmith", response.getUsername());
    assertEquals("default,qa",
        notNull(response.getRoles()).stream().sorted().collect(Collectors.joining(",")));
    assertEquals(0, notNull(response.getSamlGroups()).size());
    assertEquals(0, notNull(response.getUserClaims()).size());
    assertEquals(0, notNull(response.getSites()).size());
  }

  @Test
  void testGetSitesWithJwtCustomClaims() throws IOException {
    // given
    String jwt = new JwtTokenBuilder("bill").group("test").claim("tenant", "acme")
        .claim("profile", Map.of("department", "sales")).build();

    String url = server.getBasePath() + "/sites";

    // when
    HttpResponse<String> response = this.http.get(url,
        Optional.of(new HttpHeaders().add("Authorization", jwt)), Optional.empty());
    Map<String, Object> body = fromJson(response.body(), Map.class);
    Map<String, Object> jwtClaims = (Map<String, Object>) body.get("userClaims");

    // then
    assertEquals(200, response.statusCode());
    assertNotNull(jwtClaims);
    assertEquals("acme", jwtClaims.get("tenant"));
    assertEquals(Map.of("department", "sales"), jwtClaims.get("profile"));
    assertNull(jwtClaims.get("cognito:groups"));
  }

  @Test
  void testGetSitesWithSaml() throws ApiException {
    // given
    new JwtTokenBuilder("bill").group("test").samlGroups(List.of("asd")).build(this.client);

    // when
    GetSitesResponse response = this.systemApi.getSites(null);

    // then
    List<Site> sites = notNull(response.getSites());
    assertEquals(1, sites.size());
    assertEquals("test", sites.get(0).getSiteId());
    assertEquals("test", String.join(", ", notNull(response.getRoles())));
    assertEquals("asd", String.join(", ", notNull(response.getSamlGroups())));
  }

  /**
   * Get /sites with SES support.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetSites01() throws Exception {
    // given
    setBearerToken(new String[] {DEFAULT_SITE_ID, "Admins", "finance"});
    SiteConfiguration siteConfig =
        SiteConfiguration.builder().chatGptApiKey("somevalue").build((String) null);
    configService.save(null, siteConfig);

    ssm.putParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/maildomain", "tryformkiq.com");

    // when
    GetSitesResponse response = this.systemApi.getSites(null);

    // then
    assertEquals("default,Admins,finance", String.join(",", notNull(response.getRoles())));

    List<Site> sites = notNull(response.getSites());
    assertEquals(2, sites.size());

    Site site = sites.get(0);
    assertEquals(DEFAULT_SITE_ID, site.getSiteId());
    assertEquals(SitePermission.READ_WRITE, site.getPermission());
    assertEquals("ADMIN,DELETE,GOVERN,READ,WRITE",
        notNull(site.getPermissions()).stream().map(Enum::name).collect(Collectors.joining(",")));
    assertNotNull(site.getUploadEmail());

    String uploadEmail = site.getUploadEmail();
    assertNotNull(uploadEmail);
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));

    assertNotNull(ssm.getParameterValue(
        String.format("/formkiq/%s/siteid/%s/email", FORMKIQ_APP_ENVIRONMENT, site.getSiteId())));

    String[] strs = uploadEmail.split("@");
    assertEquals("tryformkiq.com", strs[1]);
    assertEquals("{\"siteId\":\"default\", \"appEnvironment\":\"" + FORMKIQ_APP_ENVIRONMENT + "\"}",
        ssm.getParameterValue(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));

    assertEquals("finance", sites.get(1).getSiteId());
    assertEquals(SitePermission.READ_WRITE, sites.get(1).getPermission());
    assertNotNull(sites.get(1).getUploadEmail());
    uploadEmail = sites.get(1).getUploadEmail();
    assertNotNull(uploadEmail);
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));
    assertNotNull(ssm.getParameterValue(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(1).getSiteId())));
    strs = uploadEmail.split("@");
    assertEquals("{\"siteId\":\"finance\", \"appEnvironment\":\"" + FORMKIQ_APP_ENVIRONMENT + "\"}",
        ssm.getParameterValue(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));
  }

  /**
   * Get /sites WITHOUT SES support.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetSites02() throws Exception {
    // given
    ssm.removeParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/maildomain");
    setBearerToken(new String[] {DEFAULT_SITE_ID, "Admins", "finance"});

    // when
    GetSitesResponse response = this.systemApi.getSites(null);

    // then
    List<Site> sites = notNull(response.getSites());
    assertEquals(2, sites.size());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).getSiteId());
    assertEquals(SitePermission.READ_WRITE, sites.get(0).getPermission());
    assertNull(sites.get(0).getUploadEmail());

    assertEquals("finance", sites.get(1).getSiteId());
    assertEquals(SitePermission.READ_WRITE, sites.get(1).getPermission());
    assertNull(sites.get(1).getUploadEmail());
  }

  /**
   * Get /sites with 'read' permissions SES support.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetSites03() throws Exception {
    // given
    ssm.putParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/maildomain", "tryformkiq.com");
    ssm.removeParameter(
        String.format("/formkiq/%s/siteid/%s/email", FORMKIQ_APP_ENVIRONMENT, DEFAULT_SITE_ID));

    setBearerToken(new String[] {"default_read", "finance"});

    // when
    GetSitesResponse response = this.systemApi.getSites(null);

    // then
    List<Site> sites = notNull(response.getSites());

    assertEquals(2, sites.size());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).getSiteId());
    assertEquals(SitePermission.READ_ONLY, sites.get(0).getPermission());
    assertNull(sites.get(0).getUploadEmail());

    assertNull(ssm.getParameterValue(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(0).getSiteId())));

    assertEquals("finance", sites.get(1).getSiteId());
    assertEquals(SitePermission.READ_WRITE, sites.get(1).getPermission());
    assertNotNull(sites.get(1).getUploadEmail());
    String uploadEmail = sites.get(1).getUploadEmail();
    assertNotNull(uploadEmail);
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));
    assertNotNull(ssm.getParameterValue(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(1).getSiteId())));
    String[] strs = uploadEmail.split("@");
    assertEquals("{\"siteId\":\"finance\", \"appEnvironment\":\"" + FORMKIQ_APP_ENVIRONMENT + "\"}",
        ssm.getParameterValue(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));
  }

  /**
   * Get /sites with Config.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetSites04() throws Exception {
    // given
    String siteId = "finance";
    setBearerToken(siteId);

    SiteConfiguration siteConfig = SiteConfiguration.builder().maxDocuments("5").maxWebhooks("10")
        .ocr(new SiteConfigurationOcr(-1, 2)).build((String) null);
    configService.save(siteId, siteConfig);

    configService.increment(siteId, ConfigService.DOCUMENT_COUNT);
    configService.increment(siteId, ConfigService.DOCUMENT_COUNT);
    configService.increment(siteId, DocumentOcrService.CONFIG_OCR_COUNT);

    // when
    GetSitesResponse response = this.systemApi.getSites(null);

    // then
    List<Site> sites = notNull(response.getSites());

    assertEquals(1, sites.size());
    Site site = sites.get(0);
    assertEquals(siteId, site.getSiteId());
    assertEquals(SitePermission.READ_WRITE, site.getPermission());

    SiteConfig config = site.getConfig();
    assertNotNull(config);
    assertEquals("5", config.getMaxDocuments());
    assertEquals("10", config.getMaxWebhooks());

    assertNotNull(config.getOcr());
    assertEquals("2", Objects.requireNonNull(config.getOcr().getMaxTransactions()).toString());

    SiteUsage usage = site.getUsage();
    assertNotNull(usage);
    assertEquals(2, Objects.requireNonNull(usage.getDocumentCount()).longValue());
    assertEquals(1, Objects.requireNonNull(usage.getOcrTransactionCount()).longValue());
  }

  /**
   * Get /sites with 'authentication_only' role.
   *
   */
  @Test
  public void testHandleGetSites05() throws ApiException {
    // given
    String siteId = "authentication_only";
    setBearerToken(siteId);

    // when
    GetSitesResponse response = this.systemApi.getSites(null);

    // then
    assertNotNull(response);
    assertEquals("joesmith", response.getUsername());
    assertEquals("authentication_only",
        notNull(response.getRoles()).stream().sorted().collect(Collectors.joining(",")));
    assertEquals(0, notNull(response.getSamlGroups()).size());
    assertEquals(0, notNull(response.getUserClaims()).size());
    assertEquals(0, notNull(response.getSites()).size());
  }

  /**
   * Get /version.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleVersion01() throws Exception {
    // given
    setBearerToken(new String[] {DEFAULT_SITE_ID, "Admins", "finance"});

    // when
    GetVersionResponse response = this.systemApi.getVersion();

    // then
    assertNotNull(response.getVersion());
    assertEquals("typesense,site_permissions_automatic",
        String.join(",", notNull(response.getModules())));
  }
}
