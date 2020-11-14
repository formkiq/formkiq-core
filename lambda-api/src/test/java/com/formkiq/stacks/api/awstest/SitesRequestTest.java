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
package com.formkiq.stacks.api.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.net.http.HttpResponse;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.Sites;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * Process Urls.
 * <p>
 * GET /sites tests
 * </p>
 *
 */
public class SitesRequestTest extends AbstractApiTest {

  /** Http Status OK. */
  private static final int HTTP_STATUS_OK = 200;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Test GET /version.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSites01() throws Exception {
    // given
    AuthenticationResultType token = login(USER_EMAIL, USER_PASSWORD);
    putParameter("/formkiq/" + getAppenvironment() + "/maildomain", "tryformkiq.com");
    FormKiqClientV1 client = createHttpClient(token);

    // when
    Sites sites = client.getSites();
    // then
    assertEquals(1, sites.sites().size());
    assertNotNull(sites.sites().get(0).siteId());
    assertTrue(sites.sites().get(0).uploadEmail().endsWith("@tryformkiq.com"));
  }

  /**
   * Options.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      HttpResponse<String> response = client.optionsSites();
      assertEquals(HTTP_STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }
}
