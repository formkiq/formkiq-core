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
package com.formkiq.stacks.api.awstest;

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActions;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentOcrApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddAction.TypeEnum;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.AddDocumentActionsResponse;
import com.formkiq.client.model.AddDocumentOcrRequest;
import com.formkiq.client.model.AddDocumentOcrResponse;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.GetDocumentOcrResponse;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.stacks.client.models.DocumentOcr;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * GET, OPTIONS, POST, PUT, DELETE /documents/{documentId}/ocr tests.
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocumentsDocumentIdOcrRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 180;

  /**
   * POST Document OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddOcr01a() throws Exception {
    String siteId = null;
    ApiClient client = getApiClients(siteId).get(0);
    addOcr01(client, siteId);
  }

  /**
   * POST Document OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddOcr01b() throws Exception {
    String siteId = SITE_ID;
    ApiClient client = getApiClients(siteId).get(0);
    addOcr01(client, siteId);
  }

  private void addOcr01(final ApiClient client, final String siteId)
      throws IOException, InterruptedException, URISyntaxException, ApiException {
    byte[] content = toBytes("/ocr/receipt.png");
    String documentId = addDocument(client, siteId, "receipt.png", content, "image/png", null);
    waitForDocumentContent(client, siteId, documentId);

    DocumentOcrApi api = new DocumentOcrApi(client);
    AddDocumentOcrRequest req = new AddDocumentOcrRequest();

    // when
    AddDocumentOcrResponse response = api.addDocumentOcr(documentId, siteId, req);

    // then
    assertEquals("OCR request submitted", response.getMessage());

    GetDocumentOcrResponse documentOcr = getDocumentOcr(api, siteId, documentId);
    assertTrue(documentOcr.getData().contains("East Repair"));
  }

  /**
   * POST Document OCR using Actions.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddOcr02a() throws Exception {

    String siteId = null;
    ApiClient client = getApiClients(siteId).get(0);
    addOcr02(client, siteId);
  }

  /**
   * POST Document OCR using Actions.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddOcr02b() throws Exception {

    String siteId = SITE_ID;
    ApiClient client = getApiClients(siteId).get(0);
    addOcr02(client, siteId);
  }

  private void addOcr02(final ApiClient client, final String siteId)
      throws IOException, InterruptedException, URISyntaxException, ApiException {
    byte[] content = toBytes("/ocr/receipt.png");
    String documentId = addDocument(client, siteId, "receipt.png", content, "image/png", null);
    waitForDocumentContent(client, siteId, documentId);

    DocumentActionsApi actionsApi = new DocumentActionsApi(client);
    AddDocumentActionsRequest req =
        new AddDocumentActionsRequest().actions(Arrays.asList(new AddAction().type(TypeEnum.OCR)));

    // when
    AddDocumentActionsResponse response = actionsApi.addDocumentActions(documentId, siteId, req);

    // then
    assertEquals("Actions saved", response.getMessage());
    waitForActions(client, siteId, documentId, ActionStatus.COMPLETE.name());

    DocumentOcrApi api = new DocumentOcrApi(client);
    GetDocumentOcrResponse documentOcr = api.getDocumentOcr(documentId, siteId, null, null, null);
    assertTrue(documentOcr.getData().contains("East Repair"));

    GetDocumentActionsResponse actions = actionsApi.getDocumentActions(documentId, siteId, null);
    assertEquals(1, actions.getActions().size());
    assertEquals("COMPLETE", actions.getActions().get(0).getStatus().name());
  }

  /**
   * Wait for {@link DocumentOcr} to have data.
   *
   * @param api {@link DocumentOcrApi}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentOcrResponse}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws ApiException ApiException
   */
  private GetDocumentOcrResponse getDocumentOcr(final DocumentOcrApi api, final String siteId,
      final String documentId) throws IOException, InterruptedException, ApiException {

    GetDocumentOcrResponse documentOcr = api.getDocumentOcr(documentId, siteId, null, null, null);

    while (documentOcr == null || documentOcr.getData() == null) {
      try {
        documentOcr = api.getDocumentOcr(documentId, siteId, null, null, null);
      } catch (ApiException e) {
        // ignore
      }
      TimeUnit.SECONDS.sleep(1);
    }

    return documentOcr;
  }

  private byte[] toBytes(final String name) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(name)) {
      return IoUtils.toByteArray(is);
    }
  }
}
