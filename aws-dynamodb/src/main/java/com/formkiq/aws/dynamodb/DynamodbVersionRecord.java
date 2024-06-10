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
package com.formkiq.aws.dynamodb;

import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;

/**
 * Versioned {@link DynamodbRecord}.
 * 
 * @param <T> Type of DynamodbRecord
 */
public interface DynamodbVersionRecord<T> extends DynamodbRecord<T> {

  /** Archive Key Prefix. */
  String ARCHIVE_KEY_PREFIX = "Archive";

  /**
   * Get DynamoDb versioning PK.
   *
   * @param siteId {@link String}
   * @return {@link String}
   */
  String pkVersion(String siteId);

  /**
   * Get DynamoDb versioning SK.
   *
   * @return {@link String}
   */
  String skVersion();

  /**
   * Get Inserted Date {@link String}.
   * 
   * @return {@link String}
   */
  default String getInsertedDate() {
    SimpleDateFormat df = DateUtil.getIsoDateFormatter();
    return df.format(new Date());
  }

  /**
   * Update Attributes to Version.
   * 
   * @param siteId {@link String}
   * @param attrs {@link Map}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> updateAttributesToVersioned(final String siteId,
      final Map<String, AttributeValue> attrs) {

    Map<String, AttributeValue> updated = new HashMap<>(attrs);

    for (String key : List.of(PK, SK, GSI1_PK, GSI1_SK, GSI2_PK, GSI2_SK)) {
      if (attrs.containsKey(key)) {
        updated.put(ARCHIVE_KEY_PREFIX + key, attrs.get(key));
      }
    }

    updated.put(PK, AttributeValue.fromS(pkVersion(siteId)));
    updated.put(SK, AttributeValue.fromS(skVersion()));

    return updated;
  }
}
