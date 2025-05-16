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

import java.util.Collection;
import java.util.Map;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.validation.ValidationError;

/**
 * Attribute Service.
 */
public interface AttributeService {

  /**
   * Add Attribute.
   *
   * @param validationAccess {@link AttributeValidationAccess}
   * @param siteId {@link String}
   * @param key {@link String}
   * @param dataType {@link AttributeDataType}
   * @param type {@link AttributeType}
   */
  void addAttribute(AttributeValidationAccess validationAccess, String siteId, String key,
      AttributeDataType dataType, AttributeType type);

  /**
   * Add Attribute.
   *
   * @param validationAccess {@link AttributeValidationAccess}
   * @param siteId {@link String}
   * @param key {@link String}
   * @param dataType {@link AttributeDataType}
   * @param type {@link AttributeType}
   * @param allowReservedAttributeKey boolean
   */
  void addAttribute(AttributeValidationAccess validationAccess, String siteId, String key,
      AttributeDataType dataType, AttributeType type, boolean allowReservedAttributeKey);

  /**
   * Add Watermark Attribute.
   *
   * @param siteId {@link String}
   * @param key {@link String}
   * @param watermark {@link Watermark}
   */
  void addWatermarkAttribute(String siteId, String key, Watermark watermark);

  /**
   * Delete Attribute.
   *
   * @param validationAccess {@link AttributeValidationAccess}
   * @param siteId {@link String}
   * @param key {@link String}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> deleteAttribute(AttributeValidationAccess validationAccess,
      String siteId, String key);

  /**
   * Find {@link AttributeRecord}.
   * 
   * @param siteId Optional Grouping siteId
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link AttributeRecord}
   */
  PaginationResults<AttributeRecord> findAttributes(String siteId, PaginationMapToken token,
      int limit);

  /**
   * Get {@link AttributeRecord}.
   * 
   * @param siteId {@link String}
   * @param key {@link String}
   * @return {@link AttributeRecord}
   */
  AttributeRecord getAttribute(String siteId, String key);

  /**
   * Does Attribute exist.
   * 
   * @param siteId {@link String}
   * @param key {@link String}
   * @return boolean
   */
  boolean existsAttribute(String siteId, String key);

  /**
   * Get {@link AttributeRecord} by {@link Map}.
   * 
   * @param siteId {@link String}
   * @param attributeKeys {@link Collection} {@link String}
   * @return {@link AttributeRecord}
   */
  Map<String, AttributeRecord> getAttributes(String siteId, Collection<String> attributeKeys);

  /**
   * Set {@link AttributeType}.
   *
   * @param validationAccess {@link AttributeValidationAccess}
   * @param siteId {@link String}
   * @param key {@link String}
   * @param type {@link AttributeType}
   */
  void setAttributeType(AttributeValidationAccess validationAccess, String siteId, String key,
      AttributeType type);
}
