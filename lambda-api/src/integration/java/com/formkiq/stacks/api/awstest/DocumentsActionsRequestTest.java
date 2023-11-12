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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddAction.TypeEnum;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddActionParameters.NotificationTypeEnum;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * GET, POST /documents/{documentId}/actions tests.
 *
 */
public class DocumentsActionsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 90;

  /**
   * POST Document Notifications.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddNotifications01() throws Exception {
    // given
    String siteId = null;
    List<ApiClient> clients = getApiClients(siteId);
    ApiClient client = clients.get(0);

    String adminEmail =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/console/AdminEmail");
    UpdateConfigurationRequest req = new UpdateConfigurationRequest().notificationEmail(adminEmail);

    SystemManagementApi api = new SystemManagementApi(client);
    api.updateConfiguration(req, siteId);

    String content = "this is a test";
    String subject = "Test email";
    String text = "This is a text email";
    List<AddAction> actions = Arrays.asList(new AddAction().type(TypeEnum.NOTIFICATION)
        .parameters(new AddActionParameters().notificationType(NotificationTypeEnum.EMAIL)
            .notificationSubject(subject).notificationToCc("mfriesen@gmail.com")
            .notificationText(text)));

    // when
    String documentId = addDocument(client, siteId, "test.txt", content, "text/plain", actions);

    // then
    GetDocumentActionsResponse response = waitForActionsComplete(client, siteId, documentId);
    assertEquals(1, response.getActions().size());
    assertEquals("COMPLETE", response.getActions().get(0).getStatus().name());
    assertNotNull(response.getActions().get(0).getInsertedDate());
    assertNotNull(response.getActions().get(0).getCompletedDate());
  }
}
