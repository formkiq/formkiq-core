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
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsValidator;
import com.formkiq.module.actions.services.ActionsValidatorImpl;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentTagValidator;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
  public List<DocumentTag> validate(final ApiAuthorization authorization,
      final AwsServiceCache awsservice, final SiteConfiguration config, final String siteId,
      final AddDocumentRequest item, final boolean isUpdate)
      throws ValidationException, BadException {

    String userId = authorization.getUsername();
    Collection<ValidationError> errors = new ArrayList<>();
    List<DocumentTag> tags = validateTagSchema(item, userId);
    validateTags(tags, errors);
    validateActions(awsservice, config, siteId, item, authorization, errors);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return tags;
  }

  // TODO merge with ApiValidator validateActions
  private void validateActions(final AwsServiceCache awsservice, final SiteConfiguration config,
      final String siteId, final AddDocumentRequest item, final ApiAuthorization authorization,
      final Collection<ValidationError> errors) {

    initActionsValidator(awsservice);

    List<Action> actions = item.getActions();

    if (!notNull(actions).isEmpty()) {

      actions.forEach(a -> a.userId(authorization.getUsername()));

      for (Action action : actions) {
        errors.addAll(this.actionsValidator.validation(siteId, action, config.getChatGptApiKey(),
            config.getNotificationEmail()));
      }
    }
  }

  /**
   * Validate Document Tags.
   * 
   * @param tags {@link List} {@link DocumentTag}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateTags(final List<DocumentTag> tags,
      final Collection<ValidationError> errors) {

    List<String> tagKeys = tags.stream().map(DocumentTag::getKey).collect(Collectors.toList());

    DocumentTagValidator validator = new DocumentTagValidatorImpl();
    errors.addAll(validator.validateKeys(tagKeys));
  }


  /**
   * Validate {@link AddDocumentRequest} against a TagSchema.
   * 
   * @param item {@link AddDocumentRequest}
   * @param userId {@link String}
   * @return {@link List} {@link DocumentTag}
   */
  private List<DocumentTag> validateTagSchema(final AddDocumentRequest item, final String userId) {

    List<AddDocumentTag> doctags = notNull(item.getTags());
    AddDocumentTagToDocumentTag transform =
        new AddDocumentTagToDocumentTag(item.getDocumentId(), userId);

    return doctags.stream().map(transform).collect(Collectors.toList());
  }
}
