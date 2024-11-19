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

import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.SK;

/**
 * Versioned {@link DynamodbRecord}.
 * 
 * @param <T> Type of DynamodbRecord
 */
public interface DynamodbVersionRecord<T> extends DynamodbRecord<T> {

  /** Archive Key Prefix. */
  String ARCHIVE_KEY_PREFIX = "archive#";

  /**
   * Get DynamoDb versioning SK.
   *
   * @return {@link String}
   */
  String skVersion();

  /**
   * Update Attributes to Version.
   * 
   * @param siteId {@link String}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> getVersionedAttributes(final String siteId) {

    Map<String, AttributeValue> attrs = new HashMap<>(getAttributes(siteId));

    for (String key : List.of(SK)) {
      if (attrs.containsKey(key)) {
        attrs.put(ARCHIVE_KEY_PREFIX + key, attrs.get(key));
      }
    }

    attrs.put(SK, AttributeValue.fromS(skVersion()));

    return attrs;
  }

  default String truncateSk(final String sk) {
    final int len = 800;
    return Strings.truncate(sk, len);
  }
}
