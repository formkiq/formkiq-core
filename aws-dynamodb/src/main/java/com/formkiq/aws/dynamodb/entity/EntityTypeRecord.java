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

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing an entity, with its DynamoDB key structure and metadata.
 */
public record EntityTypeRecord(DynamoDbKey key, String documentId, String namespace, String name,
    Date insertedDate) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public EntityTypeRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(namespace, "namespace must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");
    insertedDate = new Date(insertedDate.getTime());
  }

  /**
   * Constructs a {@code EntityTypeRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code EntityTypeRecord} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static EntityTypeRecord fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);
    return new EntityTypeRecord(key, DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toString(attributes.get("namespace")),
        DynamoDbTypes.toString(attributes.get("name")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")));
  }

  /**
   * Builds the DynamoDB item attribute map for this entity, starting from the key attributes and
   * adding metadata fields.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("documentId", documentId)
        .withString("namespace", namespace).withString("name", name)
        .withDate("inserteddate", insertedDate).build();
  }

  /**
   * Creates a new {@link Builder} for {@link EntityTypeRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link EntityTypeRecord} that computes the DynamoDbKey.
   */
  public static class Builder implements DynamoDbEntityBuilder<EntityTypeRecord> {
    /** Document Id. */
    private String documentId;
    /** Namespace. */
    private String namespace;
    /** Name. */
    private String name;
    /** Inserted Date. */
    private Date insertedDate = new Date();

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
     * Sets the entityTypeNamespace of the document.
     *
     * @param entityTypeNamespace the entityTypeNamespace
     * @return this Builder
     */
    public Builder namespace(final String entityTypeNamespace) {
      this.namespace = entityTypeNamespace != null ? entityTypeNamespace.toUpperCase() : null;
      return this;
    }

    /**
     * Sets the entityTypeName of the document.
     *
     * @param entityTypeName the entityTypeName
     * @return this Builder
     */
    public Builder name(final String entityTypeName) {
      this.name = entityTypeName;
      return this;
    }

    /**
     * Sets the insertion timestamp with millisecond precision.
     *
     * @param entityTypeInsertedDate the insertion date
     * @return this Builder
     */
    public Builder insertedDate(final Date entityTypeInsertedDate) {
      this.insertedDate = new Date(entityTypeInsertedDate.getTime());
      return this;
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {

      Objects.requireNonNull(documentId, "documentId must not be null");
      Objects.requireNonNull(namespace, "namespace must not be null");
      Objects.requireNonNull(name, "name must not be null");

      String pk = "entityType#" + documentId;
      String sk = "entityType";
      String gsi1Pk = "entityType#";
      String gsi1Sk = name.isBlank() ? "entityType#" + namespace + "#"
          : "entityType#" + namespace + "#" + name + "#";

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk)
          .build();
    }

    @Override
    public EntityTypeRecord build(final String siteId) {
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");

      DynamoDbKey key = buildKey(siteId);
      return new EntityTypeRecord(key, documentId, namespace, name, insertedDate);
    }
  }
}
