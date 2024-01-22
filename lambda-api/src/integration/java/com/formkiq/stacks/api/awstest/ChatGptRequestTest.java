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
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentTag;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddAction.TypeEnum;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddActionParameters.EngineEnum;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.GetDocumentTagsResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * 
 * Integration Test for creating an OCR / ChatGPT actions.
 *
 */
public class ChatGptRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 120;

  @BeforeAll
  public static void beforeClass() throws IOException, InterruptedException, URISyntaxException {
    AbstractAwsIntegrationTest.beforeClass();

    if (System.getProperty("testchatgptapikey") == null) {
      throw new IOException("key not set");
    }

    try {
      ApiClient apiClient = getApiClients(null).get(0);
      SystemManagementApi api = new SystemManagementApi(apiClient);

      api.updateConfiguration(
          new UpdateConfigurationRequest().chatGptApiKey(System.getProperty("testchatgptapikey")),
          null);
    } catch (ApiException e) {
      throw new IOException(e);
    }
  }

  /**
   * POST Document OCR using Actions.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testOcrAndChatGpt01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      List<ApiClient> clients = getApiClients(siteId);
      ApiClient client = clients.get(0);

      byte[] content = toBytes("/ocr/receipt.png");

      String documentId = addDocument(client, siteId, "receipt.png", content, "image/png", null);
      waitForDocumentContent(client, siteId, documentId);

      DocumentActionsApi api = new DocumentActionsApi(client);
      String actionTags = "organization,location,person,subject,sentiment,document type";

      // when
      api.addDocumentActions(documentId, siteId,
          new AddDocumentActionsRequest().actions(Arrays.asList(new AddAction().type(TypeEnum.OCR),
              new AddAction().type(TypeEnum.DOCUMENTTAGGING).parameters(
                  new AddActionParameters().engine(EngineEnum.CHATGPT).tags(actionTags)))));

      // then
      waitForActions(client, siteId, documentId, ActionStatus.COMPLETE.name());

      DocumentTagsApi tagsApi = new DocumentTagsApi(client);

      waitForDocumentTag(client, siteId, documentId, "organization");
      GetDocumentTagsResponse tags =
          tagsApi.getDocumentTags(documentId, siteId, null, null, null, null);

      assertTrue(
          tags.getTags().stream().filter(r -> r.getKey().equals("untagged")).findFirst().isEmpty());

      assertTrue(tags.getTags().stream()
          .filter(r -> r.getKey().toLowerCase().equals("organization")).findFirst().isPresent());
    }
  }

  private byte[] toBytes(final String name) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(name)) {
      return IoUtils.toByteArray(is);
    }
  }
}
