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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * 
 * {@link Function} to convert {@link List} {@link Map} {@link AttributeValue} to
 * {@link WriteRequest}.
 *
 */
public class AttributeValuesToWriteRequests
    implements Function<List<Map<String, AttributeValue>>, Map<String, Collection<WriteRequest>>> {

  /** Table Name. */
  private String tableName;

  /**
   * constructor.
   * 
   * @param dynamoDbtableName {@link String}
   */
  public AttributeValuesToWriteRequests(final String dynamoDbtableName) {
    this.tableName = dynamoDbtableName;
  }

  @Override
  public Map<String, Collection<WriteRequest>> apply(
      final List<Map<String, AttributeValue>> values) {

    AttributeValueToWriteRequest trans = new AttributeValueToWriteRequest(this.tableName);

    List<WriteRequest> list = new ArrayList<>();

    values.forEach(value -> {
      Map<String, WriteRequest> map = trans.apply(value);
      for (Map.Entry<String, WriteRequest> e : map.entrySet()) {
        list.add(e.getValue());
      }
    });

    Map<String, Collection<WriteRequest>> items = Map.of(this.tableName, list);
    return items;
  }

}
