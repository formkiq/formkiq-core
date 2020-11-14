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
import java.net.http.HttpResponse;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;

/**
 * Process Urls.
 * <p>
 * GET /version tests
 * </p>
 *
 */
public class VersionRequestTest extends AbstractApiTest {

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
  public void testVersion01() throws Exception {
    // given
    for (FormKiqClientV1 c : getFormKiqClients()) {
      assertNotNull(c.getVersion());
    }
  }

  /**
   * Options.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      HttpResponse<String> response = client.optionsVersion();
      assertEquals(HTTP_STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }
}
