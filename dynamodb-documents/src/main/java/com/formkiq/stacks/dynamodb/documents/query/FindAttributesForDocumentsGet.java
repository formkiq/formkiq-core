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
package com.formkiq.stacks.dynamodb.documents.query;

import com.formkiq.aws.dynamodb.DynamoDbGet;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.validation.ValidationBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link DynamoDbQuery} for finding multiple attributes for multiple documents.
 */
public class FindAttributesForDocumentsGet implements DynamoDbGet {

  /** {@link Collection} {@link String}. */
  private Collection<String> documents;
  /** {@link Map}. */
  private final Map<String, Collection<DocumentAttributeRecord>> eq = new HashMap<>();

  public FindAttributesForDocumentsGet() {}

  /**
   * Set the folder path to retrieve documents from.
   * 
   * @param documentIds {@link Collection} {@link String}
   * @return this builder
   */
  public FindAttributesForDocumentsGet documentIds(final Collection<String> documentIds) {
    this.documents = documentIds;
    return this;
  }

  /**
   * Add eq clause.
   * 
   * @param attributeKey {@link String}
   * @param value {@link String}
   * @return this builder
   */
  public FindAttributesForDocumentsGet eq(final String attributeKey, final Object value) {

    this.documents.forEach(documentId -> {

      DocumentAttributeRecord r =
          new DocumentAttributeRecord().setDocumentId(documentId).setKey(attributeKey);

      if (value instanceof String s) {
        r.setStringValue(s);
      } else if (value instanceof Double d) {
        r.setNumberValue(d);
      } else if (value instanceof Boolean b) {
        r.setBooleanValue(b);
      }

      r.updateValueType();

      eq.computeIfAbsent(documentId, k -> new ArrayList<>()).add(r);
    });

    return this;
  }

  @Override
  public Collection<DynamoDbKey> build(final String siteId) {

    validate();

    return eq.entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(r -> new DynamoDbKey(r.pk(siteId), r.sk(),
            r.pkGsi1(siteId), r.skGsi1(), r.pkGsi2(siteId), r.skGsi2())).toList().stream())
        .toList();
  }

  private void validate() {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("documentIds", documents, "'documentIds' is required");
    vb.isRequired(null, eq, "'eq' is required");
    vb.check();
  }
}
