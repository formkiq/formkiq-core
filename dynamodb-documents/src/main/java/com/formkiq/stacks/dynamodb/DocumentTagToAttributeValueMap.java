/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.dynamodb;

import static com.formkiq.stacks.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.stacks.dynamodb.DbKeys.PK;
import static com.formkiq.stacks.dynamodb.DbKeys.PREFIX_TAG;
import static com.formkiq.stacks.dynamodb.DbKeys.PREFIX_TAGS;
import static com.formkiq.stacks.dynamodb.DbKeys.SK;
import static com.formkiq.stacks.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Function to Map {@link DocumentTag} to {@link Map}.
 *
 */
public class DocumentTagToAttributeValueMap
    implements Function<DocumentTag, List<Map<String, AttributeValue>>> {

  /** Site Id. */
  private String site;
  /** Document Id. */
  private String document;

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df;
  /** Primary Key Prefix. */
  private String keyPrefix;

  /**
   * constructor.
   * 
   * @param dateformat {@link SimpleDateFormat}
   * @param pkPrefix {@link String}
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  public DocumentTagToAttributeValueMap(final SimpleDateFormat dateformat, final String pkPrefix,
      final String siteId, final String documentId) {
    this.keyPrefix = pkPrefix;
    this.site = siteId;
    this.document = documentId;
    this.df = dateformat;
  }

  @Override
  public List<Map<String, AttributeValue>> apply(final DocumentTag tag) {
    
    List<Map<String, AttributeValue>> list = new ArrayList<>();
    if (isValueList(tag)) {
      
      int idx = 0;
      for (String tagValue : tag.getValues()) {
        Map<String, AttributeValue> pkvalues =
            buildTagAttributeValue(this.site, this.document, tag, tagValue, idx);
        list.add(pkvalues);
        idx++;
      }
      
    } else {
      Map<String, AttributeValue> pkvalues =
          buildTagAttributeValue(this.site, this.document, tag, tag.getValue(), -1);
      list.add(pkvalues);
    }
    return list;
  }
  
  /**
   * Build Tag {@link AttributeValue}.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tag {@link DocumentTag}
   * @param tagValue {@link String}
   * @param tagValueIndex int
   * @return {@link Map} {@link String} {@link AttributeValue}
   */
  private Map<String, AttributeValue> buildTagAttributeValue(final String siteId,
      final String documentId, final DocumentTag tag, final String tagValue,
      final int tagValueIndex) {

    String tagKey = tag.getKey();
    String fulldate = this.df.format(tag.getInsertedDate());

    DocumentTagType type = tag.getType() != null ? tag.getType() : DocumentTagType.USERDEFINED;

    Map<String, AttributeValue> pkvalues = new HashMap<String, AttributeValue>();

    String pk = createDatabaseKey(siteId, this.keyPrefix + documentId);
    String sk = tagValueIndex > -1 ? PREFIX_TAGS + tagKey + TAG_DELIMINATOR + "idx" + tagValueIndex
        : PREFIX_TAGS + tagKey;
    
    pkvalues.put(PK, AttributeValue.builder().s(pk).build());
    pkvalues.put(SK, AttributeValue.builder().s(sk).build());

    pkvalues.put(GSI1_PK, AttributeValue.builder()
        .s(createDatabaseKey(siteId, PREFIX_TAG + tagKey + TAG_DELIMINATOR + tagValue)).build());
    pkvalues.put(GSI1_SK,
        AttributeValue.builder().s(fulldate + TAG_DELIMINATOR + documentId).build());

    pkvalues.put(GSI2_PK,
        AttributeValue.builder().s(createDatabaseKey(siteId, PREFIX_TAG + tagKey)).build());
    pkvalues.put(GSI2_SK, AttributeValue.builder()
        .s(tagValue + TAG_DELIMINATOR + fulldate + TAG_DELIMINATOR + documentId).build());

    pkvalues.put("documentId", AttributeValue.builder().s(documentId).build());

    pkvalues.put("type", AttributeValue.builder().s(type.name()).build());

    if (tagKey != null) {
      pkvalues.put("tagKey", AttributeValue.builder().s(tagKey).build());
    }

    if (tagValue != null) {
      pkvalues.put("tagValue", AttributeValue.builder().s(tagValue).build());
    }

    if (tag.getValues() != null) {
      List<AttributeValue> values = tag.getValues().stream()
          .map(s -> AttributeValue.builder().s(s).build()).collect(Collectors.toList());
      pkvalues.put("tagValues", AttributeValue.builder().l(values).build());
    }
    
    if (tag.getUserId() != null) {
      pkvalues.put("userId", AttributeValue.builder().s(tag.getUserId()).build());
    }

    pkvalues.put("inserteddate", AttributeValue.builder().s(fulldate).build());

    return pkvalues;
  }

  /**
   * Is Has Value List.
   * @param tag {@link DocumentTag}
   * @return boolean
   */
  private boolean isValueList(final DocumentTag tag) {
    return tag.getValues() != null && !tag.getValues().isEmpty();
  }
}
