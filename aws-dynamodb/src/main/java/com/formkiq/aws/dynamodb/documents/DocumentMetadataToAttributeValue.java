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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_METADATA;


/**
 * Converts a collection of {@link DocumentMetadata} objects into a DynamoDB attribute map.
 *
 * <p>
 * Each {@link DocumentMetadata} entry is converted into a map entry where:
 *
 * <ul>
 * <li>The key is {@code PREFIX_DOCUMENT_METADATA + metadata.key()}</li>
 * <li>If {@code metadata.value()} is non-null, a String {@link AttributeValue} is created</li>
 * <li>If {@code metadata.values()} is non-null, a List {@link AttributeValue} is created</li>
 * </ul>
 *
 * <p>
 * This class performs the inverse transformation of {@code AttributeValueToDocumentMetadata}.
 *
 * <p>
 * If the input collection is {@code null} or empty, an empty map is returned.
 */
public class DocumentMetadataToAttributeValue
    implements Function<Collection<DocumentMetadata>, Map<String, AttributeValue>> {

  @Override
  public Map<String, AttributeValue> apply(final Collection<DocumentMetadata> metadataCollection) {

    if (metadataCollection == null || metadataCollection.isEmpty()) {
      return Map.of();
    }

    return metadataCollection.stream().collect(
        Collectors.toMap(meta -> PREFIX_DOCUMENT_METADATA + meta.key(), this::toAttributeValue));
  }

  private AttributeValue toAttributeValue(final DocumentMetadata meta) {

    if (meta.value() != null) {
      return AttributeValue.builder().s(meta.value()).build();
    }

    return AttributeValue.builder()
        .l(meta.values().stream().map(v -> AttributeValue.builder().s(v).build()).toList()).build();
  }
}
