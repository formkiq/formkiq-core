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
import static com.formkiq.stacks.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link DocumentTag}.
 *
 */
public class AttributeValueToDocumentTag
    implements Function<Map<String, AttributeValue>, DocumentTag> {

  /** {@link AttributeValueToInsertedDate}. */
  private AttributeValueToInsertedDate toDate = new AttributeValueToInsertedDate();

  /** Site Id. */
  private String site;

  /**
   * constructor.
   * 
   * @param siteId {@link String}
   */
  public AttributeValueToDocumentTag(final String siteId) {
    this.site = siteId;
  }

  @Override
  public DocumentTag apply(final Map<String, AttributeValue> map) {

    String tagValue = null;
    String pk = map.get(GSI1_PK).s();
    String[] strs = pk.split(TAG_DELIMINATOR);

    String tagKey = resetDatabaseKey(this.site, strs[0]);

    if (strs.length > 1) {
      tagValue = strs[1];

      if (strs[1].equals("null")) {
        tagValue = null;
      }
    }

    Date date = this.toDate.apply(map);
    String userId = map.containsKey("userId") ? map.get("userId").s() : null;

    DocumentTag tag = new DocumentTag(null, tagKey, tagValue, date, userId);

    if (map.containsKey("type")) {
      tag.setType(DocumentTagType.valueOf(map.get("type").s()));
    }

    if (map.containsKey("documentId")) {
      tag.setDocumentId(map.get("documentId").s());
    }

    return tag;
  }
}
