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

import com.formkiq.aws.dynamodb.builder.CustomDynamoDbAttributeBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;

/**
 * {@link CustomDynamoDbAttributeBuilder} for {@link DynamoDbKey}.
 */
public class DynamoDbKeyAttributeBuilder implements CustomDynamoDbAttributeBuilder {

  @Override
  public Map<String, AttributeValue> encode(final String name, final Object value) {

    List<Map<String, Object>> list = new ArrayList<>();

    if (value instanceof Collection<?> c) {
      c.forEach(val -> {
        if (val instanceof DynamoDbKey key) {
          list.add(Map.of(PK, key.pk(), SK, key.sk()));
        }
      });
    }

    return DynamoDbAttributeMapBuilder.builder().withList(name, list).build();
  }

  @Override
  public <T> T decode(final String name, final Map<String, AttributeValue> attrs) {

    Collection<DynamoDbKey> keys = null;
    AttributeValue av = attrs.get(name);

    if (av != null) {
      keys = av.l().stream()
          .map(a -> new DynamoDbKey(a.m().get(PK).s(), a.m().get(SK).s(), null, null, null, null))
          .toList();
    }

    return (T) keys;
  }
}
