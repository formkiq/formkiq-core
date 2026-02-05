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
package com.formkiq.aws.dynamodb.documents;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/**
 * Record representing an Document, with its DynamoDB key structure and metadata.
 */
public record DocumentRecord(DynamoDbKey key, String documentId, Date insertedDate,
    Date lastModifiedDate) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public DocumentRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");
    Objects.requireNonNull(lastModifiedDate, "lastModifiedDate must not be null");
    insertedDate = new Date(insertedDate.getTime());
    lastModifiedDate = new Date(lastModifiedDate.getTime());
  }

  /**
   * Constructs a {@code EntityRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code EntityRecord} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static DocumentRecord fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);

    return new DocumentRecord(key, DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")),
        DynamoDbTypes.toDate(attributes.get("lastModifiedDate")));
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
    DynamoDbAttributeMapBuilder map =
        key.getAttributesBuilder().withString("documentId", documentId)
            .withDate("inserteddate", insertedDate).withDate("lastModifiedDate", lastModifiedDate);

    return map.build();
  }

  /**
   * Creates a new {@link Builder} for {@link DocumentRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link DocumentRecord} that computes the DynamoDbKey.
   */
  public static class Builder implements DynamoDbEntityBuilder<DocumentRecord>, DbKeys {
    /** Document Id. */
    private String documentId;
    /** Inserted Date. */
    private Date insertedDate = new Date();
    /** Last Modified Date. */
    private Date lastModifiedDate = null;

    @Override
    public DocumentRecord build(final String siteId) {
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");
      if (lastModifiedDate == null) {
        lastModifiedDate = insertedDate;
      }

      DynamoDbKey key = buildKey(siteId);
      return new DocumentRecord(key, documentId, insertedDate, lastModifiedDate);
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {

      Objects.requireNonNull(documentId, "documentId must not be null");
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");

      String shortdate = DateUtil.getYyyyMmDdFormatter().format(insertedDate);
      String fullInsertedDate = DateUtil.getIsoDateFormatter().format(insertedDate);

      Map<String, AttributeValue> map = keysDocument(siteId, documentId);
      String pk = map.get(PK).s();
      String sk = map.get(SK).s();
      String gsi1Pk = createDatabaseKey(siteId, PREFIX_DOCUMENT_DATE_TS + shortdate);
      String gsi1Sk = fullInsertedDate + TAG_DELIMINATOR + documentId;

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk)
          .build();
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
     * Sets the insertion timestamp with millisecond precision.
     *
     * @param documentInsertedDate the insertion date
     * @return this Builder
     */
    public Builder insertedDate(final Date documentInsertedDate) {
      this.insertedDate = new Date(documentInsertedDate.getTime());
      return this;
    }

    /**
     * Sets the insertion timestamp with millisecond precision.
     *
     * @param documentLastModifiedDate the last modified date
     * @return this Builder
     */
    public Builder lastModifiedDate(final Date documentLastModifiedDate) {
      this.lastModifiedDate = new Date(documentLastModifiedDate.getTime());
      return this;
    }
  }
}
