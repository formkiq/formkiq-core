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
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_FORBIDDEN;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetSitesResponse;
import com.formkiq.client.model.Site;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class SitesRequestTest extends AbstractApiClientRequestTest {

  /** {@link ConfigService}. */
  private static ConfigService config;
  /** Email Pattern. */
  private static final String EMAIL = "[abcdefghijklmnopqrstuvwxyz0123456789]{8}";
  /** {@link SsmService}. */
  private static SsmService ssm;

  /**
   * BeforeEach.
   */
  @BeforeEach
  public void beforeEach() {
    config = getAwsServices().getExtension(ConfigService.class);
    ssm = getAwsServices().getExtension(SsmService.class);
  }

  /**
   * Get /sites with SES support.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetSites01() throws Exception {
    // given
    String siteId = null;
    setBearerToken(new String[] {"default", "Admins", "finance"});
    config.save(siteId, new DynamicObject(Map.of("chatGptApiKey", "somevalue")));

    ssm.putParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/maildomain", "tryformkiq.com");

    // when
    GetSitesResponse response = this.systemApi.getSites();

    // then
    List<Site> sites = response.getSites();
    assertEquals(2, sites.size());

    assertEquals(DEFAULT_SITE_ID, sites.get(0).getSiteId());
    assertEquals("READ_WRITE", sites.get(0).getPermission().toString());
    assertEquals("ADMIN,DELETE,READ,WRITE",
        sites.get(0).getPermissions().stream().map(p -> p.name()).collect(Collectors.joining(",")));
    assertNotNull(sites.get(0).getUploadEmail());

    String uploadEmail = sites.get(0).getUploadEmail();
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));

    assertNotNull(ssm.getParameterValue(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(0).getSiteId())));

    String[] strs = uploadEmail.split("@");
    assertEquals("tryformkiq.com", strs[1]);
    assertEquals("{\"siteId\":\"default\", \"appEnvironment\":\"" + FORMKIQ_APP_ENVIRONMENT + "\"}",
        ssm.getParameterValue(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));

    assertEquals("finance", sites.get(1).getSiteId());
    assertEquals("READ_WRITE", sites.get(1).getPermission().toString());
    assertNotNull(sites.get(1).getUploadEmail());
    uploadEmail = sites.get(1).getUploadEmail();
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
    setBearerToken(new String[] {"default", "Admins", "finance"});

    // when
    GetSitesResponse response = this.systemApi.getSites();

    // then
    List<Site> sites = response.getSites();
    assertEquals(2, sites.size());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).getSiteId());
    assertEquals("READ_WRITE", sites.get(0).getPermission().toString());
    assertNull(sites.get(0).getUploadEmail());

    assertEquals("finance", sites.get(1).getSiteId());
    assertEquals("READ_WRITE", sites.get(1).getPermission().toString());
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
        String.format("/formkiq/%s/siteid/%s/email", FORMKIQ_APP_ENVIRONMENT, "default"));

    setBearerToken(new String[] {"default_read", "finance"});

    // when
    GetSitesResponse response = this.systemApi.getSites();

    // then
    List<Site> sites = response.getSites();

    assertEquals(2, sites.size());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).getSiteId());
    assertEquals("READ_ONLY", sites.get(0).getPermission().toString());
    assertNull(sites.get(0).getUploadEmail());

    assertNull(ssm.getParameterValue(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(0).getSiteId())));

    assertEquals("finance", sites.get(1).getSiteId());
    assertEquals("READ_WRITE", sites.get(1).getPermission().toString());
    assertNotNull(sites.get(1).getUploadEmail());
    String uploadEmail = sites.get(1).getUploadEmail();
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

    ConfigService configService = getAwsServices().getExtension(ConfigService.class);
    configService.save(siteId, new DynamicObject(Map.of(MAX_DOCUMENTS, "5", MAX_WEBHOOKS, "10")));

    // when
    GetSitesResponse response = this.systemApi.getSites();

    // then
    List<Site> sites = response.getSites();

    assertEquals(1, sites.size());
    assertEquals(siteId, sites.get(0).getSiteId());
    assertEquals("READ_WRITE", sites.get(0).getPermission().toString());
  }

  /**
   * Get /sites with 'authentication_only' role.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetSites05() throws Exception {
    // given
    String siteId = "authentication_only";
    setBearerToken(siteId);

    // when
    try {
      this.systemApi.getSites();
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(SC_FORBIDDEN.getStatusCode(), e.getCode());
    }
  }
}
