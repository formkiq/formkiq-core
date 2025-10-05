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
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentTag;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.formkiq.client.model.DocumentTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddActionParameters.EngineEnum;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.UpdateConfigurationRequest;
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
  public static void beforeClassSetup() throws IOException {

    if (System.getProperty("testchatgptapikey") == null) {
      throw new IOException("key not set");
    }

    try {
      String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
      ApiClient apiClient = getApiClients(siteId).get(0);
      SystemManagementApi api = new SystemManagementApi(apiClient);

      api.updateConfiguration(siteId,
          new UpdateConfigurationRequest().chatGptApiKey(System.getProperty("testchatgptapikey")));
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
  @Timeout(value = TEST_TIMEOUT)
  public void testOcrAndChatGpt01() throws Exception {
    // given
    List<ApiClient> clients = getApiClients(null);
    ApiClient client = clients.get(0);

    byte[] content = toBytes();

    String documentId = addDocument(client, null, "receipt.png", content, "image/png", null);
    waitForDocumentContent(client, null, documentId);

    DocumentActionsApi api = new DocumentActionsApi(client);
    String actionTags = "organization,location,person,subject,sentiment,document type";

    // when
    api.addDocumentActions(documentId, null,
        new AddDocumentActionsRequest()
            .actions(Arrays.asList(new AddAction().type(DocumentActionType.OCR),
                new AddAction().type(DocumentActionType.DOCUMENTTAGGING).parameters(
                    new AddActionParameters().engine(EngineEnum.CHATGPT).tags(actionTags)))));

    // then
    waitForActions(client, null, documentId, List.of(DocumentActionStatus.COMPLETE));

    DocumentTagsApi tagsApi = new DocumentTagsApi(client);

    List<DocumentTag> tags1 =
        notNull(tagsApi.getDocumentTags(documentId, null, null, null, null, null).getTags());
    assertFalse(tags1.isEmpty());

    waitForDocumentTag(client, null, documentId, "organization");
    List<DocumentTag> tags =
        notNull(tagsApi.getDocumentTags(documentId, null, null, null, null, null).getTags());

    assertTrue(tags.stream().filter(r -> "untagged".equals(r.getKey())).findFirst().isEmpty());

    assertTrue(tags.stream().anyMatch(r -> "organization".equalsIgnoreCase(r.getKey())));
  }

  private byte[] toBytes() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/ocr/receipt.png")) {
      assertNotNull(is);
      return IoUtils.toByteArray(is);
    }
  }
}
