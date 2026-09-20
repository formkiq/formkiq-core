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

import com.formkiq.aws.dynamodb.attributes.AttributeAccessApproval;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;

/**
 * Options to use when saving a document.
 *
 */
public class SaveDocumentOptions {

  /** Time to Live. */
  private String timeToLive;
  /** {@link AttributeAccessApproval}. */
  private AttributeAccessApproval accessApproval;
  /** Whether to skip the Document Event Bridge. */
  private boolean skipDocumentEventBridge;

  /**
   * constructor.
   */
  public SaveDocumentOptions() {

  }

  /**
   * Set attribute access approval.
   *
   * @param approval {@link AttributeAccessApproval}
   * @return {@link SaveDocumentOptions}
   */
  public SaveDocumentOptions accessApproval(final AttributeAccessApproval approval) {
    this.accessApproval = approval;
    return this;
  }

  /**
   * Get {@link AttributeAccessApproval}.
   *
   * @return {@link AttributeAccessApproval}
   */
  public AttributeAccessApproval getAccessApproval() {
    return this.accessApproval;
  }

  /**
   * Is Validation Access.
   * 
   * @return boolean
   */
  public AttributeValidationAccess getValidationAccess() {
    return this.accessApproval != null ? this.accessApproval.defaultAccess() : null;
  }

  /**
   * Whether to skip document event bridge.
   * 
   * @return boolean
   */
  public boolean isSkipDocumentEventBridge() {
    return this.skipDocumentEventBridge;
  }

  /**
   * Set Skip Document Event Bridge Event.
   * 
   * @param skip boolean
   * @return SaveDocumentOptions
   */
  public SaveDocumentOptions setSkipDocumentEventBridge(final boolean skip) {
    this.skipDocumentEventBridge = skip;
    return this;
  }

  /**
   * Get Time to Live.
   * 
   * @return {@link String}
   */
  public String timeToLive() {
    return this.timeToLive;
  }

  /**
   * Set Time to Live.
   * 
   * @param ttl {@link String}
   * @return {@link SaveDocumentOptions}
   */
  public SaveDocumentOptions timeToLive(final String ttl) {
    this.timeToLive = ttl;
    return this;
  }

  /**
   * Set Is Admin Role.
   * 
   * @param access {@link AttributeValidationAccess}
   * @return {@link SaveDocumentOptions}
   */
  public SaveDocumentOptions validationAccess(final AttributeValidationAccess access) {
    this.accessApproval = new AttributeAccessApproval(access);
    return this;
  }
}
