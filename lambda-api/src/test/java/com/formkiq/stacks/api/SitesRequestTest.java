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
package com.formkiq.stacks.api;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.services.lambda.services.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.aws.services.lambda.services.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.aws.services.lambda.services.ConfigService;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class SitesRequestTest extends AbstractRequestHandler {

  /** Email Pattern. */
  private static final String EMAIL = "[abcdefghijklmnopqrstuvwxyz0123456789]{8}";

  /**
   * Get /esignature/docusign/config request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("get")
        .resource("/sites").path("/sites").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
  }

  /**
   * Get /sites with SES support.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites01() throws Exception {
    // given
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/maildomain", "tryformkiq.com");
    ApiGatewayRequestEvent event = getRequest(null, "default Admins finance");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    assertNull(resp.getString("next"));
    assertNull(resp.getString("previous"));

    List<DynamicObject> sites = resp.getList("sites");
    assertEquals(2, sites.size());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).get("siteId"));
    assertEquals("READ_WRITE", sites.get(0).get("permission"));
    assertNotNull(sites.get(0).get("uploadEmail"));

    String uploadEmail = sites.get(0).getString("uploadEmail");
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));

    assertNotNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(0).get("siteId"))));

    String[] strs = uploadEmail.split("@");
    assertEquals("tryformkiq.com", strs[1]);
    assertEquals("{\"siteId\":\"default\", \"appEnvironment\":\"" + FORMKIQ_APP_ENVIRONMENT + "\"}",
        getSsmParameter(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));

    assertEquals("finance", sites.get(1).get("siteId"));
    assertEquals("READ_WRITE", sites.get(1).get("permission"));
    assertNotNull(sites.get(1).get("uploadEmail"));
    uploadEmail = sites.get(1).getString("uploadEmail");
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));
    assertNotNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(1).get("siteId"))));
    strs = uploadEmail.split("@");
    assertEquals("{\"siteId\":\"finance\", \"appEnvironment\":\"" + FORMKIQ_APP_ENVIRONMENT + "\"}",
        getSsmParameter(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));
  }

  /**
   * Get /sites WITHOUT SES support.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites02() throws Exception {
    // given
    removeSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/maildomain");
    ApiGatewayRequestEvent event = getRequest(null, "default Admins finance");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    assertNull(resp.getString("next"));
    assertNull(resp.getString("previous"));

    List<DynamicObject> sites = resp.getList("sites");
    assertEquals(2, sites.size());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).get("siteId"));
    assertEquals("READ_WRITE", sites.get(0).get("permission"));
    assertNull(sites.get(0).get("uploadEmail"));

    assertEquals("finance", sites.get(1).get("siteId"));
    assertEquals("READ_WRITE", sites.get(1).get("permission"));
    assertNull(sites.get(1).get("uploadEmail"));
  }

  /**
   * Get /sites with 'read' permissions SES support.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites03() throws Exception {
    // given
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/maildomain", "tryformkiq.com");
    removeSsmParameter(
        String.format("/formkiq/%s/siteid/%s/email", FORMKIQ_APP_ENVIRONMENT, "default"));
    ApiGatewayRequestEvent event = getRequest(null, "default_read finance");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));

    List<DynamicObject> sites = resp.getList("sites");
    assertEquals(2, sites.size());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).get("siteId"));
    assertEquals("READ_ONLY", sites.get(0).get("permission"));
    assertNull(sites.get(0).get("uploadEmail"));

    assertNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email", FORMKIQ_APP_ENVIRONMENT,
        sites.get(0).get("siteId"))));

    assertEquals("finance", sites.get(1).get("siteId"));
    assertEquals("READ_WRITE", sites.get(1).get("permission"));
    assertNotNull(sites.get(1).get("uploadEmail"));
    String uploadEmail = sites.get(1).getString("uploadEmail");
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));
    assertNotNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email",
        FORMKIQ_APP_ENVIRONMENT, sites.get(1).get("siteId"))));
    String[] strs = uploadEmail.split("@");
    assertEquals("{\"siteId\":\"finance\", \"appEnvironment\":\"" + FORMKIQ_APP_ENVIRONMENT + "\"}",
        getSsmParameter(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));
  }

  /**
   * Get /sites with Config.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites04() throws Exception {
    // given
    String siteId = "finance";
    ApiGatewayRequestEvent event = getRequest(siteId, siteId);

    ConfigService configService = getAwsServices().getExtension(ConfigService.class);
    configService.save(siteId, new DynamicObject(Map.of(MAX_DOCUMENTS, "5", MAX_WEBHOOKS, "10")));

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));
    assertEquals(1, resp.getList("sites").size());
    assertEquals(siteId, resp.getList("sites").get(0).getString("siteId"));
    assertEquals("READ_WRITE", resp.getList("sites").get(0).getString("permission"));
    assertEquals("5", resp.getList("sites").get(0).getString(MAX_DOCUMENTS));
    assertEquals("10", resp.getList("sites").get(0).getString(MAX_WEBHOOKS));
  }
}
