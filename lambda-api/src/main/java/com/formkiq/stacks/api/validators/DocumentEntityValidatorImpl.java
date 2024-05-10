/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.api.validators;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsValidator;
import com.formkiq.module.actions.services.ActionsValidatorImpl;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.tagschema.TagSchemaInterface;
import com.formkiq.stacks.api.handler.AddDocumentRequest;
import com.formkiq.stacks.api.handler.AddDocumentTag;
import com.formkiq.stacks.api.transformers.AddDocumentTagToDocumentTag;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.DocumentTagValidator;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

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
      final AwsServiceCache awsservice, final String siteId, final AddDocumentRequest item,
      final boolean isUpdate) throws ValidationException, BadException {

    String userId = authorization.getUsername();
    Collection<ValidationError> errors = new ArrayList<>();
    List<DocumentTag> tags = validateTagSchema(awsservice, siteId, item, userId, isUpdate, errors);
    validateTags(tags, errors);
    // validateAttributes(awsservice, siteId, item, errors);
    validateActions(awsservice, siteId, item, authorization, errors);
    // validateAccessAttributes(awsservice, item, errors);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return tags;
  }

  // /**
  // * Validate Access Attributes.
  // *
  // * @param awsservice {@link AwsServiceCache}
  // * @param item {@link DynamicDocumentItem}
  // * @param errors {@link Collection} {@link ValidationError}
  // */
  // private void validateAccessAttributes(final AwsServiceCache awsservice,
  // final DynamicDocumentItem item, final Collection<ValidationError> errors) {
  //
  // List<DynamicObject> list = item.getList("accessAttributes");
  //
  // if (!awsservice.hasModule("opa") && list != null && !list.isEmpty()) {
  // errors.add(new ValidationErrorImpl().key("accessAttributes")
  // .error("Access attributes are only supported with the 'open policy access' module"));
  // }
  // }

  private void validateActions(final AwsServiceCache awsservice, final String siteId,
      final AddDocumentRequest item, final ApiAuthorization authorization,
      final Collection<ValidationError> errors) {

    initActionsValidator(awsservice);

    List<Action> actions = item.getActions();
    // List<DynamicObject> objs = item.getList("actions");
    if (!notNull(actions).isEmpty()) {

      actions.forEach(a -> a.userId(authorization.getUsername()));
      // item.put("actions", objs);

      ConfigService configsService = awsservice.getExtension(ConfigService.class);
      DynamicObject configs = configsService.get(siteId);

      // actions.forEach(a -> {
      // // a.ty
      // // });
      // // List<Action> actions = objs.stream().map(o -> {
      //
      // // ActionType type;
      // // try {
      // // String stype = o.containsKey("type") ? o.getString("type").toUpperCase() : "";
      // // type = ActionType.valueOf(stype);
      // // } catch (IllegalArgumentException e) {
      // // type = null;
      // // }
      //
      // String queueId = a.queueId();
      // DynamicObject map = o.containsKey("parameters") ? o.getMap("parameters") : null;
      // Map<String, String> parameters = map != null
      // ? map.entrySet().stream()
      // .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()))
      // : Collections.emptyMap();
      //
      // return new Action().type(type).queueId(queueId).parameters(parameters)
      // .userId(o.getString("userId"));
      // });// .collect(Collectors.toList());

      for (Action action : actions) {
        errors.addAll(this.actionsValidator.validation(siteId, action, configs));
      }
    }
  }

  // private void validateAttributes(final AwsServiceCache awsservice, final String siteId,
  // final AddDocumentRequest item, final Collection<ValidationError> errors) {
  //
  // List<DocumentAttribute> list = notNull(item.getAttributes());
  // DocumentAttributeToDocumentAttributeRecord tr =
  // new DocumentAttributeToDocumentAttributeRecord(item.getDocumentId());
  //
  // List<DocumentAttributeRecord> documentAttributes =
  // list.stream().flatMap(a -> tr.apply(a).stream()).toList();
  //
  // DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
  // AttributeValidator validator = new AttributeValidatorImpl(db);
  // // errors.addAll(validator.validate(siteId, item.getDocumentId(), documentAttributes));
  // }

  /**
   * Validate Document Tags.
   * 
   * @param tags {@link List} {@link DocumentTag}
   * @param errors {@link Collection} {@link ValidationError}
   * @throws ValidationException ValidationException
   */
  private void validateTags(final List<DocumentTag> tags, final Collection<ValidationError> errors)
      throws ValidationException {

    // List<DynamicObject> tags = item.getList("tags");
    List<String> tagKeys = tags.stream().map(t -> t.getKey()).collect(Collectors.toList());

    DocumentTagValidator validator = new DocumentTagValidatorImpl();
    errors.addAll(validator.validateKeys(tagKeys));
  }


  /**
   * Validate {@link AddDocumentRequest} against a TagSchema.
   * 
   * @param cacheService {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param item {@link AddDocumentRequest}
   * @param userId {@link String}
   * @param isUpdate boolean
   * @param errors {@link Collection} {@link ValidationError}
   * @return {@link List} {@link DocumentTag}
   * @throws ValidationException ValidationException
   * @throws BadException BadException
   */
  private List<DocumentTag> validateTagSchema(final AwsServiceCache cacheService,
      final String siteId, final AddDocumentRequest item, final String userId,
      final boolean isUpdate, final Collection<ValidationError> errors)
      throws ValidationException, BadException {

    List<AddDocumentTag> doctags = notNull(item.getTags());
    AddDocumentTagToDocumentTag transform =
        new AddDocumentTagToDocumentTag(item.getDocumentId(), userId);

    List<DocumentTag> tags = doctags.stream().map(t -> {
      return transform.apply(t);
    }).collect(Collectors.toList());

    // boolean newCompositeTags = false;
    String tagSchemaId = item.getTagSchemaId();

    if (tagSchemaId != null) {

      DocumentTagSchemaPlugin plugin = cacheService.getExtension(DocumentTagSchemaPlugin.class);
      TagSchemaInterface tagSchema = plugin.getTagSchema(siteId, tagSchemaId);

      if (tagSchema == null) {
        throw new BadException("TagschemaId " + tagSchemaId + " not found");
      }

      plugin.updateInUse(siteId, tagSchema);
      List<DocumentTag> compositeTags =
          plugin.addCompositeKeys(tagSchema, siteId, item.getDocumentId(), tags, userId, !isUpdate,
              errors).stream().map(t -> t).collect(Collectors.toList());

      // newCompositeTags = !compositeTags.isEmpty();

      if (!errors.isEmpty()) {
        throw new ValidationException(errors);
      }

      tags.addAll(compositeTags);
    }

    // DocumentTagToDynamicDocumentTag tf = new DocumentTagToDynamicDocumentTag();
    // List<DynamicDocumentTag> objs = tags.stream().map(tf).collect(Collectors.toList());
    // item.put("tags", objs);
    // item.put("newCompositeTags", Boolean.valueOf(newCompositeTags));

    return tags;
  }
}
