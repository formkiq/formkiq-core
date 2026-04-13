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
package com.formkiq.aws.dynamodb.model;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.objects.DateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * {@link DynamoDbEntityBuilder} for {@link DocumentTagRecord}.
 */
public class DocumentTagRecordBuilder
    implements DynamoDbEntityBuilder<List<DocumentTagRecord>>, DbKeys {

  /** Document Id. */
  private String documentId;
  /** Tag Key. */
  private String tagKey;
  /** Artifact Id. */
  private String artifactId;
  /** Tag Value. */
  private String tagValue;
  /** Tag Values. */
  private List<String> tagValues;
  /** Inserted Date. */
  private Date insertedDate = new Date();
  /** User Id. */
  private String userId;
  /** Tag Type. */
  private DocumentTagType type = DocumentTagType.USERDEFINED;

  /**
   * Set artifact id.
   *
   * @param id {@link String}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder artifactId(final String id) {
    this.artifactId = id;
    return this;
  }

  @Override
  public List<DocumentTagRecord> build(final DynamoDbKey key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<DocumentTagRecord> build(final String siteId) {

    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(tagKey, "tagKey must not be null");
    Objects.requireNonNull(insertedDate, "insertedDate must not be null");

    List<DocumentTagRecord> records = new ArrayList<>();
    if (isValueList()) {
      int tagValueIndex = -1;
      for (String value : tagValues) {

        DynamoDbKey key = buildKey(siteId, value, tagValueIndex);
        records.add(new DocumentTagRecord(key, documentId, artifactId, tagKey, value, tagValues,
            insertedDate, userId, type));

        tagValueIndex++;
        if (tagValueIndex == 0) {
          tagValueIndex++;
        }
      }
    } else {
      DynamoDbKey key = buildKey(siteId, tagValue, -1);
      records.add(new DocumentTagRecord(key, documentId, artifactId, tagKey, tagValue, null,
          insertedDate, userId, type));
    }

    return records;
  }

  @Override
  public DynamoDbKey buildKey(final String siteId) {
    List<DocumentTagRecord> keys = build(siteId);
    if (keys.size() > 1) {
      throw new UnsupportedOperationException("Multiple Tag Keys found");
    }

    return keys.size() == 1 ? keys.get(0).key() : null;
  }

  private DynamoDbKey buildKey(final String siteId, final String value, final int tagValueIndex) {
    String pk = PREFIX_DOCS + documentId;
    String skPrefix = artifactId != null ? "tags_art#" + artifactId + TAG_DELIMINATOR : PREFIX_TAGS;
    String sk = tagValueIndex > -1 ? skPrefix + tagKey + TAG_DELIMINATOR + "idx" + tagValueIndex
        : skPrefix + tagKey;
    String fullDate = DateUtil.getIsoDateFormatter().format(insertedDate);
    String gsi2Sk = value != null && !value.isEmpty() ? value : " ";

    return DynamoDbKey.builder().pk(siteId, pk).sk(sk)
        .gsi1Pk(siteId, PREFIX_TAG + tagKey + TAG_DELIMINATOR + value)
        .gsi1Sk(fullDate + TAG_DELIMINATOR + documentId).gsi2Pk(siteId, PREFIX_TAG + tagKey)
        .gsi2Sk(gsi2Sk).build();
  }

  /**
   * Set document id.
   *
   * @param id {@link String}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Set inserted date.
   *
   * @param date {@link Date}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder insertedDate(final Date date) {
    this.insertedDate = date != null ? new Date(date.getTime()) : null;
    return this;
  }

  private boolean isValueList() {
    return !notNull(tagValues).isEmpty();
  }

  /**
   * Set {@link DocumentTag}.
   *
   * @param tag {@link DocumentTag}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder tag(final DocumentTag tag) {
    this.documentId = tag.getDocumentId();
    this.tagKey = tag.getKey();
    this.tagValue = tag.getValue();
    this.tagValues = tag.getValues();
    this.insertedDate = tag.getInsertedDate();
    this.userId = tag.getUserId();
    this.type = tag.getType() != null ? tag.getType() : DocumentTagType.USERDEFINED;
    return this;
  }

  /**
   * Set tag key.
   *
   * @param key {@link String}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder tagKey(final String key) {
    this.tagKey = key;
    return this;
  }

  /**
   * Set tag value.
   *
   * @param value {@link String}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder tagValue(final String value) {
    this.tagValue = value;
    return this;
  }

  /**
   * Set tag values.
   *
   * @param values {@link List}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder tagValues(final List<String> values) {
    this.tagValues = values;
    return this;
  }

  /**
   * Set tag type.
   *
   * @param tagType {@link DocumentTagType}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder type(final DocumentTagType tagType) {
    this.type = tagType;
    return this;
  }

  /**
   * Set user id.
   *
   * @param id {@link String}
   * @return {@link DocumentTagRecordBuilder}
   */
  public DocumentTagRecordBuilder userId(final String id) {
    this.userId = id;
    return this;
  }
}
