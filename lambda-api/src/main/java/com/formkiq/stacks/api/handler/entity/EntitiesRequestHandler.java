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
import com.formkiq.aws.dynamodb.AttributeValueListToListMap;
import com.formkiq.aws.dynamodb.AttributeValueToMapConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.base64.MapAttributeValueToString;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityAttribute;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityRecord;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityTypeRecord;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidator;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/entities/{entityTypeId}". */
public class EntitiesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil, DbKeys {

  /**
   * constructor.
   *
   */
  public EntitiesRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String tableName = awsservice.environment("DOCUMENTS_TABLE");

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    String entityTypeId = new EntityTypeIdTransformer(awsservice, siteId).apply(event);

    DynamoDbKey key =
        EntityRecord.builder().documentId("").entityTypeId(entityTypeId).name("").buildKey(siteId);

    QueryRequest q = DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk())
        .beginsWith(key.gsi1Sk()).nextToken(event.getQueryStringParameter("next"))
        .limit(event.getQueryStringParameter("limit")).build(tableName);

    QueryResponse response = db.query(q);

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityId").build();

    List<Map<String, Object>> items =
        new AttributeValueListToListMap(config).apply(response.items());

    items.forEach(i -> new AddEntityAttributeTransformer().apply(i));

    String nextToken = new MapAttributeValueToString().apply(response.lastEvaluatedKey());

    return ApiRequestHandlerResponse.builder().status(SC_OK).data("entities", items)
        .data("next", nextToken).build();
  }

  @Override
  public String getRequestUrl() {
    return "/entities/{entityTypeId}";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = new EntityTypeIdTransformer(awsservice, siteId).apply(event);

    AddEntityRequest request = fromBodyToObject(event, AddEntityRequest.class);

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    validate(awsservice, db, siteId, entityTypeId, request);

    AddEntity addEntity = request.getEntity();
    List<EntityAttribute> entityAttributes =
        notNull(addEntity.getAttributes()).stream().map(new AddEntityAttributeMapper()).toList();

    EntityRecord entity = EntityRecord.builder().documentId(ID.uuid()).name(addEntity.getName())
        .entityTypeId(entityTypeId).attributes(entityAttributes).build(siteId);

    Map<String, AttributeValue> attributes = entity.getAttributes();
    db.putItem(attributes);

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("entityId", entity.documentId())));
  }

  private void validate(final AwsServiceCache awsservice, final DynamoDbService db,
      final String siteId, final String entityTypeId, final AddEntityRequest request)
      throws BadException, ValidationException {

    if (request == null || request.getEntity() == null) {
      throw new BadException("Missing 'entity'");
    }

    ValidationBuilder vb = new ValidationBuilder();
    validateRequired(db, siteId, entityTypeId, request, vb);
    validateAttributes(awsservice, siteId, notNull(request.getEntity().getAttributes()), vb);
  }

  private void validateAttributes(final AwsServiceCache awsservice, final String siteId,
      final List<AddEntityAttribute> attributes, final ValidationBuilder vb) {

    AddEntityAttributeToRecordTransformer transformer = new AddEntityAttributeToRecordTransformer();
    List<DocumentAttributeRecord> list =
        attributes.stream().flatMap(a -> transformer.apply(a).stream()).toList();

    AttributeValidator attributeValidator = awsservice.getExtension(AttributeValidator.class);
    Map<String, AttributeRecord> attributeRecordMap =
        attributeValidator.getAttributeRecordMap(siteId, list);
    Collection<ValidationError> errors =
        attributeValidator.validateFullAttribute(Collections.emptyList(), siteId, list,
            attributeRecordMap, AttributeValidationAccess.ADMIN_CREATE);
    vb.addErrors(errors);
    vb.check();

    // List<String> attributeKeys = attributes.stream().map(AddEntityAttribute::getKey).toList();
    // Map<String, AttributeRecord> map = attributeService.getAttributes(siteId, attributeKeys);
    //
    // validateMissingAttributes(vb, attributeKeys, map);
    //
    // attributes.forEach(a -> {
    //
    // AttributeRecord record = map.get(a.getKey());
    //
    // List<Object> values = Arrays.asList(a.getBooleanValue(), a.getNumberValue(),
    // a.getNumberValues(), a.getStringValue(), a.getStringValues());
    //
    // if (!AttributeDataType.KEY_ONLY.equals(record.getDataType())) {
    // vb.isRequiredOnlyOne(a.getKey(), values, "only 1 attribute value is allowed");
    // }
    // });
    //
    // vb.check();
  }

  private static void validateMissingAttributes(final ValidationBuilder vb,
      final List<String> attributeKeys, final Map<String, AttributeRecord> map) {
    List<String> missingKeys = attributeKeys.stream().filter(key -> !map.containsKey(key)).toList();
    missingKeys.forEach(k -> vb.isRequired(k, (String) null, "missing attribute '" + k + "'"));
    vb.check();
  }

  private void validateRequired(final DynamoDbService db, final String siteId,
      final String entityTypeId, final AddEntityRequest request, final ValidationBuilder vb) {
    vb.isRequired("entityTypeId", entityTypeId);
    vb.isRequired("name", request.getEntity().getName());
    vb.check();

    EntityTypeRecord entityTypeRecord =
        EntityTypeRecord.builder().documentId(entityTypeId).namespace("").name("").build(siteId);

    vb.isRequired("entityTypeId", db.exists(entityTypeRecord.key()));
    vb.check();

    notNull(request.getEntity().getAttributes()).forEach(a -> vb.isRequired("key", a.getKey()));
  }
}
