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
package com.formkiq.stacks.dynamodb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;

/**
 * 
 * {@link Function} to convert {@link DynamicObject} to {@link DocumentTag}.
 *
 */
public class DynamicObjectToDocumentTag implements Function<DynamicObject, DocumentTag> {

  /** {@link SimpleDateFormat}. */
  private SimpleDateFormat df;

  /**
   * constructor.
   * 
   * @param formatter {@link SimpleDateFormat}
   */
  public DynamicObjectToDocumentTag(final SimpleDateFormat formatter) {
    this.df = formatter;
  }

  @Override
  public DocumentTag apply(final DynamicObject t) {
    DocumentTag tag = new DocumentTag();
    tag.setDocumentId(t.getString("documentId"));
    tag.setKey(t.getString("key"));

    if (t.hasString("type")) {
      tag.setType(DocumentTagType.valueOf(t.getString("type").toUpperCase()));
    } else {
      tag.setType(DocumentTagType.USERDEFINED);
    }
    tag.setUserId(t.getString("userId"));
    tag.setValue(t.getString("value"));

    Object ob = t.get("insertedDate");
    if (ob instanceof Date) {
      tag.setInsertedDate((Date) ob);
    } else if (this.df != null && ob instanceof String) {
      try {
        tag.setInsertedDate(this.df.parse(ob.toString()));
      } catch (ParseException e) {
        tag.setInsertedDate(new Date());
      }
    }

    if (t.containsKey("values")) {
      tag.setValue(null);
      tag.setValues(t.getStringList("values"));
    }

    if (tag.getInsertedDate() == null) {
      tag.setInsertedDate(new Date());
    }

    return tag;
  }

}
