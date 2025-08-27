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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.builder.MapToAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing an entity, with its DynamoDB key structure and metadata.
 */
public record EntityRecord(DynamoDbKey key, String entityTypeId, String documentId, String name,
    Date insertedDate, Map<String, AttributeValue> attributes) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public EntityRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(entityTypeId, "entityTypeId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");
    insertedDate = new Date(insertedDate.getTime());
  }

  /**
   * Constructs a {@code EntityRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code EntityRecord} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static EntityRecord fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);

    Map<String, AttributeValue> entityAttributes = Collections.emptyMap();

    return new EntityRecord(key, DynamoDbTypes.toString(attributes.get("entityTypeId")),
        DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toString(attributes.get("name")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")), entityAttributes);
  }

  /**
   * Builds the DynamoDB item attribute map for this entity, starting from the key attributes and
   * adding metadata fields.
   * <p>
   * Only non-null values are included via {@link DynamoDbAttributeMapBuilder}.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    DynamoDbAttributeMapBuilder map = key.getAttributesBuilder()
        .withString("entityTypeId", entityTypeId).withString("documentId", documentId)
        .withString("name", name).withDate("inserteddate", insertedDate);

    if (attributes != null) {
      attributes.forEach(map::withAttributeValue);
    }

    return map.build();
  }

  /**
   * Creates a new {@link Builder} for {@link EntityRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link EntityRecord} that computes the DynamoDbKey.
   */
  public static class Builder implements DynamoDbEntityBuilder<EntityRecord> {
    /** Entity Type Id. */
    private String entityTypeId;
    /** Document Id. */
    private String documentId;
    /** Name. */
    private String name;
    /** Inserted Date. */
    private Date insertedDate = new Date();
    /** Attributes. */
    private List<EntityAttribute> attributes = new ArrayList<>();

    /**
     * Sets the entity attributes.
     *
     * @param entityAttributes {@link List} {@link EntityAttribute}
     * @return this Builder
     */
    public Builder attributes(final List<EntityAttribute> entityAttributes) {
      this.attributes = entityAttributes;
      return this;
    }

    /**
     * Sets the entity type identifier.
     *
     * @param entityTypeDocumentId the document ID
     * @return this Builder
     */
    public Builder entityTypeId(final String entityTypeDocumentId) {
      this.entityTypeId = entityTypeDocumentId;
      return this;
    }

    /**
     * Sets the document identifier.
     *
     * @param entityTypeDocumentId the document ID
     * @return this Builder
     */
    public Builder documentId(final String entityTypeDocumentId) {
      this.documentId = entityTypeDocumentId;
      return this;
    }

    /**
     * Sets the entityName of the document.
     *
     * @param entityName the entityName
     * @return this Builder
     */
    public Builder name(final String entityName) {
      this.name = entityName;
      return this;
    }

    /**
     * Sets the insertion timestamp with millisecond precision.
     *
     * @param entityInsertedDate the insertion date
     * @return this Builder
     */
    public Builder insertedDate(final Date entityInsertedDate) {
      this.insertedDate = new Date(entityInsertedDate.getTime());
      return this;
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {

      Objects.requireNonNull(entityTypeId, "entityTypeId must not be null");
      Objects.requireNonNull(name, "name must not be null");
      Objects.requireNonNull(documentId, "documentId must not be null");

      String pk = "entity#" + entityTypeId + "#" + documentId;
      String sk = "entity";
      String gsi1Pk = "entity#" + entityTypeId;
      String gsi1Sk = name.isBlank() ? "entity#" : "entity#" + name + "#" + documentId;

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk)
          .build();
    }

    @Override
    public EntityRecord build(final String siteId) {
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");

      DynamoDbKey key = buildKey(siteId);

      Map<String, Object> map = new EntityAttributesToMapTransformer().apply(attributes);

      Map<String, AttributeValue> entityAttributes = new MapToAttributeValue().apply(map);
      return new EntityRecord(key, entityTypeId, documentId, name, insertedDate, entityAttributes);
    }
  }
}
