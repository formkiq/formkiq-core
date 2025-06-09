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
package com.formkiq.stacks.api.handler.documents;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import java.util.Date;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;

/**
 * 
 * {@link Function} to convert {@link AddDocumentTag} to {@link DocumentTag}.
 *
 */
public class AddDocumentTagToDocumentTag implements Function<AddDocumentTag, DocumentTag> {

  /** {@link String}. */
  private String docId;
  /** {@link Date}. */
  private Date date;
  /** User Id. */
  private String userId;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   * @param username {@link String}
   */
  public AddDocumentTagToDocumentTag(final String documentId, final String username) {
    this.docId = documentId;
    this.date = new Date();
    this.userId = username;
  }

  @Override
  public DocumentTag apply(final AddDocumentTag t) {

    DocumentTag tag = new DocumentTag();

    tag.setDocumentId(this.docId);
    tag.setKey(t.getKey());
    tag.setType(DocumentTagType.USERDEFINED);

    tag.setUserId(this.userId);
    tag.setValue(t.getValue());
    tag.setInsertedDate(this.date);

    if (!notNull(t.getValues()).isEmpty()) {
      tag.setValue(null);
      tag.setValues(t.getValues());
    }

    return tag;
  }

}
