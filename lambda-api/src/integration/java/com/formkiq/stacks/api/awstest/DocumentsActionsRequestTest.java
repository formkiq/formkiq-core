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

import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.api.DocumentAttributesApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddActionParameters.NotificationTypeEnum;
import com.formkiq.client.model.AddDocumentActionsRetryResponse;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentsResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.testutils.FileGenerator;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.nimbusds.jose.util.StandardCharset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import software.amazon.awssdk.utils.IoUtils;


import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForAction;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActions;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActionsComplete;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GET, POST /documents/{documentId}/actions tests.
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocumentsActionsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;
  /** Actions Event Bus. */
  private static String actionsEventBus;
  /** Actions Event Queue. */
  private static String actionsEventQueue;
  /** {@link FileGenerator}. */
  private final FileGenerator fileGenerator = new FileGenerator();

  @BeforeAll
  public static void setup() {
    actionsEventBus = getSsm().getParameterValue(
        "/formkiq/" + getAppenvironment() + "/eventbridge/actions-event-bus/name");
    actionsEventQueue = getSsm()
        .getParameterValue("/formkiq/" + getAppenvironment() + "/sqs/actions-event-queue/url");

    getSqs().clearQueue(actionsEventQueue);
  }

  /**
   * POST /documents/{documentId}.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddDocumentActions01() throws Exception {
    // given
    List<ApiClient> clients = getApiClients(null);
    ApiClient client = clients.get(0);
    byte[] data = "somedata".getBytes(StandardCharset.UTF_8);

    List<AddAction> actions =
        List.of(new AddAction().type(DocumentActionType.QUEUE).queueId("test"));
    List<AddDocumentTag> tags = Collections.emptyList();

    // when
    try {
      addDocument(client, null, "data.txt", data, MimeType.MIME_PLAIN_TEXT.getContentType(),
          actions, tags);
    } catch (ApiException e) {
      // then
      assertEquals("{\"errors\":[{\"key\":\"queueId\",\"error\":\"'queueId' does not exist\"}]}",
          e.getResponseBody());
    }
  }

  /**
   * POST Document Action Event Bridge.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddDocumentActions02() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    List<ApiClient> clients = getApiClients(siteId);
    ApiClient client = clients.get(0);

    String content = "this is a test";
    AddAction addAction = new AddAction().type(DocumentActionType.EVENTBRIDGE)
        .parameters(new AddActionParameters().eventBusName(actionsEventBus));
    List<AddAction> actions = List.of(addAction, addAction);

    // when
    String documentId = addDocument(client, siteId, "test.txt", content, "text/plain", actions);

    // then
    GetDocumentActionsResponse response = waitForActionsComplete(client, siteId, documentId);
    List<DocumentAction> docActions = notNull(response.getActions());
    assertEquals(2, docActions.size());
    assertEquals(DocumentActionStatus.COMPLETE, docActions.get(0).getStatus());
    assertNotNull(docActions.get(0).getStartDate());
    assertNotNull(docActions.get(0).getInsertedDate());
    assertNotNull(docActions.get(0).getCompletedDate());
    assertEquals(DocumentActionStatus.COMPLETE, docActions.get(1).getStatus());

    assertEventBridgeMessage(actionsEventQueue);
  }

  private static void assertEventBridgeMessage(final String queueUrl) throws InterruptedException {

    List<Message> receiveMessages;

    do {
      receiveMessages = getSqs().receiveMessages(queueUrl).messages();
      if (receiveMessages.isEmpty()) {
        TimeUnit.SECONDS.sleep(1);
      }

    } while (receiveMessages.isEmpty());

    Gson gson = new GsonBuilder().create();
    String body = receiveMessages.iterator().next().body();
    Map<String, Object> map = gson.fromJson(body, Map.class);
    assertTrue(map.containsKey("detail"));
    Map<String, Object> detail = (Map<String, Object>) map.get("detail");
    assertTrue(detail.containsKey("documents"));

    getSqs().clearQueue(queueUrl);
  }

  /**
   * POST /documents/{documentId}/actions with resize.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddDocumentActions03() throws Exception {
    // given
    List<ApiClient> clients = getApiClients(null);
    ApiClient client = clients.get(0);

    try (InputStream is = LambdaContextRecorder.class.getResourceAsStream("/input.gif")) {
      assertNotNull(is);
      byte[] data = IoUtils.toByteArray(is);

      List<AddAction> actions = List.of(new AddAction().type(DocumentActionType.RESIZE)
          .parameters(new AddActionParameters().width("100").height("auto")));
      List<AddDocumentTag> tags = Collections.emptyList();

      // when
      String documentId = addDocument(client, null, "input.gif", data,
          MimeType.MIME_GIF.getContentType(), actions, tags);

      // then
      waitForActionsComplete(client, null, documentId);

      DocumentAttributesApi documentAttributesApi = new DocumentAttributesApi(client);
      List<DocumentAttribute> documentAttributes = notNull(documentAttributesApi
          .getDocumentAttributes(documentId, null, null, null).getAttributes());
      assertEquals(1, documentAttributes.size());
      DocumentAttribute documentAttribute = documentAttributes.get(0);
      assertEquals("Relationships", documentAttribute.getKey());
      assertEquals(AttributeValueType.STRING, documentAttribute.getValueType());
      assertNotNull(documentAttribute.getStringValue());
      assertTrue(documentAttribute.getStringValue().startsWith("RENDITION#"));

      String renditionDocId = documentAttribute.getStringValue().substring("RENDITION#".length());
      DocumentsApi api = new DocumentsApi(client);
      GetDocumentResponse item = api.getDocument(renditionDocId, null, null);
      assertEquals("100", item.getWidth());
      assertEquals("56", item.getHeight());
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddDocumentActionsRetry01() throws Exception {
    // given
    List<ApiClient> clients = getApiClients(null);
    ApiClient client = clients.get(0);
    final long targetSize = 1024;
    File file = this.fileGenerator.generateZipFile(targetSize);
    byte[] data = Files.readAllBytes(file.toPath());

    List<AddAction> actions = List.of(new AddAction().type(DocumentActionType.OCR));
    List<AddDocumentTag> tags = Collections.emptyList();

    // when
    String documentId = addDocument(client, null, "retry.docx", data,
        MimeType.MIME_DOCX.getContentType(), actions, tags);

    // then
    GetDocumentActionsResponse response =
        waitForActions(client, null, documentId, List.of(DocumentActionStatus.FAILED));
    assertEquals(1, notNull(response.getActions()).size());
    assertEquals("FAILED", Objects.requireNonNull(response.getActions().get(0).getStatus()).name());

    // given
    DocumentActionsApi actionsApi = new DocumentActionsApi(client);

    // when
    AddDocumentActionsRetryResponse retryResponse =
        actionsApi.addDocumentRetryAction(documentId, null);

    // then
    assertEquals("Actions retrying", retryResponse.getMessage());

    response = waitForAction(client, null, documentId, List.of(DocumentActionStatus.FAILED));
    assertEquals(2, notNull(response.getActions()).size());
    assertEquals("FAILED_RETRY",
        Objects.requireNonNull(response.getActions().get(0).getStatus()).name());
    assertEquals("FAILED", Objects.requireNonNull(response.getActions().get(1).getStatus()).name());

    DocumentsApi docApi = new DocumentsApi(client);

    GetDocumentsResponse documents =
        docApi.getDocuments(null, "FAILED", null, null, null, null, null, null, "100");
    Optional<Document> o = notNull(documents.getDocuments()).stream()
        .filter(d -> documentId.equals(d.getDocumentId())).findAny();
    assertFalse(o.isEmpty());

    documents =
        docApi.getDocuments(null, "FAILED_RETRY", null, null, null, null, null, null, "100");
    o = notNull(documents.getDocuments()).stream().filter(d -> documentId.equals(d.getDocumentId()))
        .findAny();
    assertFalse(o.isEmpty());
  }

  /**
   * POST Document Notifications.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
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
    List<AddAction> actions = List.of(new AddAction().type(DocumentActionType.NOTIFICATION)
        .parameters(new AddActionParameters().notificationType(NotificationTypeEnum.EMAIL)
            .notificationSubject(subject).notificationToCc("mfriesen@gmail.com")
            .notificationText(text)));

    // when
    String documentId = addDocument(client, siteId, "test.txt", content, "text/plain", actions);

    // then
    GetDocumentActionsResponse response = waitForActionsComplete(client, siteId, documentId);
    List<DocumentAction> docActions = notNull(response.getActions());
    assertEquals(1, docActions.size());
    assertEquals("COMPLETE", Objects.requireNonNull(docActions.get(0).getStatus()).name());
    assertNotNull(docActions.get(0).getStartDate());
    assertNotNull(docActions.get(0).getInsertedDate());
    assertNotNull(docActions.get(0).getCompletedDate());
  }
}
