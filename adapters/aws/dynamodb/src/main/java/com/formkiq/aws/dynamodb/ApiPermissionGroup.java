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
package com.formkiq.aws.dynamodb;

/**
 * FormKiQ client operation tag groups used by API permissions. These labels describe API categories
 * and do not themselves grant access.
 */
public enum ApiPermissionGroup {
  /** Access Control client tag. */
  ACCESS_CONTROL("Access Control"),
  /** Attributes client tag. */
  ATTRIBUTES("Attributes"),
  /** Document Actions client tag. */
  DOCUMENT_ACTIONS("Document Actions"),
  /** Document Attributes client tag. */
  DOCUMENT_ATTRIBUTES("Document Attributes"),
  /** Document Folders client tag. */
  DOCUMENT_FOLDERS("Document Folders"),
  /** Document Generation client tag. */
  DOCUMENT_GENERATION("Document Generation"),
  /** Document Shares client tag. */
  DOCUMENT_SHARES("Document Shares"),
  /** Document Tags client tag. */
  DOCUMENT_TAGS("Document Tags"),
  /** Document Versions client tag. */
  DOCUMENT_VERSIONS("Document Versions"),
  /** Document Workflows client tag. */
  DOCUMENT_WORKFLOWS("Document Workflows"),
  /** Documents client tag. */
  DOCUMENTS("Documents"),
  /** E-Signature client tag. */
  E_SIGNATURE("E-Signature"),
  /** Entity client tag. */
  ENTITY("Entity"),
  /** Examine Objects client tag. */
  EXAMINE_OBJECTS("Examine Objects"),
  /** Google Integration client tag. */
  GOOGLE_INTEGRATION("Google Integration"),
  /** Intelligent Document Processing client tag. */
  INTELLIGENT_DOCUMENT_PROCESSING("Intelligent Document Processing"),
  /** Mappings client tag. */
  MAPPINGS("Mappings"),
  /** Reminder Policy permission group. */
  REMINDER_POLICY("Reminder Policy"),
  /** Retention Policy permission group. */
  RETENTION_POLICY("Retention Policy"),
  /** Rulesets client tag. */
  RULESETS("Rulesets"),
  /** Schemas client tag. */
  SCHEMAS("Schemas"),
  /** Shortlinks client tag. */
  SHORTLINKS("Shortlinks"),
  /** System Management client tag. */
  SYSTEM_MANAGEMENT("System Management"),
  /** User Activities client tag. */
  USER_ACTIVITIES("User Activities"),
  /** User Management client tag. */
  USER_MANAGEMENT("User Management"),
  /** Webhooks client tag. */
  WEBHOOKS("Webhooks");

  /** Exact operation tag used by the FormKiQ client OpenAPI specification. */
  private final String tag;

  /**
   * Constructor.
   *
   * @param clientTag FormKiQ client operation tag
   */
  ApiPermissionGroup(final String clientTag) {
    this.tag = clientTag;
  }

  /**
   * Get the FormKiQ client operation tag.
   *
   * @return exact client tag label
   */
  public String getTag() {
    return this.tag;
  }
}
