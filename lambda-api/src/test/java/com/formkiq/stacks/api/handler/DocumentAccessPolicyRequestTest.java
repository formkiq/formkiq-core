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
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_PAYMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;

import com.formkiq.aws.dynamodb.ID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.api.AccessControlApi;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.SetOpaAccessPolicyItemsRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites/{siteId}/opa/accessPolicy. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentAccessPolicyRequestTest extends AbstractApiClientRequestTest {

  /** {@link AccessControlApi}. */
  private AccessControlApi api = new AccessControlApi(this.client);

  /**
   * DELETE /sites/{siteId}/opa/accessPolicy/policyItems.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteOpaConfiguration() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      try {
        // when
        this.api.deleteOpaAccessPolicyItems(siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /sites/{siteId}/opa/accessPolicy.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetOpaConfiguration() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      try {
        // when
        this.api.getOpaAccessPolicy(siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /sites/{siteId}/opa/accessPolicy.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetOpaConfigurations() throws Exception {
    // given
    String siteId = null;
    setBearerToken(siteId);

    try {
      // when
      this.api.getOpaAccessPolicies();
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
    }
  }

  /**
   * PUT /sites/{siteId}/opa/accessPolicy/policyItems.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPutOpaConfiguration() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      SetOpaAccessPolicyItemsRequest req = new SetOpaAccessPolicyItemsRequest();

      try {
        // when
        this.api.setOpaAccessPolicyItems(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }
}
