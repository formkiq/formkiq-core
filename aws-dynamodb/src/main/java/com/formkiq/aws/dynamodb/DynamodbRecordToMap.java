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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link DynamodbRecord} to {@link Map}.
 */
public class DynamodbRecordToMap implements Function<DynamodbRecord<?>, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(final DynamodbRecord<?> r) {

    Map<String, AttributeValue> data = r.getDataAttributes();

    Map<String, Object> m = new HashMap<>(data.size());

    for (Map.Entry<String, AttributeValue> e : data.entrySet()) {

      Object val = null;

      if (e.getValue().bool() != null) {
        val = e.getValue().bool();
      } else if (e.getValue().n() != null) {
        val = Double.valueOf(e.getValue().n());
      } else if (e.getValue().s() != null) {
        val = e.getValue().s();
      }

      if ("inserteddate".equals(e.getKey())) {
        m.put("insertedDate", val);
      } else {
        m.put(e.getKey(), val);
      }
    }

    return m;
  }

}
