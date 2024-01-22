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
import com.formkiq.client.model.AddQueueRequest;
import com.formkiq.client.model.AddWorkflowRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /workflows. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class WorkflowRequestTest extends AbstractApiClientRequestTest {

  /**
   * POST /queues.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddQueues() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.addQueue(new AddQueueRequest(), siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * POST /workflows.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddWorkflows() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      AddWorkflowRequest req = new AddWorkflowRequest();
      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.addWorkflow(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * DELETE /queue/{queueId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteQueue() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.deleteQueue("1", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * DELETE /workflows/{workflowId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteWorkflow() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.deleteWorkflow(UUID.randomUUID().toString(), siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /queue/{queueId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetQueue() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.getQueue("1", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /queues/{queueId}/documents.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetQueueDocuments() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.getWorkflowQueueDocuments("1", siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /queues.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetQueues() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.getQueues(siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * Get /workflows/{workflowId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWorkflow() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.getWorkflow(UUID.randomUUID().toString(), siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * GET /workflows/{workflowId}/documents.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWorkflowDocuments() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.getWorkflowDocuments(UUID.randomUUID().toString(), siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * Get /workflows.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWorkflows() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      try {
        // when
        this.workflowApi.getWorkflows(siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
      }
    }
  }
}
