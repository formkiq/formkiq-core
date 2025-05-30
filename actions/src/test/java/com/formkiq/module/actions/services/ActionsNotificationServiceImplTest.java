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
package com.formkiq.module.actions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.LogType;
import com.formkiq.module.lambdaservices.logger.LoggerImpl;
import org.junit.jupiter.api.Test;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.events.EventServiceMock;
import com.formkiq.module.events.document.DocumentEvent;

/**
 * Unit Tests for {@link ActionsNotificationServiceImpl}.
 */
public class ActionsNotificationServiceImplTest {

  /** {@link EventServiceMock}. */
  private static final EventServiceMock ES = new EventServiceMock();
  /** {@link ActionsNotificationService}. */
  private static final ActionsNotificationService SERVICE =
      new ActionsNotificationServiceImpl(new LoggerImpl(LogLevel.INFO, LogType.TEXT), ES);

  /**
   * Test adding pending action.
   */
  @Test
  void publishNextActionEvent01() {
    // given
    String documentId = ID.uuid();
    List<Action> actions = List.of(new Action().documentId(documentId).type(ActionType.OCR));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      SERVICE.publishNextActionEvent(actions, siteId, documentId);

      // then
      assertEquals(1, ES.getDocumentEvents().size());
      DocumentEvent e = ES.getDocumentEvents().get(0);
      assertEquals(documentId, e.documentId());
      assertEquals("actions", e.type());

      ES.getDocumentEvents().clear();
    }
  }

  /**
   * Test adding failing action.
   */
  @Test
  void publishNextActionEvent02() {
    // given
    String documentId = ID.uuid();
    List<Action> actions = List
        .of(new Action().documentId(documentId).type(ActionType.OCR).status(ActionStatus.FAILED));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      SERVICE.publishNextActionEvent(actions, siteId, documentId);

      // then
      assertEquals(0, ES.getDocumentEvents().size());
    }
  }

  /**
   * Test adding running action.
   */
  @Test
  void publishNextActionEvent03() {
    // given
    String documentId = ID.uuid();
    List<Action> actions = Arrays.asList(
        new Action().documentId(documentId).type(ActionType.OCR).status(ActionStatus.RUNNING),
        new Action().documentId(documentId).type(ActionType.OCR).status(ActionStatus.PENDING));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      SERVICE.publishNextActionEvent(actions, siteId, documentId);

      // then
      assertEquals(0, ES.getDocumentEvents().size());
    }
  }
}
