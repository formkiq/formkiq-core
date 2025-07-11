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
package com.formkiq.aws.dynamodb.useractivities;

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;

/**
 * {@link BiFunction} to generate a {@link ChangeRecord}.
 */
public class AttributeValuesToChangeRecordFunction implements
    BiFunction<Map<String, AttributeValue>, Map<String, AttributeValue>, Map<String, ChangeRecord>> {

  /** DynamoDb Keys. */
  private static final Collection<String> KEYS = Set.of(PK, SK, GSI1_PK, GSI1_SK, GSI2_PK, GSI2_SK);
  /** Keys to Rename. */
  private final Map<String, String> rename;

  /**
   * constructor.
   * 
   * @param renameKeys {@link Map} of key renames.
   */
  public AttributeValuesToChangeRecordFunction(final Map<String, String> renameKeys) {
    this.rename = renameKeys;
  }

  @Override
  public Map<String, ChangeRecord> apply(final Map<String, AttributeValue> oldValue,
      final Map<String, AttributeValue> newValue) {

    AttributeValueToMap toMap = new AttributeValueToMap();

    Map<String, Object> oldValueMap = toMap.apply(oldValue);
    Map<String, Object> newValueMap = toMap.apply(newValue);

    return Stream.concat(oldValueMap.keySet().stream(), newValueMap.keySet().stream()).distinct()
        .filter(Predicate.not(KEYS::contains))
        // only those that changed
        .filter(k -> !Objects.equals(oldValueMap.get(k), newValueMap.get(k)))
        .collect(Collectors.toMap(k -> rename.getOrDefault(k, k), k -> {
          Object oldObj = oldValueMap.get(k);
          Object newObj = newValueMap.get(k);
          return new ChangeRecord(oldObj, newObj);
        }));
  }
}
