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
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;

/**
 * Record representing an Llm Result, with its DynamoDB key structure and metadata.
 */
public record LlmResultRecord(DynamoDbKey key, String documentId, String llmPromptEntityName,
    String content, Date insertedDate, List<Map<String, Object>> attributes) {

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public LlmResultRecord {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(llmPromptEntityName, "llmPromptEntityName must not be null");
    Objects.requireNonNull(content, "content must not be null");
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
  public static LlmResultRecord fromAttributeMap(final Map<String, AttributeValue> attributes) {
    Objects.requireNonNull(attributes, "attributes must not be null");
    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);
    List<Map<String, Object>> attrs = DynamoDbTypes.toList(attributes.get("attributes"));
    return new LlmResultRecord(key, DynamoDbTypes.toString(attributes.get("documentId")),
        DynamoDbTypes.toString(attributes.get("llmPromptEntityName")),
        DynamoDbTypes.toString(attributes.get("content")),
        DynamoDbTypes.toDate(attributes.get("inserteddate")), attrs);
  }

  /**
   * Builds the DynamoDB item attribute map for this entity, starting from the key attributes and
   * adding metadata fields.
   *
   * @return a Map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString("documentId", documentId)
        .withString("content", content).withString("llmPromptEntityName", llmPromptEntityName)
        .withList("attributes", attributes).withDate("inserteddate", insertedDate).build();
  }

  /**
   * Creates a new {@link Builder} for {@link LlmResultRecord}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link LlmResultRecord} that computes the DynamoDbKey.
   */
  public static class Builder implements DynamoDbEntityBuilder<LlmResultRecord> {
    /** Document Id. */
    private String documentId;
    /** LLM Prompt Entity Name. */
    private String llmPromptEntityName;
    /** Content. */
    private String content;
    /** Inserted Date. */
    private Date insertedDate = new Date();
    /** Attributes. */
    private List<Map<String, Object>> attributes;

    /**
     * Sets the document identifier.
     *
     * @param llmResultDocumentId the document ID
     * @return this Builder
     */
    public Builder documentId(final String llmResultDocumentId) {
      this.documentId = llmResultDocumentId;
      return this;
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
     * Llm Result Attributes.
     * 
     * @param resultAttributes {@link List} {@link Map}
     * @return this Builder
     */
    public Builder attributes(final List<Map<String, Object>> resultAttributes) {
      this.attributes = resultAttributes;
      return this;
    }

    @Override
    public DynamoDbKey buildKey(final String siteId) {

      Objects.requireNonNull(documentId, "documentId must not be null");
      Objects.requireNonNull(llmPromptEntityName, "llmPromptEntityName must not be null");

      String pk = PREFIX_DOCS + documentId;
      String sk = "llmresult#" + DateUtil.getNowInIso8601Format() + "#" + llmPromptEntityName;

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).build();
    }

    @Override
    public LlmResultRecord build(final String siteId) {
      Objects.requireNonNull(insertedDate, "insertedDate must not be null");

      DynamoDbKey key = buildKey(siteId);
      return new LlmResultRecord(key, documentId, llmPromptEntityName, content, insertedDate,
          attributes);
    }
  }
}
