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
package com.formkiq.stacks.dynamodb.attributes;

/**
 * Attribute Validation method.
 */
public enum AttributeValidationAccess {

  /** Admin Create Access. */
  ADMIN_CREATE(true, true),
  /** Admin Delete Access. */
  ADMIN_DELETE(true, false),
  /** Admin Set Access. */
  ADMIN_SET(true, false),
  /** Admin Update Access. */
  ADMIN_UPDATE(true, false),
  /** Create Access. */
  CREATE(false, true),
  /** Delete Access. */
  DELETE(false, false),
  /** None Access. */
  NONE(false, false),
  /** Set Access. */
  SET(false, false),
  /** Update Access. */
  UPDATE(false, false),
  /** Admin Set Item. */
  ADMIN_SET_ITEM(true, false),
  /** Set Item. */
  SET_ITEM(false, false);

  /** Is Access ADMIN or Govern Role. */
  private final boolean adminOrGovernRole;
  /** Is Create. */
  private final boolean create;

  AttributeValidationAccess(final boolean isAdminOrGovernRole, final boolean isCreate) {
    this.adminOrGovernRole = isAdminOrGovernRole;
    this.create = isCreate;
  }

  public boolean isAdminOrGovernRole() {
    return adminOrGovernRole;
  }

  public boolean isCreate() {
    return create;
  }
}
