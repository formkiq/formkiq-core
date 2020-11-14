/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.dynamodb;

import static com.formkiq.stacks.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.stacks.dynamodb.DbKeys.PK;
import static com.formkiq.stacks.dynamodb.DbKeys.PREFIX_TAGS;
import static com.formkiq.stacks.dynamodb.DbKeys.SK;
import static com.formkiq.stacks.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Function to Map {@link DocumentTag} to {@link Map}.
 *
 */
public class DocumentTagToAttributeValueMap
    implements Function<DocumentTag, Map<String, AttributeValue>> {

  /** Site Id. */
  private String site;
  /** Document Id. */
  private String document;

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df;

  /**
   * constructor.
   * 
   * @param dateformat {@link SimpleDateFormat}
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  public DocumentTagToAttributeValueMap(final SimpleDateFormat dateformat, final String siteId,
      final String documentId) {
    this.site = siteId;
    this.document = documentId;
    this.df = dateformat;
  }

  @Override
  public Map<String, AttributeValue> apply(final DocumentTag tag) {
    Map<String, AttributeValue> pkvalues = buildTagAttributeValue(this.site, this.document, tag);
    return pkvalues;
  }

  /**
   * Build Tag {@link AttributeValue}.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tag {@link DocumentTag}
   * @return {@link Map} {@link String} {@link AttributeValue}
   */
  private Map<String, AttributeValue> buildTagAttributeValue(final String siteId,
      final String documentId, final DocumentTag tag) {

    String tagKey = tag.getKey();
    String tagValue = tag.getValue();
    DocumentTagType type = tag.getType() != null ? tag.getType() : DocumentTagType.USERDEFINED;

    Map<String, AttributeValue> pkvalues = new HashMap<String, AttributeValue>();

    pkvalues.put(PK,
        AttributeValue.builder().s(createDatabaseKey(siteId, PREFIX_TAGS + documentId)).build());
    pkvalues.put(SK, AttributeValue.builder().s(tagKey).build());

    pkvalues.put(GSI1_PK, AttributeValue.builder()
        .s(createDatabaseKey(siteId, tagKey + TAG_DELIMINATOR + tagValue)).build());
    pkvalues.put(GSI1_SK, AttributeValue.builder().s(documentId).build());

    pkvalues.put(GSI2_PK, AttributeValue.builder().s(createDatabaseKey(siteId, tagKey)).build());
    pkvalues.put(GSI2_SK,
        AttributeValue.builder().s(tagValue + TAG_DELIMINATOR + documentId).build());

    pkvalues.put("documentId", AttributeValue.builder().s(documentId).build());

    pkvalues.put("type", AttributeValue.builder().s(type.name()).build());

    if (tagKey != null) {
      pkvalues.put("tagKey", AttributeValue.builder().s(tagKey).build());
    }

    if (tagValue != null) {
      pkvalues.put("tagValue", AttributeValue.builder().s(tagValue).build());
    }

    if (tag.getUserId() != null) {
      pkvalues.put("userId", AttributeValue.builder().s(tag.getUserId()).build());
    }

    String fulldate = this.df.format(tag.getInsertedDate());
    pkvalues.put("inserteddate", AttributeValue.builder().s(fulldate).build());

    return pkvalues;
  }
}
