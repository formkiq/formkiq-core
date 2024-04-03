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
import static com.formkiq.testutils.aws.FkqDocumentService.waitForAction;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActions;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActionsComplete;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddActionParameters.NotificationTypeEnum;
import com.formkiq.client.model.AddDocumentActionsRetryResponse;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.GetDocumentsResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.testutils.FileGenerator;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import com.nimbusds.jose.util.StandardCharset;

/**
 * GET, POST /documents/{documentId}/actions tests.
 *
 */
public class DocumentsActionsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 90;
  /** {@link FileGenerator}. */
  private FileGenerator fileGenerator = new FileGenerator();

  /**
   * POST /documents/{documentId}.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testaddDocumentActions01() throws Exception {
    // given
    String siteId = null;
    List<ApiClient> clients = getApiClients(siteId);
    ApiClient client = clients.get(0);
    byte[] data = "somedata".getBytes(StandardCharset.UTF_8);

    List<AddAction> actions =
        Arrays.asList(new AddAction().type(DocumentActionType.QUEUE).queueId("test"));
    List<AddDocumentTag> tags = Collections.emptyList();

    // when
    try {
      addDocument(client, siteId, "data.txt", data, MimeType.MIME_PLAIN_TEXT.getContentType(),
          actions, tags);
    } catch (ApiException e) {
      // then
      assertEquals("{\"errors\":[{\"key\":\"queueId\",\"error\":\"'queueId' does not exist\"}]}",
          e.getResponseBody());
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddDocumentActionsRetry01() throws Exception {
    // given
    String siteId = null;
    List<ApiClient> clients = getApiClients(siteId);
    ApiClient client = clients.get(0);
    final long targetSize = 1024;
    File file = this.fileGenerator.generateZipFile(targetSize);
    byte[] data = Files.readAllBytes(file.toPath());

    List<AddAction> actions = Arrays.asList(new AddAction().type(DocumentActionType.OCR));
    List<AddDocumentTag> tags = Collections.emptyList();

    // when
    String documentId = addDocument(client, siteId, "retry.docx", data,
        MimeType.MIME_DOCX.getContentType(), actions, tags);

    // then
    GetDocumentActionsResponse response =
        waitForActions(client, siteId, documentId, Arrays.asList(DocumentActionStatus.FAILED));
    assertEquals(1, response.getActions().size());
    assertEquals("FAILED", response.getActions().get(0).getStatus().name());

    // given
    DocumentActionsApi actionsApi = new DocumentActionsApi(client);

    // when
    AddDocumentActionsRetryResponse retryResponse =
        actionsApi.addDocumentRetryAction(documentId, siteId);

    // then
    assertEquals("Actions retrying", retryResponse.getMessage());

    response =
        waitForAction(client, siteId, documentId, Arrays.asList(DocumentActionStatus.FAILED));
    assertEquals(2, response.getActions().size());
    assertEquals("FAILED_RETRY", response.getActions().get(0).getStatus().name());
    assertEquals("FAILED", response.getActions().get(1).getStatus().name());

    DocumentsApi docApi = new DocumentsApi(client);

    GetDocumentsResponse documents =
        docApi.getDocuments(siteId, "FAILED", null, null, null, null, null, "100");
    Optional<Document> o = documents.getDocuments().stream()
        .filter(d -> d.getDocumentId().equals(documentId)).findAny();
    assertFalse(o.isEmpty());

    documents = docApi.getDocuments(siteId, "FAILED_RETRY", null, null, null, null, null, "100");
    o = documents.getDocuments().stream().filter(d -> d.getDocumentId().equals(documentId))
        .findAny();
    assertFalse(o.isEmpty());
  }

  /**
   * POST Document Notifications.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddNotifications01() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    List<ApiClient> clients = getApiClients(siteId);
    ApiClient client = clients.get(0);

    String adminEmail =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/console/AdminEmail");
    UpdateConfigurationRequest req = new UpdateConfigurationRequest().notificationEmail(adminEmail);

    SystemManagementApi api = new SystemManagementApi(client);
    api.updateConfiguration(siteId, req);

    String content = "this is a test";
    String subject = "Test email";
    String text = "This is a text email";
    List<AddAction> actions = Arrays.asList(new AddAction().type(DocumentActionType.NOTIFICATION)
        .parameters(new AddActionParameters().notificationType(NotificationTypeEnum.EMAIL)
            .notificationSubject(subject).notificationToCc("mfriesen@gmail.com")
            .notificationText(text)));

    // when
    String documentId = addDocument(client, siteId, "test.txt", content, "text/plain", actions);

    // then
    GetDocumentActionsResponse response = waitForActionsComplete(client, siteId, documentId);
    assertEquals(1, response.getActions().size());
    assertEquals("COMPLETE", response.getActions().get(0).getStatus().name());
    assertNotNull(response.getActions().get(0).getStartDate());
    assertNotNull(response.getActions().get(0).getInsertedDate());
    assertNotNull(response.getActions().get(0).getCompletedDate());
  }
}
