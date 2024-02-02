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
import com.formkiq.client.api.AccessControlApi;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentAccessAttributesRequest;
import com.formkiq.client.model.SetDocumentAccessAttributesRequest;
import com.formkiq.client.model.SetOpaConfigurationRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /documents/{documentId}/accessAttributes. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentAccessAttributesRequestTest extends AbstractApiClientRequestTest {

  /** {@link AccessControlApi}. */
  private AccessControlApi api = new AccessControlApi(this.client);
  /** Document Id. */
  private String documentId = UUID.randomUUID().toString();

  /**
   * POST /documents/{documentId}/accessAttribute.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddDocumentAccessAttributes() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      AddDocumentAccessAttributesRequest req = new AddDocumentAccessAttributesRequest();

      try {
        // when
        this.api.addDocumentAccessAttributes(this.documentId, req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/accessAttribute.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteDocumentAccessAttributes() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.api.deleteDocumentAccessAttributes(this.documentId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * DELETE /configuration/opa/{opaId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteOpaConfiguration() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String opaId = UUID.randomUUID().toString();

      try {
        // when
        this.api.deleteOpaConfiguration(opaId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /documents/{documentId}/accessAttribute.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetDocumentAccessAttributes() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.api.getDocumentAccessAttributes(this.documentId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /configuration/opa/{opaId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetOpaConfiguration() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String opaId = UUID.randomUUID().toString();

      try {
        // when
        this.api.getOpaConfiguration(opaId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /configuration/opa/{opaId}.
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
      this.api.getOpaConfigurations();
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
    }
  }

  /**
   * PUT /configuration/opa.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPutOpaConfiguration() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      SetOpaConfigurationRequest request = new SetOpaConfigurationRequest();

      try {
        // when
        this.api.setOpaConfiguration(request);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/accessAttribute.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSetDocumentAccessAttributes() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      SetDocumentAccessAttributesRequest req = new SetDocumentAccessAttributesRequest();

      try {
        // when
        this.api.setDocumentAccessAttributes(this.documentId, req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }
}
