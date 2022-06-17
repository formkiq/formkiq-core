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
package com.formkiq.module.actions;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/** Unit Tests for {@link ActionsServiceDynamoDbTest}. */
@ExtendWith(DynamoDbExtension.class)
public class ActionsServiceDynamoDbTest {

  /** {@link ActionsService}. */
  private static ActionsService service;

  /**
   * BeforeAll.
   * 
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {

    DynamoDbConnectionBuilder db = DynamoDbTestServices.getDynamoDbConnection(null);
    service = new ActionsServiceDynamoDb(db, DOCUMENTS_TABLE);
  }

  /**
   * Test Action.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSave01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String userId0 = "joe";
      String userId1 = "jane";
      String documentId0 = UUID.randomUUID().toString();
      String documentId1 = UUID.randomUUID().toString();

      Action action0 = new Action().type(ActionType.OCR).userId(userId0);
      Action action1 =
          new Action().type(ActionType.OCR).userId(userId1).status(ActionStatus.COMPLETE);

      // when
      service.saveActions(siteId, documentId0, Arrays.asList(action0));
      service.saveActions(siteId, documentId1, Arrays.asList(action1));

      // then
      List<Action> results = service.getActions(siteId, documentId0);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.PENDING, results.get(0).status());
      assertEquals(ActionType.OCR, results.get(0).type());
      assertEquals(userId0, results.get(0).userId());

      results = service.getActions(siteId, documentId1);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.COMPLETE, results.get(0).status());
      assertEquals(ActionType.OCR, results.get(0).type());
      assertEquals(userId1, results.get(0).userId());

      // given
      action0.status(ActionStatus.FAILED);

      // when
      service.updateActionStatus(siteId, documentId0, action0, 0);

      // then
      results = service.getActions(siteId, documentId0);
      assertEquals(ActionStatus.FAILED, results.get(0).status());
    }
  }
}
