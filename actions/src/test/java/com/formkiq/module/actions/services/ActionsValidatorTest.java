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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.module.actions.ActionBuilder;
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
  /** Valid Image Formats. */
  private static final List<String> VALID_IMAGE_FORMATS =
      List.of("bmp", "gif", "jpeg", "png", "tif", "BmP");

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

  private static void testDimensionTemplate(final Map<String, Object> parameters,
      final String errorKey, final String errorMessage) {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().document(document).indexUlid().type(ActionType.RESIZE)
        .userId("joe").parameters(parameters).build((String) null);
    testTemplate(action, errorKey, errorMessage);
  }

  private static void testTemplate(final Action action, final String errorKey,
      final String errorMessage) {
    // when
    Collection<ValidationError> errors = validator.validation(null, action, null, null);

    // then
    boolean shouldHaveError = errorMessage != null;
    assertEquals(shouldHaveError ? 1 : 0, errors.size());

    if (shouldHaveError) {
      ValidationError error = errors.iterator().next();
      assertEquals(errorKey, error.key());
      assertEquals(errorMessage, error.error());
    }
  }

  @Test
  void testAutoWidthAndHeight() {
    testDimensionTemplate(Map.of("height", "auto", "width", "auto"), "parameters.width",
        "'width' and 'height' parameters cannot be both set to auto");
  }

  @Test
  void testInvalidHeight() {
    testDimensionTemplate(Map.of("width", "auto", "height", "invalidValue"), "parameters.height",
        "'height' parameter must be an integer > 0 or 'auto'");
  }

  @Test
  void testInvalidImageFormat() {
    testDimensionTemplate(Map.of("height", "100", "width", "100", "outputType", "invalid"),
        "parameters.outputType",
        "'outputType' parameter must be one of [bmp, gif, jpeg, png, tif]");
  }

  @Test
  void testInvalidWidth() {
    testDimensionTemplate(Map.of("height", "auto", "width", "invalidValue"), "parameters.width",
        "'width' parameter must be an integer > 0 or 'auto'");
  }

  @Test
  void testNoHeight() {
    testDimensionTemplate(Map.of("width", "auto"), "parameters.height",
        "'height' parameter is required");
  }

  @Test
  void testNoWidth() {
    testDimensionTemplate(Map.of("height", "auto"), "parameters.width",
        "'width' parameter is required");
  }

  @Test
  void testNumericWidthAndHeight() {
    testDimensionTemplate(Map.of("height", "100", "width", "100"), null, null);
  }

  @Test
  void testValidImageFormats() {
    for (String format : VALID_IMAGE_FORMATS) {
      testDimensionTemplate(Map.of("height", "100", "width", "100", "outputType", format), null,
          null);
    }
  }

  @Test
  void testValidation01() {
    testTemplate(null, null, "action is required");
  }

  @Test
  void testValidation02() {
    Action action = new Action(null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
    testTemplate(action, "type", "action 'type' is required");
  }

  @Test
  void testValidation03() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.WEBHOOK).userId("joe").document(document)
        .indexUlid().build((String) null);
    testTemplate(action, "parameters.url", "action 'url' parameter is required");
  }

  @Test
  void testValidation04() {
    // given
    Action action = new Action(null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
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
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.OCR).document(document).indexUlid()
        .userId("joe").build((String) null);
    testTemplate(action, null, null);
  }

  @Test
  void testValidation06() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.QUEUE).document(document).indexUlid()
        .userId("joe").build((String) null);
    testTemplate(action, "queueId", "'queueId' is required");
  }

  @Test
  void testValidation07() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.QUEUE).document(document).indexUlid()
        .queueId("Testqueue").userId("joe").build((String) null);
    testTemplate(action, "queueId", "'queueId' does not exist");
  }

  /**
   * Event Bridge Missing eventBusName.
   */
  @Test
  void testValidation08() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.EVENTBRIDGE).userId("joe")
        .document(document).indexUlid().build((String) null);
    testTemplate(action, "parameters.eventBusName", "'eventBusName' parameter is required");
  }

  /**
   * DATA_CLASSIFICATION Missing llmPromptEntityName.
   */
  @Test
  void testValidation09() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.DATA_CLASSIFICATION).userId("joe")
        .document(document).indexUlid().build((String) null);
    testTemplate(action, "parameters.llmPromptEntityName",
        "'llmPromptEntityName' parameter is required");
  }

  /**
   * METADATA_EXTRACTION Missing llmPromptEntityName.
   */
  @Test
  void testValidation10() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.METADATA_EXTRACTION).userId("joe")
        .document(document).indexUlid().build((String) null);
    testTemplate(action, "parameters.llmPromptEntityName",
        "'llmPromptEntityName' parameter is required");
  }

  @Test
  void testValidation11() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.CHECKSUM).userId("joe").document(document)
        .indexUlid().build((String) null);
    testTemplate(action, "parameters.checksumType", "'checksumType' parameter is required");
  }

  @Test
  void testValidation12() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.CHECKSUM).userId("joe")
        .parameters(Map.of("checksumType", "md5")).document(document).indexUlid()
        .build((String) null);
    testTemplate(action, "parameters.checksumType",
        "'checksumType' parameter must be one of [SHA1, SHA256, SHA512]");
  }

  @Test
  void testValidation13() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.CHECKSUM).userId("joe")
        .parameters(Map.of("checksumType", "sha256")).document(document).indexUlid()
        .build((String) null);
    testTemplate(action, null, null);
  }

  @Test
  void testValidation13Sha512() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.CHECKSUM).userId("joe")
        .parameters(Map.of("checksumType", "sha512")).document(document).indexUlid()
        .build((String) null);
    testTemplate(action, null, null);
  }

  @Test
  void testValidationDeleteInvalidDeleteType() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.DELETE).userId("joe")
        .parameters(Map.of("deleteType", "ARCHIVE")).document(document).indexUlid()
        .build((String) null);
    testTemplate(action, "parameters.deleteType",
        "action 'deleteType' parameter must be one of [SOFT_DELETE, HARD_DELETE, PURGE]");
  }

  @Test
  void testValidationDeleteMissingDeleteType() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.DELETE).userId("joe").document(document)
        .indexUlid().build((String) null);
    testTemplate(action, "parameters.deleteType", "action 'deleteType' parameter is required");
  }

  @Test
  void testValidationDeleteValidDeleteType() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.DELETE).userId("joe")
        .parameters(Map.of("deleteType", "SOFT_DELETE")).document(document).indexUlid()
        .build((String) null);
    testTemplate(action, null, null);
  }

  @Test
  void testValidationMoveMissingPath() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.MOVE).userId("joe").document(document)
        .indexUlid().build((String) null);
    testTemplate(action, "parameters.path", "action 'path' parameter is required");
  }

  @Test
  void testValidationMovePathMustEndWithSlash() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.MOVE).userId("joe")
        .parameters(Map.of("path", "/approved")).document(document).indexUlid()
        .build((String) null);
    testTemplate(action, "parameters.path", "action 'path' parameter must end with '/'");
  }

  @Test
  void testValidationMoveValidPath() {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    Action action = new ActionBuilder().type(ActionType.MOVE).userId("joe")
        .parameters(Map.of("path", "approved/")).document(document).indexUlid()
        .build((String) null);
    testTemplate(action, null, null);
  }

  @Test
  void testZeroHeight() {
    testDimensionTemplate(Map.of("height", "0", "width", "100"), "parameters.height",
        "'height' parameter must be an integer > 0 or 'auto'");
  }

  @Test
  void testZeroWidth() {
    testDimensionTemplate(Map.of("height", "100", "width", "0"), "parameters.width",
        "'width' parameter must be an integer > 0 or 'auto'");
  }
}
