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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link DocumentTag}.
 *
 */
public class AttributeValueToDocumentTag
    implements Function<Map<String, AttributeValue>, DocumentTag> {

  /** {@link AttributeValueToInsertedDate}. */
  private AttributeValueToInsertedDate toDate = new AttributeValueToInsertedDate();

  // /** Site Id. */
  // private String site;

  /**
   * constructor.
   * 
   * @param siteId {@link String}
   */
  public AttributeValueToDocumentTag(final String siteId) {
    // this.site = siteId;
  }

  @Override
  public DocumentTag apply(final Map<String, AttributeValue> map) {
    String tagKey = null;
    String tagValue = null;

    if (map.containsKey("tagKey")) {
      tagKey = map.get("tagKey").s();
    }

    if (map.containsKey("tagValue")) {
      tagValue = map.get("tagValue").s();
    } else {
      tagValue = "";
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
    
    if (map.containsKey("tagValues")) {
      List<String> values = new AttributeValueToListOfStrings().apply(map.get("tagValues").l());
      tag.setValues(values);
      tag.setValue(null);
    }

    return tag;
  }
}
