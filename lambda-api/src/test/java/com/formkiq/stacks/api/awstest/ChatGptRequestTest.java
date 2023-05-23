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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddDocumentAction;
import com.formkiq.stacks.client.models.Config;
import com.formkiq.stacks.client.models.DocumentActionType;
import com.formkiq.stacks.client.models.DocumentTags;
import com.formkiq.stacks.client.requests.AddDocumentActionRequest;
import com.formkiq.stacks.client.requests.GetDocumentTagsRequest;
import com.formkiq.stacks.client.requests.UpdateConfigsRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * 
 * Integration Test for creating an OCR / ChatGPT actions.
 *
 */
public class ChatGptRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 120000;

  @BeforeClass
  public static void beforeClass() throws IOException {
    AbstractApiTest.beforeClass();

    if (System.getProperty("chatGptApiKey") == null) {
      throw new IOException("key not set");
    }

    FormKiqClientV1 client = getFormKiqClients().get(0);
    Config config = new Config();
    config.chatGptApiKey(System.getProperty("chatGptApiKey"));
    try {
      client.updateConfigs(new UpdateConfigsRequest().config(config));
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  /**
   * POST Document OCR using Actions.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOcrAndChatGpt01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // given
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        byte[] content = toBytes("/ocr/receipt.png");
        String documentId = addDocument(client, siteId, "receipt.png", content, "image/png");
        waitForDocumentContent(client, siteId, documentId);

        // when
        AddDocumentActionRequest addReq =
            new AddDocumentActionRequest().siteId(siteId).documentId(documentId)
                .actions(Arrays.asList(new AddDocumentAction().type(DocumentActionType.OCR),
                    new AddDocumentAction().type(DocumentActionType.DOCUMENTTAGGING)
                        .parameters(Map.of("engine", "chatgpt", "tags",
                            "organization,location,person,subject,sentiment,document type"))));
        client.addDocumentAction(addReq);

        // then
        waitForActionsComplete(client, siteId, documentId, DocumentActionType.DOCUMENTTAGGING);

        GetDocumentTagsRequest tagsReq =
            new GetDocumentTagsRequest().siteId(siteId).documentId(documentId);
        DocumentTags tags = client.getDocumentTags(tagsReq);
        assertTrue(
            tags.tags().stream().filter(r -> r.key().equals("untagged")).findFirst().isEmpty());

        assertTrue(tags.tags().stream().filter(r -> r.key().toLowerCase().equals("person"))
            .findFirst().isPresent());
      }
    }
  }

  private byte[] toBytes(final String name) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(name)) {
      return IoUtils.toByteArray(is);
    }
  }
}
