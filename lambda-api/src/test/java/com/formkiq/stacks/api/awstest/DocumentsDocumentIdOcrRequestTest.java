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
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddDocumentAction;
import com.formkiq.stacks.client.models.DocumentActionType;
import com.formkiq.stacks.client.models.DocumentActions;
import com.formkiq.stacks.client.models.DocumentOcr;
import com.formkiq.stacks.client.requests.AddDocumentActionRequest;
import com.formkiq.stacks.client.requests.AddDocumentOcrRequest;
import com.formkiq.stacks.client.requests.GetDocumentActionsRequest;
import com.formkiq.stacks.client.requests.GetDocumentOcrRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * GET, OPTIONS, POST, PUT, DELETE /documents/{documentId}/ocr tests.
 *
 */
public class DocumentsDocumentIdOcrRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 90000;

  /**
   * POST Document OCR.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testAddOcr01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // given
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        byte[] content = toBytes("/ocr/receipt.png");
        String documentId = addDocument(client, siteId, "receipt.png", content, "image/png");
        waitForDocumentContent(client, siteId, documentId);

        // when
        AddDocumentOcrRequest addReq =
            new AddDocumentOcrRequest().siteId(siteId).documentId(documentId);
        client.addDocumentOcr(addReq);

        // then
        DocumentOcr documentOcr = getDocumentOcr(client, siteId, documentId);
        assertTrue(documentOcr.data().contains("East Repair"));
      }
    }
  }

  /**
   * POST Document OCR using Actions.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testAddOcr02() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // given
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        byte[] content = toBytes("/ocr/receipt.png");
        String documentId = addDocument(client, siteId, "receipt.png", content, "image/png");
        waitForDocumentContent(client, siteId, documentId);

        // when
        AddDocumentActionRequest addReq =
            new AddDocumentActionRequest().siteId(siteId).documentId(documentId)
                .actions(Arrays.asList(new AddDocumentAction().type(DocumentActionType.OCR)));
        client.addDocumentAction(addReq);

        // then
        DocumentOcr documentOcr = getDocumentOcr(client, siteId, documentId);
        assertTrue(documentOcr.data().contains("East Repair"));

        GetDocumentActionsRequest getReq =
            new GetDocumentActionsRequest().siteId(siteId).documentId(documentId);
        DocumentActions actions = client.getDocumentActions(getReq);
        assertEquals(1, actions.actions().size());
        assertEquals("complete", actions.actions().get(0).status());
      }
    }
  }

  /**
   * Wait for {@link DocumentOcr} to have data.
   * 
   * @param client {@link FormKiqClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link DocumentOcr}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private DocumentOcr getDocumentOcr(final FormKiqClient client, final String siteId,
      final String documentId) throws IOException, InterruptedException {
    DocumentOcr documentOcr = null;
    GetDocumentOcrRequest getReq =
        new GetDocumentOcrRequest().siteId(siteId).documentId(documentId);

    while (documentOcr == null || documentOcr.data() == null) {
      try {
        documentOcr = client.getDocumentOcr(getReq);
      } catch (IOException e) {
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
