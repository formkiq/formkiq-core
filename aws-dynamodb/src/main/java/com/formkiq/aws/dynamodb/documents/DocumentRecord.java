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

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * DocumentRecord representing a document item, with its DynamoDB key structure and metadata.
 */
public record DocumentRecord(DynamoDbKey key, String documentId, String artifactId,
    String belongsToDocumentId, String path, String deepLinkPath, String contentType,
    Long contentLength, String checksum, String checksumType, String s3version, String userId,
    String version, String width, String height, String timeToLive, Date insertedDate,
    Date lastModifiedDate, Collection<DocumentMetadata> metadata) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date fields.
   */
  public DocumentRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");

    if (insertedDate != null) {
      insertedDate = new Date(insertedDate.getTime());
    }
    if (lastModifiedDate != null) {
      lastModifiedDate = new Date(lastModifiedDate.getTime());
    }
  }

  /**
   * Constructs a {@code DocumentRecord} from a map of DynamoDB attributes.
   *
   * <p>
   * <b>Note:</b> This implementation maps simple scalar fields. Nested fields like {@code metadata}
   * and {@code documents} are not deserialized here (left as-is / null), because their DynamoDB
   * shape varies by implementation.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code DocumentRecord} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static DocumentRecord fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");

    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);
    Collection<DocumentMetadata> metadata =
        new AttributeValueToDocumentMetadata().apply(attributes);

    return new DocumentRecord(key, DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toString(attributes.get("artifactId")),
        DynamoDbTypes.toString(attributes.get("belongsToDocumentId")),
        DynamoDbTypes.toString(attributes.get("path")),
        DynamoDbTypes.toString(attributes.get("deepLinkPath")),
        DynamoDbTypes.toString(attributes.get("contentType")),
        DynamoDbTypes.toLong(attributes.get("contentLength")),
        DynamoDbTypes.toString(attributes.get("checksum")),
        DynamoDbTypes.toString(attributes.get("checksumType")),
        DynamoDbTypes.toString(attributes.get("s3version")),
        DynamoDbTypes.toString(attributes.get("userId")),
        DynamoDbTypes.toString(attributes.get("version")),
        DynamoDbTypes.toString(attributes.get("width")),
        DynamoDbTypes.toString(attributes.get("height")),
        DynamoDbTypes.toString(attributes.get("timeToLive")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")),
        DynamoDbTypes.toDate(attributes.get("lastModifiedDate")), metadata);
  }

  /**
   * Builds the DynamoDB item attribute map for this document, starting from the key attributes and
   * adding metadata fields.
   * <p>
   * Only non-null values are included via {@link DynamoDbAttributeMapBuilder}.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {

    Map<String, AttributeValue> metadataAttrs =
        new DocumentMetadataToAttributeValue().apply(metadata);

    DynamoDbAttributeMapBuilder map = key.getAttributesBuilder()
        .withString("documentId", documentId).withString("artifactId", artifactId)
        .withString("belongsToDocumentId", belongsToDocumentId).withString("path", path)
        .withString("deepLinkPath", deepLinkPath).withString("contentType", contentType)
        .withLong("contentLength", contentLength).withString("checksum", checksum)
        .withString("checksumType", checksumType).withString("s3version", s3version)
        .withString("userId", userId).withString("version", version).withString("width", width)
        .withString("height", height).withNumber("TimeToLive", timeToLive)
        .withDate("inserteddate", insertedDate).withDate("lastModifiedDate", lastModifiedDate)
        .withMap(metadataAttrs);

    return map.build();
  }

  /**
   * Creates a new {@link DocumentRecordBuilder} for {@link DocumentRecord}.
   *
   * @return a Builder instance
   */
  public static DocumentRecordBuilder builder() {
    return new DocumentRecordBuilder();
  }
}
