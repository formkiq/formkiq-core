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
package com.formkiq.aws.dynamodb.llm;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;

/**
 * Record representing a Document LLM Result.
 *
 * <p>
 * Key pattern:
 * <ul>
 * <li>PK = "docs#" + documentId</li>
 * <li>SK = "llmaipromptresult#" + llmPromptEntityName + "#" + timestamp</li>
 * </ul>
 */
public record DocumentAiPromptResultRecord(DynamoDbKey key, String documentId, String artifactId,
    String llmPromptEntityName, String content, Date insertedDate,
    List<DocumentAiPromptValue> values, String userId) {

  /** {@link DocumentAiPromptResultRecord} SK Prefix. */
  public static final String KEY_SK_PREFIX = "llmaipromptresult#";

  /** {@link DocumentAiPromptResultRecord} SK Prefix. */
  public static final String KEY_SK_PREFIX_ART = "llmaipromptresult_art#";

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public DocumentAiPromptResultRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(llmPromptEntityName, "llmPromptEntityName must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");
    Objects.requireNonNull(values, "values must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    insertedDate = new Date(insertedDate.getTime());
    values = List.copyOf(values);
  }

  /**
   * Constructs a {@code DocumentAiPromptResultRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code DocumentAiPromptResultRecord} instance
   * @throws NullPointerException if {@code attributes} is null
   */
  public static DocumentAiPromptResultRecord fromAttributeMap(
      final Map<String, AttributeValue> attributes) {

    Objects.requireNonNull(attributes, "attributes must not be null");

    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);
    AttributeValue valuesAttribute = attributes.get("values");
    List<DocumentAiPromptValue> promptValues =
        valuesAttribute != null
            ? DynamoDbTypes.toList(valuesAttribute).stream().map(DocumentAiPromptValue::fromMap)
                .toList()
            : List.of();

    return new DocumentAiPromptResultRecord(key,
        DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toString(attributes.get("artifactId")),
        DynamoDbTypes.toString(attributes.get("llmPromptEntityName")),
        DynamoDbTypes.toString(attributes.get("content")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")), promptValues,
        DynamoDbTypes.toString(attributes.get("userId")));
  }

  /**
   * Builds the DynamoDB item attribute map for this entity, starting from the key attributes and
   * adding metadata fields.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("documentId", documentId)
        .withString("artifactId", artifactId).withString("content", content)
        .withString("llmPromptEntityName", llmPromptEntityName).withString("userId", userId)
        .withList("values", values.stream().map(DocumentAiPromptValue::toMap).toList())
        .withDate("inserteddate", insertedDate).build();
  }

  /**
   * Creates a new {@link Builder} for {@link DocumentAiPromptResultRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link DocumentAiPromptResultRecord} that computes the DynamoDbKey.
   */
  public static class Builder implements DynamoDbEntityBuilder<DocumentAiPromptResultRecord> {
    /** Document Id. */
    private DocumentArtifact document;
    /** LLM Prompt Entity Name. */
    private String llmPromptEntityName;
    /** Content. */
    private String content;
    /** UserId. */
    private String userId;
    /** Inserted Date. */
    private Date insertedDate = new Date();
    /** Values. */
    private List<DocumentAiPromptValue> values;

    @Override
    public DocumentAiPromptResultRecord build(final DynamoDbKey key) {
      return new DocumentAiPromptResultRecord(key, document.documentId(), document.artifactId(),
          llmPromptEntityName, content, insertedDate, values, userId);
    }

    @Override
    public DocumentAiPromptResultRecord build(final String siteId) {
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");

      DynamoDbKey key = buildKey(siteId);
      return build(key);
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {

      String documentId = document.documentId();
      Objects.requireNonNull(documentId, "documentId must not be null");
      Objects.requireNonNull(llmPromptEntityName, "llmPromptEntityName must not be null");

      String prefix = document.artifactId() == null ? KEY_SK_PREFIX
          : KEY_SK_PREFIX_ART + document.artifactId() + "#";

      String timestamp = DateUtil.getNowInIso8601Format();
      String pk = PREFIX_DOCS + documentId;
      String sk = prefix + llmPromptEntityName + "#" + timestamp;
      String gsi1Pk = PREFIX_DOCS + documentId;
      String gsi1Sk = prefix + timestamp + "#" + llmPromptEntityName;

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk)
          .build();
    }

    /**
     * Sets the llmContent of the document.
     *
     * @param llmContent the llmContent
     * @return this Builder
     */
    public Builder content(final String llmContent) {
      this.content = llmContent;
      return this;
    }

    /**
     * Sets the document identifier.
     *
     * @param llmResultDocument the document
     * @return this Builder
     */
    public Builder document(final DocumentArtifact llmResultDocument) {
      this.document = llmResultDocument;
      return this;
    }

    /**
     * Sets the insertion timestamp with millisecond precision.
     *
     * @param now the insertion date
     * @return this Builder
     */
    public Builder insertedDate(final Date now) {
      this.insertedDate = new Date(now.getTime());
      return this;
    }

    /**
     * Sets the llmPromptEntity of the document.
     *
     * @param llmPromptEntity the llmPromptEntity
     * @return this Builder
     */
    public Builder llmPromptEntityName(final String llmPromptEntity) {
      this.llmPromptEntityName = llmPromptEntity;
      return this;
    }

    /**
     * Sets the User identifier.
     *
     * @param llmResultUserId the user ID
     * @return this Builder
     */
    public Builder userId(final String llmResultUserId) {
      this.userId = llmResultUserId;
      return this;
    }

    /**
     * AI prompt values.
     *
     * @param promptValues {@link List} {@link DocumentAiPromptValue}
     * @return this Builder
     */
    public Builder values(final List<DocumentAiPromptValue> promptValues) {
      this.values = promptValues;
      return this;
    }
  }
}
