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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;

/**
 * Convert {@link DocumentAttribute} to {@link DocumentAttributeRecord}.
 */
public class DocumentAttributeToDocumentAttributeRecord
    implements Function<DocumentAttribute, Collection<DocumentAttributeRecord>> {

  /** Document Id. */
  private String docId;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   */
  public DocumentAttributeToDocumentAttributeRecord(final String documentId) {
    this.docId = documentId;
  }

  private void addToList(final Collection<DocumentAttributeRecord> list,
      final DocumentAttributeValueType valueType, final String key, final String stringValue,
      final Boolean boolValue, final Double numberValue) {

    DocumentAttributeRecord a = new DocumentAttributeRecord();
    a.key(key);
    a.documentId(this.docId);
    a.stringValue(stringValue);
    a.booleanValue(boolValue);
    a.numberValue(numberValue);
    a.valueType(valueType);

    list.add(a);
  }

  @Override
  public Collection<DocumentAttributeRecord> apply(final DocumentAttribute a) {
    Collection<DocumentAttributeRecord> c = new ArrayList<>();

    boolean used = false;
    String key = a.getKey();

    if (!isEmpty(a.getStringValue())) {
      used = true;
      addToList(c, DocumentAttributeValueType.STRING, key, a.getStringValue(), null, null);
    }

    if (a.getNumberValue() != null) {
      used = true;
      addToList(c, DocumentAttributeValueType.NUMBER, key, null, null, a.getNumberValue());
    }

    if (a.getBooleanValue() != null) {
      used = true;
      addToList(c, DocumentAttributeValueType.BOOLEAN, key, null, a.getBooleanValue(), null);
    }

    for (String stringValue : notNull(a.getStringValues())) {
      used = true;
      addToList(c, DocumentAttributeValueType.STRING, key, stringValue, null, null);
    }

    for (Double numberValue : notNull(a.getNumberValues())) {
      used = true;
      addToList(c, DocumentAttributeValueType.NUMBER, key, null, null, numberValue);
    }

    if (!used) {
      addToList(c, DocumentAttributeValueType.NO_VALUE, key, null, null, null);
    }

    return c;
  }
}
