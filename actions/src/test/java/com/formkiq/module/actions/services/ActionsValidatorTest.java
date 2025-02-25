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

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationError;

@ExtendWith(DynamoDbExtension.class)
class ActionsValidatorTest {

  /** {@link ActionsValidator}. */
  private static ActionsValidator validator;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService db = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
    validator = new ActionsValidatorImpl(db);
  }

  @Test
  void testValidation01() {
    // given

    // when
    Collection<ValidationError> errors = validator.validation(null, (Action) null, null, null);

    // then
    assertEquals(1, errors.size());
    ValidationError error = errors.iterator().next();
    assertNull(error.key());
    assertEquals("action is required", error.error());
  }

  @Test
  void testValidation02() {
    // given
    Action action = new Action();

    // when
    Collection<ValidationError> errors = validator.validation(null, action, null, null);

    // then
    assertEquals(1, errors.size());
    ValidationError error = errors.iterator().next();
    assertEquals("type", error.key());
    assertEquals("action 'type' is required", error.error());
  }

  @Test
  void testValidation03() {
    // given
    Action action = new Action().type(ActionType.WEBHOOK).userId("joe");

    // when
    Collection<ValidationError> errors = validator.validation(null, action, null, null);

    // then
    assertEquals(1, errors.size());
    ValidationError error = errors.iterator().next();
    assertEquals("parameters.url", error.key());
    assertEquals("action 'url' parameter is required", error.error());
  }

  @Test
  void testValidation04() {
    // given
    Action action = new Action();
    List<Action> actions = List.of(action);

    // when
    List<Collection<ValidationError>> errorList = validator.validation(null, actions, null, null);

    // then
    assertEquals(1, errorList.size());

    Collection<ValidationError> errors = errorList.get(0);
    ValidationError error = errors.iterator().next();
    assertEquals("type", error.key());
    assertEquals("action 'type' is required", error.error());
  }

  @Test
  void testValidation05() {
    // given
    Action action = new Action().type(ActionType.OCR).userId("joe");

    // when
    Collection<ValidationError> errorList = validator.validation(null, action, null, null);

    // then
    assertEquals(0, errorList.size());
  }

  @Test
  void testValidation06() {
    // given
    Action action = new Action().type(ActionType.QUEUE).userId("joe");

    // when
    Collection<ValidationError> errors = validator.validation(null, action, null, null);

    // then
    assertEquals(1, errors.size());

    ValidationError error = errors.iterator().next();
    assertEquals("queueId", error.key());
    assertEquals("'queueId' is required", error.error());
  }

  @Test
  void testValidation07() {
    // given
    Action action = new Action().type(ActionType.QUEUE).queueId("Testqueue").userId("joe");

    // when
    Collection<ValidationError> errors = validator.validation(null, action, null, null);

    // then
    assertEquals(1, errors.size());

    ValidationError error = errors.iterator().next();
    assertEquals("queueId", error.key());
    assertEquals("'queueId' does not exist", error.error());
  }

  /**
   * Event Bridge Missing eventBusName.
   */
  @Test
  void testValidation08() {
    // given
    Action action = new Action().type(ActionType.EVENTBRIDGE).userId("joe");

    // when
    Collection<ValidationError> errors = validator.validation(null, action, null, null);

    // then
    assertEquals(1, errors.size());
    ValidationError error = errors.iterator().next();
    assertEquals("parameters.eventBusName", error.key());
    assertEquals("'eventBusName' parameter is required", error.error());
  }
}
