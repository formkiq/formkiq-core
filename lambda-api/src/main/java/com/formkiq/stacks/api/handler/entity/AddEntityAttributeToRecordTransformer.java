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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.strings.Strings.isEmpty;

/**
 * {@link Function} to convert {@link AddEntityAttribute} {@link DocumentAttributeRecord}.
 */
public class AddEntityAttributeToRecordTransformer
    implements Function<AddEntityAttribute, Collection<DocumentAttributeRecord>> {
  @Override
  public Collection<DocumentAttributeRecord> apply(final AddEntityAttribute attr) {
    List<DocumentAttributeRecord> strings =
        notNull(attr.stringValues()).stream().map(s -> convert(attr, s)).toList();
    List<DocumentAttributeRecord> numbers =
        notNull(attr.numberValues()).stream().map(s -> convert(attr, s)).toList();

    List<DocumentAttributeRecord> list =
        new ArrayList<>(Stream.concat(strings.stream(), numbers.stream()).toList());

    if (!isEmpty(attr.stringValue())) {
      list.add(convert(attr, attr.stringValue()));
    }

    if (attr.numberValue() != null) {
      list.add(convert(attr, attr.numberValue()));
    }

    if (attr.booleanValue() != null) {
      list.add(convert(attr, attr.booleanValue()));
    }

    return list;
  }

  private DocumentAttributeRecord.Builder convert(final AddEntityAttribute attr) {
    String username = ApiAuthorization.getAuthorization().getUsername();
    return DocumentAttributeRecord.builder().key(attr.key()).userId(username);
  }

  private DocumentAttributeRecord convert(final AddEntityAttribute attr, final String stringValue) {
    return convert(attr).stringValue(stringValue).build().updateValueType();
  }

  private DocumentAttributeRecord convert(final AddEntityAttribute attr, final Double doubleValue) {
    return convert(attr).numberValue(doubleValue).build().updateValueType();
  }

  private DocumentAttributeRecord convert(final AddEntityAttribute attr,
      final Boolean booleanValue) {
    return convert(attr).booleanValue(booleanValue).build().updateValueType();
  }
}
