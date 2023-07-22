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
package com.formkiq.stacks.dynamodb;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link DocumentTag}.
 *
 */
public class AttributeValueToGlobalMetaFolder
    implements Function<Map<String, AttributeValue>, Map<String, Object>>, DbKeys {

  /** {@link AttributeValueToDate}. */
  private AttributeValueToDate toInsertedDateDate = new AttributeValueToDate("inserteddate");
  /** {@link AttributeValueToDate}. */
  private AttributeValueToDate toLastModifiedDate = new AttributeValueToDate("lastModifiedDate");

  /**
   * constructor.
   * 
   */
  public AttributeValueToGlobalMetaFolder() {}

  @Override
  public Map<String, Object> apply(final Map<String, AttributeValue> map) {

    Map<String, Object> result = new HashMap<>();

    if (map.get(PK).s().contains(GLOBAL_FOLDER_TAGS)) {

      result.put("value", map.get("tagKey").s());

    } else {

      String parent = map.get(PK).s().substring(map.get(PK).s().lastIndexOf(TAG_DELIMINATOR) + 1);
      String documentId = map.get("documentId").s();
      String path = map.get("path").s();

      String key = parent + TAG_DELIMINATOR + path;

      result.put("path", path);
      result.put("documentId", documentId);
      result.put("folder", Boolean.TRUE);
      result.put("indexKey", key);

      Date insertedDate = this.toInsertedDateDate.apply(map);
      result.put("insertedDate", insertedDate);

      Date lastmodifedDate = this.toLastModifiedDate.apply(map);
      result.put("lastModifiedDate", lastmodifedDate);

      String userId = map.containsKey("userId") ? map.get("userId").s() : null;
      result.put("userId", userId);
    }

    return result;
  }
}
