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
package com.formkiq.module.events.folder;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Response to a Document Create Event.
 *
 */
@Reflectable
public class FolderEvent {

  /** S3 Key. */
  @Reflectable
  private String destinationPath;
  /** Document Id. */
  @Reflectable
  private String documentId;
  /** Folder SiteId. */
  @Reflectable
  private String siteId;
  /** S3 Key. */
  @Reflectable
  private String sourcePath;
  /** Event Type. */
  @Reflectable
  private String type;

  /**
   * constructor.
   */
  public FolderEvent() {

  }

  /**
   * Get Destination Path.
   * 
   * @return {@link String}
   */
  public String destinationPath() {
    return this.destinationPath;
  }

  /**
   * Set Destination Path.
   * 
   * @param path {@link String}
   * @return {@link FolderEvent}
   */
  public FolderEvent destinationPath(final String path) {
    this.destinationPath = path;
    return this;
  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String documentId() {
    return this.documentId;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   * @return {@link FolderEvent}
   */
  public FolderEvent documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Get Site Id.
   * 
   * @return {@link String}
   */
  public String siteId() {
    return this.siteId;
  }

  /**
   * Set Site Id.
   * 
   * @param id {@link String}
   * @return {@link FolderEvent}
   */
  public FolderEvent siteId(final String id) {
    this.siteId = id;
    return this;
  }

  /**
   * Get Source path.
   * 
   * @return {@link String}
   */
  public String sourcePath() {
    return this.sourcePath;
  }

  /**
   * Set Source Path.
   * 
   * @param path {@link String}
   * @return {@link FolderEvent}
   */
  public FolderEvent sourcePath(final String path) {
    this.sourcePath = path;
    return this;
  }

  /**
   * Get {@link FolderEvent} type.
   * 
   * @return {@link String}
   */
  public String type() {
    return this.type;
  }

  /**
   * Set {@link FolderEvent} type.
   * 
   * @param eventtype {@link String}
   * @return {@link FolderEvent}
   */
  public FolderEvent type(final String eventtype) {
    this.type = eventtype;
    return this;
  }
}
