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

import java.util.List;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.actions.Action;

/**
 * Add Document Request holder.
 */
@Reflectable
public class AddDocumentRequest {

  /** Document Actions. */
  private List<Action> actions;
  /** Document Attribute. */
  private List<DocumentAttribute> attributes;
  /** Document content. */
  private String content;
  /** Content Type. */
  private String contentType;
  /** Deep Link Path. */
  private String deepLinkPath;
  /** Document Id. */
  private String documentId;
  /** Child documents. */
  private List<AddDocumentRequest> documents;
  /** Is base 64. */
  private boolean isBase64;
  /** Document meta data. */
  private List<DocumentMetadata> metadata;
  /** Document Path. */
  private String path;
  /** Document Tags. */
  private List<AddDocumentTag> tags;
  /** Tag Schema Id. */
  private String tagSchemaId;

  /**
   * constructor.
   */
  public AddDocumentRequest() {

  }

  /**
   * Get Document Actions.
   * 
   * @return {@link List} {@link Action}
   */
  public List<Action> getActions() {
    return this.actions;
  }

  /**
   * Get Document Attributes.
   * 
   * @return {@link List} {@link DocumentAttribute}
   */
  public List<DocumentAttribute> getAttributes() {
    return this.attributes;
  }

  /**
   * Document Content.
   * 
   * @return {@link String}
   */
  public String getContent() {
    return this.content;
  }

  /**
   * Get Content Type.
   * 
   * @return {@link String}
   */
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Get Deep Link Path.
   * 
   * @return {@link String}
   */
  public String getDeepLinkPath() {
    return this.deepLinkPath;
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
   * Get Child Documents.
   * 
   * @return {@link List} {@link AddDocumentRequest}
   */
  public List<AddDocumentRequest> getDocuments() {
    return this.documents;
  }

  /**
   * Get Document Metadata.
   * 
   * @return {@link List} {@link DocumentMetadata}
   */
  public List<DocumentMetadata> getMetadata() {
    return this.metadata;
  }

  /**
   * Get Document Path.
   * 
   * @return {@link String}
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Get Document Tags.
   * 
   * @return {@link List} {@link AddDocumentTag}
   */
  public List<AddDocumentTag> getTags() {
    return this.tags;
  }

  /**
   * Get Tag Schema Id.
   * 
   * @return {@link String}
   */
  public String getTagSchemaId() {
    return this.tagSchemaId;
  }

  /**
   * Is Base 64.
   * 
   * @return boolean
   */
  public boolean isBase64() {
    return this.isBase64;
  }

  /**
   * Set Document Actions.
   * 
   * @param documentActions {@link List} {@link Action}
   */
  public void setActions(final List<Action> documentActions) {
    this.actions = documentActions;
  }

  /**
   * Set Document Attributes.
   * 
   * @param documentAttributes {@link List} {@link DocumentAttribute}
   */
  public void setAttributes(final List<DocumentAttribute> documentAttributes) {
    this.attributes = documentAttributes;
  }

  /**
   * Set Base 64.
   * 
   * @param contentIsBase64 boolean
   */
  public void setBase64(final boolean contentIsBase64) {
    this.isBase64 = contentIsBase64;
  }

  /**
   * Set Document Content.
   * 
   * @param documentContent {@link String}
   */
  public void setContent(final String documentContent) {
    this.content = documentContent;
  }

  /**
   * Set Content Type.
   * 
   * @param documentContentType {@link String}
   */
  public void setContentType(final String documentContentType) {
    this.contentType = documentContentType;
  }

  /**
   * Set Deep Link Path.
   * 
   * @param documentDeepLinkPath {@link String}
   */
  public void setDeepLinkPath(final String documentDeepLinkPath) {
    this.deepLinkPath = documentDeepLinkPath;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   */
  public void setDocumentId(final String id) {
    this.documentId = id;
  }

  /**
   * Set Child Documents.
   * 
   * @param childDocuments {@link List} {@link AddDocumentRequest}
   */
  public void setDocuments(final List<AddDocumentRequest> childDocuments) {
    this.documents = childDocuments;
  }

  /**
   * Set Document Metadata.
   * 
   * @param documentMetadata {@link List} {@link DocumentMetadata}
   */
  public void setMetadata(final List<DocumentMetadata> documentMetadata) {
    this.metadata = documentMetadata;
  }

  /**
   * Set Document Path.
   * 
   * @param documentPath {@link String}
   */
  public void setPath(final String documentPath) {
    this.path = documentPath;
  }

  /**
   * Set Document Tags.
   * 
   * @param documentTags {@link List} {@link AddDocumentTag}
   */
  public void setTags(final List<AddDocumentTag> documentTags) {
    this.tags = documentTags;
  }

  /**
   * Set Tag Schema Id.
   * 
   * @param documentTagSchemaId {@link String}
   */
  public void setTagSchemaId(final String documentTagSchemaId) {
    this.tagSchemaId = documentTagSchemaId;
  }
}
