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
import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.AttributeValueToMapConfig;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.entity.FindEntityTypeByName;
import com.formkiq.aws.dynamodb.entity.GetPresetEntity;
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.attributes.AttributeType;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;
import static com.formkiq.aws.dynamodb.DbKeys.PK;

/** {@link ApiGatewayRequestHandler} for "/entityTypes/{entityTypeId}". */
public class EntityTypeRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public EntityTypeRequestHandler() {}

  private List<Map<String, AttributeValue>> addPresetAttributes(final AwsServiceCache awsservice,
      final String siteId, final AddEntityType addEntityType) {
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
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");
    EntityTypeNamespace namespace = new QueryParameterNamespace().apply(event);

    Map<String, AttributeValue> attributes =
        validateDelete(awsservice, siteId, entityTypeId, namespace);

    UserActivityContext.setDelete(ActivityResourceType.ENTITY_TYPE, attributes);

    DynamoDbKey entityTypeKey = EntityTypeRecord.builder().documentId(entityTypeId)
        .namespace(namespace, EntityTypeNamespace.CUSTOM).name("").buildKey(siteId);

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    if (!db.deleteItem(entityTypeKey).isDelete()) {
      throw new NotFoundException("entityType '" + entityTypeId + "' not found");
    }

    return ApiRequestHandlerResponse.builder().ok().body("message", "EntityType deleted").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");
    EntityTypeNamespace namespace = new QueryParameterNamespace().apply(event);

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);

    EntityTypeRecord.Builder builder =
        EntityTypeRecord.builder().documentId(entityTypeId).namespace(namespace).name("");

    Map<String, AttributeValue> attributes = db.get(builder.buildKey(siteId));

    if (attributes.isEmpty()) {
      throw new NotFoundException("entityType '" + entityTypeId + "' not found");
    }

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityTypeId").build();
    Map<String, Object> values = new AttributeValueToMap(config).apply(attributes);
    return ApiRequestHandlerResponse.builder().ok().body("entityType", values).build();
  }

  @Override
  public String getRequestUrl() {
    return "/entityTypes/{entityTypeId}";
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();

    new SetEntityTypeRecordFunction(awsservice, true).apply(siteId, event);
    // String entityTypeId = event.getPathParameter("entityTypeId");
    // boolean createIfMissing =
    // "true".equalsIgnoreCase(event.getQueryStringParameter("createIfMissing"));
    //
    // AddEntityTypeRequest request =
    // JsonToObject.fromJson(awsservice, event, AddEntityTypeRequest.class);
    // validate(request);
    //
    // AddEntityType addEntityType = request.entityType();
    // EntityTypeNamespace namespace =
    // addEntityType.namespace() != null ? addEntityType.namespace() : EntityTypeNamespace.CUSTOM;
    //
    // DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    // EntityTypeRecord entityType = EntityTypeRecord.builder().documentId(entityTypeId)
    // .name(addEntityType.name()).namespace(namespace).build(siteId);
    // Map<String, AttributeValue> attributes = entityType.getAttributes();
    // Map<String, AttributeValue> existing = db.get(entityType.key());
    //
    // if (!createIfMissing && existing.isEmpty()) {
    // throw new NotFoundException("entityType '" + entityTypeId + "' not found");
    // }
    //
    // validateNameAvailable(awsservice, db, siteId, entityTypeId, addEntityType.name(), namespace);
    //
    // List<Map<String, AttributeValue>> attributeList = new ArrayList<>();
    // attributeList.add(attributes);
    // if (existing.isEmpty()) {
    // attributeList.addAll(addPresetAttributes(awsservice, siteId, addEntityType));
    // UserActivityContext.setCreate(ActivityResourceType.ENTITY_TYPE, attributes);
    // } else {
    // UserActivityContext.setUpdate(ActivityResourceType.ENTITY_TYPE, existing, attributes);
    // }
    //
    // db.putItems(attributeList);

    return ApiRequestHandlerResponse.builder().ok().body("message", "EntityType set").build();
  }

  private void validate(final AddEntityTypeRequest request)
      throws BadException, ValidationException {

    if (request == null || request.entityType() == null) {
      throw new BadException("Missing required parameter 'entityType'");
    }
  }

  private Map<String, AttributeValue> validateDelete(final AwsServiceCache awsservice,
      final String siteId, final String entityTypeId, final EntityTypeNamespace namespace)
      throws ValidationException {

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    String tableName = awsservice.environment("DOCUMENTS_TABLE");

    DynamoDbKey entityTypeKey = EntityTypeRecord.builder().documentId(entityTypeId)
        .namespace(namespace, EntityTypeNamespace.CUSTOM).nameEmpty().buildKey(siteId);

    Map<String, AttributeValue> entityType = db.get(entityTypeKey);

    EntityRecord entity =
        EntityRecord.builder().entityTypeId(entityTypeId).documentId("").name("").build(siteId);
    DynamoDbKey entityKey = entity.key();

    QueryRequest q = DynamoDbQueryBuilder.builder().projectionExpression(PK).indexName(GSI1)
        .pk(entityKey.gsi1Pk()).beginsWith(entityKey.gsi1Sk()).limit("1").build(tableName);

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("entityTypeId", !entityType.isEmpty(), "EntityType not found");
    vb.isRequired("entityId", !db.exists(q), "Entities attached to Entity type");
    vb.check();

    return entityType;
  }

  private void validateNameAvailable(final AwsServiceCache awsservice, final DynamoDbService db,
      final String siteId, final String entityTypeId, final String name,
      final EntityTypeNamespace namespace) {
    ValidationBuilder vb = new ValidationBuilder();

    String documentsTable = awsservice.environment("DOCUMENTS_TABLE");

    QueryRequest req = new FindEntityTypeByName().build(documentsTable, siteId,
        new FindEntityTypeByName.EntityTypeName(namespace, name));
    Map<String, AttributeValue> attributes = db.getByQuery(req);
    String existingEntityTypeId =
        attributes.containsKey("documentId") ? attributes.get("documentId").s() : null;
    vb.isRequired("name", attributes.isEmpty() || entityTypeId.equals(existingEntityTypeId),
        "'name' already exists");

    vb.check();
  }
}
