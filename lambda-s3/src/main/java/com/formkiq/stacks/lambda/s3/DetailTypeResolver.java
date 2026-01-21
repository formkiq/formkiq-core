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
package com.formkiq.stacks.lambda.s3;

import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * {@link Function} to convert {@link Collection} {@link Map} to DetailType {@link String}.
 */
public class DetailTypeResolver
    implements Function<Collection<Map<String, AttributeValue>>, String> {

  /** Default {@link String}. */
  static final String DEFAULT_DETAIL = "Unknown";

  /** Predicate → label mapping in order of priority. */
  private final List<Map.Entry<Predicate<Map<String, AttributeValue>>, String>> rules =
      List.of(Map.entry(hasTypeResource("CREATE", "documents"), "New Document Create Metadata"),
          Map.entry(hasTypeResource("NEW_VERSION", "documents"), "New Document Create Content"),
          Map.entry(hasTypeResource("UPDATE_VERSION", "documents"), "Document Create Content"),
          Map.entry(hasTypeResource("RESTORE", "documents"), "Document Restore"),
          Map.entry(hasTypeResource("UPDATE", "documents"), "Document Update Metadata"),
          Map.entry(hasTypeResource("CREATE", "documentAttributes"), "Document Create Metadata"),
          Map.entry(hasTypeResource("UPDATE", "documentAttributes"), "Document Update Metadata"),
          Map.entry(hasTypeResource("SOFT_DELETE", "documents"), "Document Soft Delete"),
          Map.entry(hasTypeResource("DELETE", "documents"), "Document Delete"),
          Map.entry(hasTypeResource("DELETE", "documentAttributes"), "Document Delete Metadata"));

  @Override
  public String apply(final Collection<Map<String, AttributeValue>> activities) {
    if (!notNull(activities).isEmpty()) {
      for (var rule : rules) {
        Predicate<Map<String, AttributeValue>> predicate = rule.getKey();
        if (activities.stream().anyMatch(predicate)) {
          return rule.getValue();
        }
      }
    }
    return DEFAULT_DETAIL;
  }

  private String avToString(final Map<String, AttributeValue> map, final String key) {
    AttributeValue av = map.get(key);
    return av != null ? av.s() : null;
  }

  /**
   * Builds a predicate for a given type/resource combination.
   * 
   * @param type {@link String}
   * @param resource {@link String}
   * @return {@link Predicate}
   */
  private Predicate<Map<String, AttributeValue>> hasTypeResource(final String type,
      final String resource) {
    return a -> {
      String t = DynamoDbTypes.toString(a.get("type"));
      String r = DynamoDbTypes.toString(a.get("resource"));
      return type.equalsIgnoreCase(t) && resource.equalsIgnoreCase(r);
    };
  }
}
