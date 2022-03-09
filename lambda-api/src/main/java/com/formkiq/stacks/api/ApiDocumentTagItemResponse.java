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
package com.formkiq.stacks.api;

import java.util.Date;
import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.lambda.apigateway.ApiResponse;

/** API Response Object. */
@Reflectable
public class ApiDocumentTagItemResponse implements ApiResponse {

  /** Document Tag Key. */
  @Reflectable
  private String key;
  /** Document String Tag Value. */
  @Reflectable
  private String value;
  /** Document String Tag Value. */
  @Reflectable
  private List<String> values;
  /** User Id. */
  @Reflectable
  private String userId;
  /** Document Inserted Date. */
  @Reflectable
  private Date insertedDate;
  /** Tag Type. */
  @Reflectable
  private String type;
  /** Document Id. */
  @Reflectable
  private String documentId;

  /** constructor. */
  public ApiDocumentTagItemResponse() {}

  @Override
  public String getNext() {
    return null;
  }

  @Override
  public String getPrevious() {
    return null;
  }

  /**
   * Get Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Set Key.
   * 
   * @param tagkey {@link String}
   */
  public void setKey(final String tagkey) {
    this.key = tagkey;
  }

  /**
   * Get Value.
   * 
   * @return {@link String}
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Set Value.
   * 
   * @param s {@link String}
   */
  public void setValue(final String s) {
    this.value = s;
  }

  /**
   * Get Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getValues() {
    return this.values;
  }

  /**
   * Set Values.
   * 
   * @param list {@link List} {@link String}
   */
  public void setValues(final List<String> list) {
    this.values = list;
  }
  
  /**
   * Get UserId.
   * 
   * @return {@link String}
   */
  public String getUserId() {
    return this.userId;
  }

  /**
   * Set User Id.
   *
   * @param user {@link String}
   */
  public void setUserId(final String user) {
    this.userId = user;
  }

  /**
   * Get Inserted Date.
   * 
   * @return {@link Date}
   */
  public Date getInsertedDate() {
    return this.insertedDate != null ? (Date) this.insertedDate.clone() : null;
  }

  /**
   * Set Inserted Date.
   *
   * @param date {@link Date}
   */
  public void setInsertedDate(final Date date) {
    this.insertedDate = date != null ? (Date) date.clone() : null;
  }

  /**
   * Get Tag Type.
   * 
   * @return {@link String}
   */
  public String getType() {
    return this.type;
  }

  /**
   * Set Tag Type.
   * 
   * @param tagtype {@link String}
   */
  public void setType(final String tagtype) {
    this.type = tagtype;
  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   */
  public void setDocumentId(final String id) {
    this.documentId = id;
  }
}
