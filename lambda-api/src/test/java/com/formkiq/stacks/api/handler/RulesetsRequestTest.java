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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_PAYMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/**
 * Unit Tests for request /userActivities, /userActivities/{userId},
 * /documents/{documentId}/userActivities.
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class RulesetsRequestTest extends AbstractApiClientRequestTest {

  /**
   * GET /rulesets.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetRulesets() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.rulesetApi.getRulesets(siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /rulesets/{rulesetId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetRulesetsId() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      String rulesetId = UUID.randomUUID().toString();

      try {
        // when
        this.rulesetApi.getRuleset(rulesetId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /rulesets/{rulesetId}/rules.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetRulesetsRules() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      String rulesetId = UUID.randomUUID().toString();

      try {
        // when
        this.rulesetApi.getRules(rulesetId, siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /rulesets/{rulesetId}/rules/{ruleId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetRulesetsRulesId() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      String rulesetId = UUID.randomUUID().toString();
      String ruleId = UUID.randomUUID().toString();

      try {
        // when
        this.rulesetApi.getRule(rulesetId, ruleId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }
}
