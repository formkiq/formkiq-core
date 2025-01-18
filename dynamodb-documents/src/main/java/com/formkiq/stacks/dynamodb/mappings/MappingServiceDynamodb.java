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
package com.formkiq.stacks.dynamodb.mappings;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * {@link MappingService} implementation for Dynamodb.
 */
public class MappingServiceDynamodb implements MappingService, DbKeys {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link AttributeService}. */
  private final AttributeService attributeService;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public MappingServiceDynamodb(final DynamoDbService dbService) {
    this.db = dbService;
    this.attributeService = new AttributeServiceDynamodb(dbService);
  }

  @Override
  public boolean deleteMapping(final String siteId, final String mappingId) {

    MappingRecord r = new MappingRecord().setDocumentId(mappingId);
    return this.db.deleteItem(Map.of(PK, r.fromS(r.pk(siteId)), SK, r.fromS(r.sk())));
  }

  @Override
  public PaginationResults<MappingRecord> findMappings(final String siteId,
      final PaginationMapToken token, final int limit) {

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);
    QueryConfig config = new QueryConfig().indexName(DbKeys.GSI1).scanIndexForward(Boolean.TRUE);
    AttributeValue pk = AttributeValue.fromS(createDatabaseKey(siteId, MappingRecord.PREFIX_PK));
    AttributeValue sk = AttributeValue.fromS(MappingRecord.PREFIX_SK_GSI1);
    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, startkey, limit);

    List<Map<String, AttributeValue>> keys =
        response.items().stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();

    List<Map<String, AttributeValue>> attrs = this.db.getBatch(new BatchGetConfig(), keys);

    List<MappingRecord> list =
        attrs.stream().map(a -> new MappingRecord().getFromAttributes(siteId, a)).toList();

    return new PaginationResults<>(list, new QueryResponseToPagination().apply(response));
  }

  @Override
  public MappingRecord getMapping(final String siteId, final String mappingId) {

    MappingRecord r = new MappingRecord().setDocumentId(mappingId);
    Map<String, AttributeValue> attr = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    return !attr.isEmpty() ? r.getFromAttributes(siteId, attr) : null;
  }

  @Override
  public MappingRecord saveMapping(final String siteId, final String mappingId,
      final Mapping mapping) throws ValidationException {

    Collection<ValidationError> errors = validate(siteId, mapping);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    MappingRecord record = new MappingToMappingRecord().apply(mapping);

    if (!isEmpty(mappingId)) {
      record.setDocumentId(mappingId);
    }

    this.db.putItem(record.getAttributes(siteId));

    return record;
  }

  @Override
  public List<MappingAttribute> getAttributes(final MappingRecord mapping) {
    Gson gson = new GsonBuilder().create();
    Type listType = new TypeToken<ArrayList<MappingAttribute>>() {}.getType();
    return gson.fromJson(mapping.getAttributes(), listType);
  }

  private Collection<ValidationError> validate(final String siteId, final Mapping record) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (isEmpty(record.getName())) {
      errors.add(new ValidationErrorImpl().key("name").error("'name' is required"));
    }

    if (notNull(record.getAttributes()).isEmpty()) {
      errors.add(new ValidationErrorImpl().key("attributes").error("'attributes' is required"));
    }

    if (errors.isEmpty()) {
      int size = record.getAttributes().size();

      for (int i = 0; i < size; i++) {
        MappingAttribute attribute = record.getAttributes().get(i);
        validate(attribute, i, errors);

        if (errors.isEmpty()) {
          validateMetadataField(attribute, i, errors);
        }
      }

      if (errors.isEmpty()) {
        List<String> attributeKeys = notNull(record.getAttributes()).stream()
            .map(MappingAttribute::getAttributeKey).toList();

        Map<String, AttributeRecord> attributes =
            this.attributeService.getAttributes(siteId, attributeKeys);

        for (String attributeKey : attributeKeys) {
          if (!attributes.containsKey(attributeKey)) {
            errors.add(new ValidationErrorImpl().key(attributeKey)
                .error("invalid attribute '" + attributeKey + "'"));
          }
        }

        if (attributeKeys.size() != new HashSet<>(attributeKeys).size()) {
          errors.add(new ValidationErrorImpl().error("duplicate attributes in mapping"));
        }
      }
    }

    return errors;
  }

  private void validate(final MappingAttribute attribute, final int index,
      final Collection<ValidationError> errors) {

    if (isEmpty(attribute.getAttributeKey())) {
      errors.add(new ValidationErrorImpl().key("attribute[" + index + "].attributeKey")
          .error("'attributeKey' is required"));
    }

    if (attribute.getSourceType() == null) {
      errors.add(new ValidationErrorImpl().key("attribute[" + index + "].sourceType")
          .error("'sourceType' is required"));
    }

    if (MappingAttributeSourceType.MANUAL.equals(attribute.getSourceType())) {

      if (isEmpty(attribute.getDefaultValue()) && notNull(attribute.getDefaultValues()).isEmpty()) {
        errors.add(new ValidationErrorImpl().key("attribute[" + index + "].defaultValue")
            .error("'defaultValue' or 'defaultValues' is required"));
      }

    } else {

      if (attribute.getLabelMatchingType() == null) {
        errors.add(new ValidationErrorImpl().key("attribute[" + index + "].labelMatchingType")
            .error("'labelMatchingType' is required"));
      }

      if (notNull(attribute.getLabelTexts()).isEmpty()) {
        errors.add(new ValidationErrorImpl().key("attribute[" + index + "].labelTexts")
            .error("'labelTexts' is required"));
      }
    }
  }

  private void validateMetadataField(final MappingAttribute attribute, final int index,
      final Collection<ValidationError> errors) {

    if (MappingAttributeSourceType.METADATA.equals(attribute.getSourceType())
        && attribute.getMetadataField() == null) {
      errors.add(new ValidationErrorImpl().key("attribute[" + index + "].metadataField")
          .error("'metadataField' is required"));
    }
  }
}
