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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActions;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.formkiq.client.model.DocumentAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentOcrApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.AddDocumentActionsResponse;
import com.formkiq.client.model.AddDocumentOcrRequest;
import com.formkiq.client.model.AddDocumentOcrResponse;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.GetDocumentOcrResponse;
import com.formkiq.client.model.OcrEngine;
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
    assertNotNull(documentOcr.getData());
    assertTrue(documentOcr.getData().contains("East Repair"));
  }

  private void addOcr02(final ApiClient client, final String siteId)
      throws IOException, InterruptedException, URISyntaxException, ApiException {
    byte[] content = toBytes("/ocr/receipt.png");
    String documentId = addDocument(client, siteId, "receipt.png", content, "image/png", null);
    waitForDocumentContent(client, siteId, documentId);

    DocumentActionsApi actionsApi = new DocumentActionsApi(client);
    AddDocumentActionsRequest req = new AddDocumentActionsRequest()
        .actions(List.of(new AddAction().type(DocumentActionType.OCR)));

    // when
    AddDocumentActionsResponse response = actionsApi.addDocumentActions(documentId, siteId, req);

    // then
    assertEquals("Actions saved", response.getMessage());
    waitForActions(client, siteId, documentId, List.of(DocumentActionStatus.COMPLETE));

    DocumentOcrApi api = new DocumentOcrApi(client);
    GetDocumentOcrResponse documentOcr =
        api.getDocumentOcr(documentId, siteId, null, null, null, null);
    assertNotNull(documentOcr.getData());
    assertTrue(documentOcr.getData().contains("East Repair"));

    List<DocumentAction> actions =
        notNull(actionsApi.getDocumentActions(documentId, siteId, null, null, null).getActions());
    assertEquals(1, actions.size());
    assertEquals(DocumentActionStatus.COMPLETE, Objects.requireNonNull(actions.get(0).getStatus()));
  }

  /**
   * Wait for {@link DocumentOcrApi} to have data.
   *
   * @param api {@link DocumentOcrApi}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentOcrResponse}
   * @throws InterruptedException InterruptedException
   * @throws ApiException ApiException
   */
  private GetDocumentOcrResponse getDocumentOcr(final DocumentOcrApi api, final String siteId,
      final String documentId) throws InterruptedException, ApiException {

    GetDocumentOcrResponse documentOcr =
        api.getDocumentOcr(documentId, siteId, null, null, null, null);

    while (documentOcr == null || documentOcr.getData() == null) {
      try {
        documentOcr = api.getDocumentOcr(documentId, siteId, null, null, null, null);
      } catch (ApiException e) {
        // ignore
      }
      TimeUnit.SECONDS.sleep(1);
    }

    return documentOcr;
  }

  /**
   * POST Document OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddOcr01a() throws Exception {
    ApiClient client = getApiClients(null).get(0);
    addOcr01(client, null);
  }

  /**
   * POST Document OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddOcr01b() throws Exception {
    String siteId = SITE_ID;
    ApiClient client = getApiClients(siteId).get(0);
    addOcr01(client, siteId);
  }

  /**
   * POST Document OCR using Actions.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddOcr02a() throws Exception {
    ApiClient client = getApiClients(null).get(0);
    addOcr02(client, null);
  }

  /**
   * POST Document OCR using Actions.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddOcr02b() throws Exception {

    String siteId = SITE_ID;
    ApiClient client = getApiClients(siteId).get(0);
    addOcr02(client, siteId);
  }

  /**
   * Test OCR PDF Document only first 2 pages.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddOcr03() throws Exception {
    ApiClient client = getApiClients(null).get(0);

    byte[] content = toBytes("/multipage_example.pdf");
    String documentId =
        addDocument(client, null, "multipage_example.pdf", content, "application/pdf", null);
    waitForDocumentContent(client, null, documentId);

    DocumentActionsApi actionsApi = new DocumentActionsApi(client);
    AddDocumentActionsRequest req = new AddDocumentActionsRequest()
        .actions(List.of(new AddAction().type(DocumentActionType.OCR).parameters(
            new AddActionParameters().ocrEngine(OcrEngine.TESSERACT).ocrNumberOfPages("2"))));

    // when
    AddDocumentActionsResponse response = actionsApi.addDocumentActions(documentId, null, req);

    // then
    assertEquals("Actions saved", response.getMessage());
    waitForActions(client, null, documentId, List.of(DocumentActionStatus.COMPLETE));

    DocumentOcrApi api = new DocumentOcrApi(client);
    GetDocumentOcrResponse documentOcr =
        api.getDocumentOcr(documentId, null, null, null, null, null);

    String text = documentOcr.getData();
    assertNotNull(text);
    assertTrue(text.contains("Your Company"));
    assertTrue(text.contains("2/9"));
    assertFalse(text.contains("3/9"));
    assertFalse(text.contains("Current process"));

    List<DocumentAction> actions =
        notNull(actionsApi.getDocumentActions(documentId, null, null, null, null).getActions());
    assertEquals(1, actions.size());
    assertEquals(DocumentActionStatus.COMPLETE, actions.get(0).getStatus());
  }

  private byte[] toBytes(final String name) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(name)) {
      assertNotNull(is);
      return IoUtils.toByteArray(is);
    }
  }
}
