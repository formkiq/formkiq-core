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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityAttribute;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityRecord;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.useractivities.AttributeValuesToChangeRecordFunction;
import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidator;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationError;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.strings.Strings.isEmpty;

/**
 * {@link Function} to transform {@link ApiGatewayRequestEvent} to {@link EntityRecord}.
 */
public class AddEntityRequestToEntityRecordTransformer
    implements ApiGatewayRequestEventUtil, Function<ApiGatewayRequestEvent, EntityRecord> {

  /** {@link AwsServiceCache}. */
  private final AwsServiceCache awsServices;
  /** Site Identifier. */
  private final String siteId;
  /** Is Update. */
  private final boolean update;

  /**
   * constructor.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param authorization {@link ApiAuthorization}
   * @param isUpdate boolean
   */
  public AddEntityRequestToEntityRecordTransformer(final AwsServiceCache awsservice,
      final ApiAuthorization authorization, final boolean isUpdate) {
    this.awsServices = awsservice;
    this.siteId = authorization.getSiteId();
    this.update = isUpdate;
  }

  @Override
  public EntityRecord apply(final ApiGatewayRequestEvent event) {

    String entityId = update ? event.getPathParameter("entityId") : ID.uuid();
    String entityTypeId = event.getPathParameter("entityTypeId");
    AddEntityRequest request = getEntityRequest(event, entityTypeId, entityId);

    validate(entityTypeId, request);

    AddEntity addEntity = request.entity();
    List<EntityAttribute> entityAttributes =
        notNull(addEntity.attributes()).stream().map(new AddEntityAttributeMapper()).toList();

    EntityRecord entity = EntityRecord.builder().documentId(entityId).name(addEntity.name())
        .entityTypeId(entityTypeId).attributes(entityAttributes).build(siteId);

    Map<String, AttributeValue> attributes = entity.getAttributes();

    DynamoDbService db = this.awsServices.getExtension(DynamoDbService.class);

    if (!update) {
      Map<String, ChangeRecord> changes =
          new AttributeValuesToChangeRecordFunction(Map.of("documentId", "entityId")).apply(null,
              attributes);
      UserActivityContext.set(UserActivityType.CREATE, changes);
    } else {

      Map<String, AttributeValue> oldAttributes = db.get(entity.key());
      Map<String, ChangeRecord> changes =
          new AttributeValuesToChangeRecordFunction(Map.of("documentId", "entityId"))
              .apply(oldAttributes, attributes);
      UserActivityContext.set(UserActivityType.UPDATE, changes);
    }

    db.putItem(attributes);

    return entity;
  }

  private AddEntityRequest getEntityRequest(final ApiGatewayRequestEvent event,
      final String entityTypeId, final String entityId) {

    AddEntityRequest req = fromBodyToObject(event, AddEntityRequest.class);

    if (update) {

      DynamoDbService db = this.awsServices.getExtension(DynamoDbService.class);
      EntityRecord.Builder builder =
          EntityRecord.builder().documentId(entityId).entityTypeId(entityTypeId).name("");

      Map<String, AttributeValue> attributes = db.get(builder.buildKey(siteId));
      if (attributes.isEmpty()) {
        throw new NotFoundException("Entity '" + entityId + "' not found");
      }

      if (req != null && req.entity() != null && isEmpty(req.entity().name())) {
        AddEntity entity = new AddEntity(attributes.get("name").s(), req.entity().attributes());
        req = new AddEntityRequest(entity);
      }
    }

    return req;
  }

  private void validate(final String entityTypeId, final AddEntityRequest request) {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired(null, request, "Missing 'entity'");
    vb.check();

    vb.isRequired(null, request.entity(), "Missing 'entity'");
    vb.check();

    DynamoDbService db = this.awsServices.getExtension(DynamoDbService.class);
    validateRequired(db, entityTypeId, request, vb);
    validateAttributes(notNull(request.entity().attributes()), vb);
  }

  private void validateAttributes(final List<AddEntityAttribute> attributes,
      final ValidationBuilder vb) {

    AddEntityAttributeToRecordTransformer transformer = new AddEntityAttributeToRecordTransformer();
    List<DocumentAttributeRecord> list =
        attributes.stream().flatMap(a -> transformer.apply(a).stream()).toList();

    AttributeValidator attributeValidator = this.awsServices.getExtension(AttributeValidator.class);
    Map<String, AttributeRecord> attributeRecordMap =
        attributeValidator.getAttributeRecordMap(siteId, list);
    Collection<ValidationError> errors =
        attributeValidator.validateFullAttribute(Collections.emptyList(), siteId, list,
            attributeRecordMap, AttributeValidationAccess.ADMIN_CREATE);
    vb.addErrors(errors);
    vb.check();
  }

  private void validateRequired(final DynamoDbService db, final String entityTypeId,
      final AddEntityRequest request, final ValidationBuilder vb) {
    vb.isRequired("entityTypeId", entityTypeId);
    vb.isRequired("name", request.entity().name());
    vb.check();

    EntityTypeRecord entityTypeRecord =
        EntityTypeRecord.builder().documentId(entityTypeId).namespace("").name("").build(siteId);

    vb.isRequired("entityTypeId", db.exists(entityTypeRecord.key()));
    vb.check();

    notNull(request.entity().attributes()).forEach(a -> vb.isRequired("key", a.key()));
  }
}
