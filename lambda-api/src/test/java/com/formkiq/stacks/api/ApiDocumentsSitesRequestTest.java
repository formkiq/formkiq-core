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

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;

/** Unit Tests for request /sites. */
public class ApiDocumentsSitesRequestTest extends AbstractRequestHandler {

  /** Email Pattern. */
  private static final String EMAIL = "[abcdefghijklmnopqrstuvwxyz0123456789]{8}";

  /**
   * Get /sites with SES support.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites01() throws Exception {
    // given
    putSsmParameter("/formkiq/" + getAppenvironment() + "/maildomain", "tryformkiq.com");
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-sites01.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    ApiSitesResponse resp = GsonUtil.getInstance().fromJson(m.get("body"), ApiSitesResponse.class);

    assertNull(resp.getNext());
    assertNull(resp.getPrevious());

    assertEquals(2, resp.getSites().size());
    assertEquals(DEFAULT_SITE_ID, resp.getSites().get(0).getSiteId());
    assertNotNull(resp.getSites().get(0).getUploadEmail());

    String uploadEmail = resp.getSites().get(0).getUploadEmail();
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));

    assertNotNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email", getAppenvironment(),
        resp.getSites().get(0).getSiteId())));

    String[] strs = uploadEmail.split("@");
    assertEquals("tryformkiq.com", strs[1]);
    assertEquals("{\"siteId\":\"default\", \"appEnvironment\":\"" + getAppenvironment() + "\"}",
        getSsmParameter(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));

    assertEquals("finance", resp.getSites().get(1).getSiteId());
    assertNotNull(resp.getSites().get(1).getUploadEmail());
    uploadEmail = resp.getSites().get(1).getUploadEmail();
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));
    assertNotNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email", getAppenvironment(),
        resp.getSites().get(1).getSiteId())));
    strs = uploadEmail.split("@");
    assertEquals("{\"siteId\":\"finance\", \"appEnvironment\":\"" + getAppenvironment() + "\"}",
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
    removeSsmParameter("/formkiq/" + getAppenvironment() + "/maildomain");
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-sites01.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    ApiSitesResponse resp = GsonUtil.getInstance().fromJson(m.get("body"), ApiSitesResponse.class);

    assertNull(resp.getNext());
    assertNull(resp.getPrevious());

    assertEquals(2, resp.getSites().size());
    assertEquals(DEFAULT_SITE_ID, resp.getSites().get(0).getSiteId());
    assertNull(resp.getSites().get(0).getUploadEmail());

    assertEquals("finance", resp.getSites().get(1).getSiteId());
    assertNull(resp.getSites().get(1).getUploadEmail());
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
    putSsmParameter("/formkiq/" + getAppenvironment() + "/maildomain", "tryformkiq.com");
    removeSsmParameter(
        String.format("/formkiq/%s/siteid/%s/email", getAppenvironment(), "default"));
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-sites01.json");
    setCognitoGroup(event, "default_read finance");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    ApiSitesResponse resp = GsonUtil.getInstance().fromJson(m.get("body"), ApiSitesResponse.class);

    assertNull(resp.getNext());
    assertNull(resp.getPrevious());

    assertEquals(2, resp.getSites().size());
    assertEquals(DEFAULT_SITE_ID, resp.getSites().get(0).getSiteId());
    assertNull(resp.getSites().get(0).getUploadEmail());

    assertNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email", getAppenvironment(),
        resp.getSites().get(0).getSiteId())));

    assertEquals("finance", resp.getSites().get(1).getSiteId());
    assertNotNull(resp.getSites().get(1).getUploadEmail());
    String uploadEmail = resp.getSites().get(1).getUploadEmail();
    assertTrue(uploadEmail.endsWith("@tryformkiq.com"));
    assertTrue(Pattern.matches(EMAIL, uploadEmail.subSequence(0, uploadEmail.indexOf("@"))));
    assertNotNull(getSsmParameter(String.format("/formkiq/%s/siteid/%s/email", getAppenvironment(),
        resp.getSites().get(1).getSiteId())));
    String[] strs = uploadEmail.split("@");
    assertEquals("{\"siteId\":\"finance\", \"appEnvironment\":\"" + getAppenvironment() + "\"}",
        getSsmParameter(String.format("/formkiq/ses/%s/%s", strs[1], strs[0])));
  }
}
