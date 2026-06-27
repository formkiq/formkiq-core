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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.attributes.AttributeType;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.entity.FindEntityTypeByName;
import com.formkiq.aws.dynamodb.entity.GetPresetEntities;
import com.formkiq.aws.dynamodb.entity.GetPresetEntity;
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * {@link BiFunction} to set {@link EntityTypeRecord}.
 */
public class SetEntityTypeRecordFunction
    implements BiFunction<String, ApiGatewayRequestEvent, EntityTypeRecord> {
  /** {@link AwsServiceCache}. */
  private final AwsServiceCache awsservice;
  /** Is Update. */
  private final boolean update;

  /**
   * constructor.
   * 
   * @param awsserviceCache {@link AwsServiceCache}
   * @param isUpdate boolean
   */
  public SetEntityTypeRecordFunction(final AwsServiceCache awsserviceCache,
      final boolean isUpdate) {
    this.awsservice = awsserviceCache;
    this.update = isUpdate;
  }

  private List<Map<String, AttributeValue>> addPresetAttributes(final String siteId,
      final AddEntityType addEntityType) {
    final List<Map<String, AttributeValue>> attributeList = new ArrayList<>();

    if (EntityTypeNamespace.PRESET.equals(addEntityType.namespace())) {

      Optional<PresetEntity> presetEntity =
          new GetPresetEntity(awsservice).apply(addEntityType.name());

      presetEntity.ifPresent(presetEntityI -> presetEntityI.getAttributeKeys().forEach(k -> {

        AttributeKeyReserved key = AttributeKeyReserved.find(k);
        AttributeDataType dataType = key != null ? key.getDataType() : AttributeDataType.STRING;

        AttributeRecord a = new AttributeRecord().type(AttributeType.STANDARD).dataType(dataType)
            .key(k).documentId(k);
        attributeList.add(a.getAttributes(siteId));
      }));
    }

    return attributeList;
  }

  @Override
  public EntityTypeRecord apply(final String siteId, final ApiGatewayRequestEvent event) {

    AddEntityTypeRequest request =
        JsonToObject.fromJson(awsservice, event, AddEntityTypeRequest.class);
    validate(request);

    AddEntityType addEntityType = request.entityType();
    String entityTypeId = update ? event.getPathParameter("entityTypeId") : ID.uuid();

    Collection<PresetEntity> presetEntities = new GetPresetEntities(awsservice).apply(null);

    EntityTypeRecord entityType =
        EntityTypeRecord.builderWithPresets(presetEntities).documentId(entityTypeId)
            .name(addEntityType.name()).namespace(addEntityType.namespace()).build(siteId);

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    validateExist(db, siteId, addEntityType.name(), addEntityType.namespace());

    List<Map<String, AttributeValue>> attributeList = new ArrayList<>();

    Map<String, AttributeValue> attributes = entityType.getAttributes();
    attributeList.add(attributes);
    attributeList.addAll(addPresetAttributes(siteId, addEntityType));

    setUserActivity(event, db, entityType, entityTypeId, attributes);

    db.putItems(attributeList);
    return entityType;
  }

  private void setUserActivity(final ApiGatewayRequestEvent event, final DynamoDbService db,
      final EntityTypeRecord entityType, final String entityTypeId,
      final Map<String, AttributeValue> attributes) {

    if (!update) {
      UserActivityContext.setCreate(ActivityResourceType.ENTITY_TYPE, attributes);
      return;
    }

    Map<String, AttributeValue> oldAttributes = db.get(entityType.key());
    boolean createIfMissing =
        "true".equalsIgnoreCase(event.getQueryStringParameter("createIfMissing"));

    if (oldAttributes.isEmpty() && !createIfMissing) {
      throw new NotFoundException("Entity Type '" + entityTypeId + "' not found");
    }

    if (createIfMissing) {
      UserActivityContext.setCreate(ActivityResourceType.ENTITY_TYPE, attributes);
    } else {
      UserActivityContext.setUpdate(ActivityResourceType.ENTITY_TYPE, oldAttributes, attributes);
    }
  }

  private void validate(final AddEntityTypeRequest request)
      throws BadException, ValidationException {

    if (request == null || request.entityType() == null) {
      throw new BadException("Missing required parameter 'entityType'");
    }
  }

  private void validateExist(final DynamoDbService db, final String siteId, final String name,
      final EntityTypeNamespace namespace) {
    ValidationBuilder vb = new ValidationBuilder();

    String documentsTable = awsservice.environment("DOCUMENTS_TABLE");

    QueryRequest req = new FindEntityTypeByName().build(documentsTable, siteId,
        new FindEntityTypeByName.EntityTypeName(namespace, name));
    vb.isRequired("name", !db.exists(req), "'name' already exists");

    vb.check();
  }
}
