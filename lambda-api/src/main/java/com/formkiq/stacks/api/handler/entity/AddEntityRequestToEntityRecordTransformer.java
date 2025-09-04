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
import com.formkiq.aws.dynamodb.entity.EntityAttribute;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.aws.dynamodb.entity.PresetLlmPromptEntityBuilder;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
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
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.strings.Strings.isEmpty;

/**
 * {@link Function} to transform {@link ApiGatewayRequestEvent} to {@link EntityRecord}.
 */
public class AddEntityRequestToEntityRecordTransformer implements ApiGatewayRequestEventUtil,
    BiFunction<ApiAuthorization, ApiGatewayRequestEvent, EntityRecord> {

  /** {@link AwsServiceCache}. */
  private final AwsServiceCache awsServices;
  /** Is Update. */
  private final boolean update;

  /**
   * constructor.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param isUpdate boolean
   */
  public AddEntityRequestToEntityRecordTransformer(final AwsServiceCache awsservice,
      final boolean isUpdate) {
    this.awsServices = awsservice;
    this.update = isUpdate;
  }

  @Override
  public EntityRecord apply(final ApiAuthorization authorization,
      final ApiGatewayRequestEvent event) {

    String siteId = authorization.getSiteId();
    String entityId = update ? event.getPathParameter("entityId") : ID.uuid();
    String entityTypeId = event.getPathParameter("entityTypeId");
    AddEntityRequest request = getEntityRequest(siteId, event, entityTypeId, entityId);

    EntityTypeRecord entityType = validate(siteId, entityTypeId, request);

    // AddEntity addEntity = request.entity();
    // List<EntityAttribute> entityAttributes =
    // notNull(addEntity.attributes()).stream().map(new AddEntityAttributeMapper()).toList();

    EntityRecord entity = getEntityRecord(siteId, request, entityType, entityId);
    // EntityRecord.builder().documentId(entityId).name(addEntity.name())
    // .entityTypeId(entityTypeId).attributes(entityAttributes).build(siteId);

    Map<String, AttributeValue> attributes = entity.getAttributes();

    DynamoDbService db = this.awsServices.getExtension(DynamoDbService.class);

    if (!update) {
      Map<String, ChangeRecord> changes =
          new AttributeValuesToChangeRecordFunction(Map.of("documentId", "entityId")).apply(null,
              attributes);
      UserActivityContext.set(ActivityResourceType.ENTITY, UserActivityType.CREATE, changes,
          Collections.emptyMap());
    } else {

      Map<String, AttributeValue> oldAttributes = db.get(entity.key());
      Map<String, ChangeRecord> changes =
          new AttributeValuesToChangeRecordFunction(Map.of("documentId", "entityId"))
              .apply(oldAttributes, attributes);
      UserActivityContext.set(ActivityResourceType.ENTITY, UserActivityType.UPDATE, changes,
          Collections.emptyMap());
    }

    db.putItem(attributes);

    return entity;
  }

  private EntityRecord getEntityRecord(final String siteId, final AddEntityRequest request,
      final EntityTypeRecord entityType, final String entityId) {

    EntityRecord entity = null;
    AddEntity addEntity = request.entity();
    List<EntityAttribute> entityAttributes =
        notNull(addEntity.attributes()).stream().map(new AddEntityAttributeMapper()).toList();

    if (EntityTypeNamespace.PRESET.equals(entityType.namespace())) {

      PresetEntity presetEntity = PresetEntity.fromString(entityType.name());
      if (presetEntity != null) {

        entity = new PresetLlmPromptEntityBuilder().documentId(entityId).name(addEntity.name())
            .entityTypeId(entityType.documentId()).attributes(entityAttributes).build(siteId);

      } else {
        throw new NotFoundException("Entity Type '" + entityType.name() + "' not found");
      }

    } else {

      entity = EntityRecord.builder().documentId(entityId).name(addEntity.name())
          .entityTypeId(entityType.documentId()).attributes(entityAttributes).build(siteId);
    }

    return entity;
  }

  private AddEntityRequest getEntityRequest(final String siteId, final ApiGatewayRequestEvent event,
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

  private EntityTypeRecord validate(final String siteId, final String entityTypeId,
      final AddEntityRequest request) {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired(null, request, "Missing 'entity'");
    vb.check();

    vb.isRequired(null, request.entity(), "Missing 'entity'");
    vb.check();

    DynamoDbService db = this.awsServices.getExtension(DynamoDbService.class);
    EntityTypeRecord entityType = validateRequired(db, siteId, entityTypeId, request, vb);
    validateAttributes(siteId, notNull(request.entity().attributes()), vb);

    return entityType;
  }

  private void validateAttributes(final String siteId, final List<AddEntityAttribute> attributes,
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

  private EntityTypeRecord validateRequired(final DynamoDbService db, final String siteId,
      final String entityTypeId, final AddEntityRequest request, final ValidationBuilder vb) {
    vb.isRequired("entityTypeId", entityTypeId);
    vb.isRequired("name", request.entity().name());
    vb.check();

    EntityTypeRecord entityTypeRecord = EntityTypeRecord.builder().documentId(entityTypeId)
        .namespace(EntityTypeNamespace.CUSTOM).nameEmpty().build(siteId);

    Map<String, AttributeValue> entityRecord = db.get(entityTypeRecord.key());
    vb.isRequired("entityTypeId", !entityRecord.isEmpty());
    vb.check();

    notNull(request.entity().attributes()).forEach(a -> vb.isRequired("key", a.key()));

    return EntityTypeRecord.fromAttributeMap(entityRecord);
  }
}
