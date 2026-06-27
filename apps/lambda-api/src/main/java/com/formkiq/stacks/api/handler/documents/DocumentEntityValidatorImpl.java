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
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.module.actions.services.ActionsValidator;
import com.formkiq.module.actions.services.ActionsValidatorImpl;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentTagValidator;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;

import java.util.List;
import java.util.stream.Stream;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * {@link DocumentEntityValidator} implementation.
 */
public class DocumentEntityValidatorImpl implements DocumentEntityValidator {

  /** {@link ActionsValidator}. */
  private ActionsValidator actionsValidator = null;

  private void initActionsValidator(final AwsServiceCache awsservice) {
    if (this.actionsValidator == null) {
      DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
      this.actionsValidator = new ActionsValidatorImpl(db);
    }
  }

  @Override
  public void validate(final AwsServiceCache awsservice, final SiteConfiguration config,
      final String siteId, final com.formkiq.stacks.dynamodb.documents.AddDocumentRequest request)
      throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    validateTags(vb, request);
    validateActions(awsservice, config, siteId, request, vb);

    vb.check();
  }

  // TODO merge with ApiValidator validateActions
  private void validateActions(final AwsServiceCache awsservice, final SiteConfiguration config,
      final String siteId, final com.formkiq.stacks.dynamodb.documents.AddDocumentRequest item,
      final ValidationBuilder vb) {

    initActionsValidator(awsservice);
    DocumentArtifact document =
        DocumentArtifact.of(item.getDocumentId(), item.isArtifacts() ? ID.ulid() : null);

    List<Action> actions = notNull(item.getActions()).stream()
        .map(a -> new AddActionToActionFunction(document).apply(siteId, a)).toList();

    if (!actions.isEmpty()) {
      int beforeActionErrors = vb.getErrors().size();

      for (Action action : actions) {
        this.actionsValidator.validation(vb, siteId, action, config.chatGptApiKey(),
            config.notificationEmail());
      }

      if (vb.getErrors().size() == beforeActionErrors) {
        actions.stream().filter(a -> ApiValidator.WORKFLOW_ONLY_ACTION_TYPES.contains(a.type()))
            .findFirst().ifPresent(action -> vb.addError("type",
                "action type cannot be '" + action.type().name() + "'"));
      }
    }
  }

  // /**
  // * Validate {@link AddDocumentRequest} against a TagSchema.
  // *
  // * @param item {@link AddDocumentRequest}
  // * @param userId {@link String}
  // * @return {@link List} {@link DocumentTag}
  // */
  // private List<DocumentTag> validateTagSchema(final AddDocumentRequest item, final String userId)
  // {
  //
  // List<AddDocumentTag> doctags = notNull(item.getTags());
  // AddDocumentTagToDocumentTag transform =
  // new AddDocumentTagToDocumentTag(item.getDocumentId(), userId);
  //
  // return doctags.stream().map(transform).collect(Collectors.toList());
  // }

  /**
   * Validate Document Tags.
   *
   * @param vb {@link ValidationBuilder}
   * @param request {@link com.formkiq.stacks.dynamodb.documents.AddDocumentRequest}
   */
  private void validateTags(final ValidationBuilder vb,
      final com.formkiq.stacks.dynamodb.documents.AddDocumentRequest request) {

    DocumentTagValidator validator = new DocumentTagValidatorImpl();

    var tagKeys = Stream
        .concat(
            notNull(request.getTags()).stream()
                .map(com.formkiq.stacks.dynamodb.documents.AddDocumentTag::getKey),
            notNull(request.getDocuments()).stream().flatMap(doc -> notNull(doc.getTags()).stream())
                .map(com.formkiq.stacks.dynamodb.documents.AddDocumentTag::getKey))
        .distinct().toList();

    validator.validateKeys(vb, tagKeys);
  }
}
