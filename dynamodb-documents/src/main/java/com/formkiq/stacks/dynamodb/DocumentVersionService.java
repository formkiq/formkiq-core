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

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Interface for Document Versioning.
 *
 */
public interface DocumentVersionService {

  /** FormKiQ S3 Version. */
  String S3VERSION_ATTRIBUTE = "fk#s3version";
  /** FormKiQ Version. */
  String VERSION_ATTRIBUTE = "fk#version";

  /**
   * Add Document Versioning Attributes to Previous / Current Objects.
   * 
   * @param previous {@link Map}
   * @param current {@link Map}
   */
  void addDocumentVersionAttributes(Map<String, AttributeValue> previous,
      Map<String, AttributeValue> current);

  /**
   * Get DynamoDB Documents Versions Table Name.
   * 
   * @return {@link String}
   */
  String getDocumentVersionsTableName();

  /**
   * Initialize Service.
   * 
   * @param map {@link Map}
   */
  void initialize(Map<String, String> map);
}
