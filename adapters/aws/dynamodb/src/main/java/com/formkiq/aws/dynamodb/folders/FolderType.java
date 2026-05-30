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
package com.formkiq.aws.dynamodb.folders;

/**
 * Represents the supported folder-related document types.
 *
 * <p>
 * This enum provides a type-safe representation of the underlying string values stored in the
 * database ("folder", "file").
 */
public enum FolderType {

  /**
   * Represents a folder container.
   */
  FOLDER("folder"),

  /**
   * Represents a file within a folder.
   */
  FILE("file");

  /**
   * Is {@link String} a file.
   * 
   * @param value {@link String}
   * @return boolean
   */
  public static boolean isFile(final String value) {
    return FILE.value.equals(value);
  }

  /**
   * Is {@link String} a folder.
   * 
   * @param value {@link String}
   * @return boolean
   */
  public static boolean isFolder(final String value) {
    return FOLDER.value.equals(value);
  }

  /** Folder type value. */
  private final String value;

  /**
   * Creates a FolderType with its persistent string folderType.
   *
   * @param folderType the string representation used in storage
   */
  FolderType(final String folderType) {
    this.value = folderType;
  }

  /**
   * Returns the string value used for persistence.
   *
   * @return the database representation of this type
   */
  public String getValue() {
    return this.value;
  }
}
