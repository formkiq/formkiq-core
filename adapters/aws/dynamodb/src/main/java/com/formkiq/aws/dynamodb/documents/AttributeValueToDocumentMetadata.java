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
package com.formkiq.aws.dynamodb.documents;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_METADATA;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link Collection} {@link DocumentMetadata}.
 */
public class AttributeValueToDocumentMetadata
    implements Function<Map<String, AttributeValue>, Collection<DocumentMetadata>> {
  private static DocumentMetadata toMetadata(final String key, final AttributeValue av) {
    final String kk = key.substring(PREFIX_DOCUMENT_METADATA.length());

    if (av.s() != null) {
      return new DocumentMetadata(kk, av.s(), null);
    }

    List<String> strs = av.l().stream().map(AttributeValue::s).toList();
    return new DocumentMetadata(kk, null, strs);
  }

  @Override
  public Collection<DocumentMetadata> apply(final Map<String, AttributeValue> map) {
    List<DocumentMetadata> list =
        map.entrySet().stream().filter(e -> e.getKey().startsWith(PREFIX_DOCUMENT_METADATA))
            .map(e -> toMetadata(e.getKey(), e.getValue())).toList();
    return !list.isEmpty() ? list : null;
  }
}
