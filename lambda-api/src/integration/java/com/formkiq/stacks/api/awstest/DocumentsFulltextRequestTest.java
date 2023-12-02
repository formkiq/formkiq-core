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
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActionsComplete;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentOcrApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddAction.TypeEnum;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.GetDocumentFulltextResponse;
import com.formkiq.client.model.GetDocumentOcrResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * GET, OPTIONS, POST, PUT, DELETE /documents/{documentId}/fulltext tests.
 *
 */
public class DocumentsFulltextRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;

  /**
   * Action Fulltext with PDF.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddFulltextPdf01a() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      ApiClient client = getApiClients(siteId).get(0);

      byte[] content = toBytes("/ocr/sample.pdf");
      String documentId =
          addDocument(client, siteId, "sample.pdf", content, "application/pdf", null);
      waitForDocumentContent(client, siteId, documentId);

      DocumentActionsApi api = new DocumentActionsApi(client);
      AddDocumentActionsRequest req =
          new AddDocumentActionsRequest().addActionsItem(new AddAction().type(TypeEnum.FULLTEXT));

      // when
      api.addDocumentActions(documentId, siteId, req);

      // then
      waitForActionsComplete(client, siteId, documentId);

      DocumentOcrApi ocrApi = new DocumentOcrApi(client);
      GetDocumentOcrResponse response = ocrApi.getDocumentOcr(documentId, siteId, null, null, null);

      assertTrue(response.getData().contains("This is a small demonstration"));

      GetDocumentActionsResponse actions = api.getDocumentActions(documentId, siteId, null);
      assertEquals(2, actions.getActions().size());
      assertEquals("OCR", actions.getActions().get(0).getType().name());
      assertEquals("COMPLETE", actions.getActions().get(0).getStatus().name());
      assertEquals("FULLTEXT", actions.getActions().get(1).getType().name());
      assertEquals("COMPLETE", actions.getActions().get(1).getStatus().name());

      AdvancedDocumentSearchApi searchApi = new AdvancedDocumentSearchApi(client);
      GetDocumentFulltextResponse fullText =
          searchApi.getDocumentFulltext(documentId, siteId, null);
      assertTrue(fullText.getContent().contains("This is a small demonstration"));
    }
  }

  private byte[] toBytes(final String name) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(name)) {
      return IoUtils.toByteArray(is);
    }
  }
}
