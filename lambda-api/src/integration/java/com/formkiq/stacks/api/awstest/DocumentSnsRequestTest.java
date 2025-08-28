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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentRequestBuilder;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import com.formkiq.testutils.aws.sqs.SqsMessageReceiver;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration Test for Document Sns.
 */
class DocumentSnsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;

  /** {@link Gson}. */
  private static final Gson GSON = new Gson();
  /** Document Sns Queue. */
  private static String documentSnsQueue;

  @BeforeAll
  public static void setup() {
    documentSnsQueue =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/sqs/sns-queue/url");
    getSqs().clearQueue(documentSnsQueue);
  }

  @BeforeEach
  public void setupTest() {
    getSqs().clearQueue(documentSnsQueue);
  }

  @AfterAll
  public static void teardown() {
    getSqs().clearQueue(documentSnsQueue);
  }

  /**
   * Test Add Document.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddDocument() throws Exception {
    // given
    SqsMessageReceiver receiver = getSqsMessageReceiver();
    ApiClient client = getApiClients(null).get(0);
    String path = ID.uuid() + ".txt";

    // when
    String documentId = new AddDocumentRequestBuilder().content().path(path).submit(client, null)
        .response().getDocumentId();

    // then
    Message message = getMessage(receiver);
    assertMessage(message, "create", documentId, path);

    // when
    new DeleteDocumentRequestBuilder(documentId).softDelete(true).submit(client, null);

    // then
    message = getMessage(receiver);
    assertMessage(message, "softDelete", documentId, path);

    // when
    new DeleteDocumentRequestBuilder(documentId).submit(client, null);

    // then
    message = getMessage(receiver);
    assertMessage(message, "delete", documentId, path);
  }

  private void assertMessage(final Message message, final String type, final String documentId,
      final String path) {
    Map<String, Object> map = getDocument(message);
    assertEquals(type, map.get("type"));
    assertEquals("default", map.get("siteId"));
    assertEquals(path, map.get("path"));
    assertEquals(documentId, map.get("documentId"));
    assertNotNull(map.get("url"));
  }

  private static SqsMessageReceiver getSqsMessageReceiver() {
    String eventBridgeQueueUrl =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/sqs/sns-queue/url");
    return new SqsMessageReceiver(getSqs(), eventBridgeQueueUrl);
  }

  private static Message getMessage(final SqsMessageReceiver receiver) throws InterruptedException {
    List<Message> messages = receiver.get().messages();
    assertEquals(1, messages.size());
    return messages.get(0);
  }

  private Map<String, Object> getDocument(final Message message) {
    return GSON.fromJson(message.body(), Map.class);
  }
}
