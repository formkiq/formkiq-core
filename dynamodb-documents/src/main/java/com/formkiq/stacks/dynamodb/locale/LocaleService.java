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
package com.formkiq.stacks.dynamodb.locale;

import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.base64.Pagination;
import com.formkiq.validation.ValidationError;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Locale Services.
 */
public interface LocaleService {

  /**
   * Save {@link LocaleRecord}.
   *
   * @param siteId {@link String}
   * @param records {@link Collection} {@link LocaleRecord}
   * @return List {@link ValidationError}
   */
  List<ValidationError> save(String siteId, Collection<LocaleRecord> records);

  /**
   * Get Locale for Interface.
   *
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param interfaceKeys {@link Collection} {@link String}
   * @return LocaleRecord
   */
  List<LocaleRecord> getLocaleInterface(String siteId, String locale,
      Collection<String> interfaceKeys);

  /**
   * Get Locale for Schema Attribute.
   *
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param attributeKeys {@link Map} {@link Collection} {@link String}
   * @return LocaleRecord
   */
  List<LocaleRecord> getLocaleSchema(String siteId, String locale,
      Map<String, Collection<String>> attributeKeys);

  /**
   * Get Locale for Classification Attribute.
   *
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param classificationId {@link String}
   * @param attributeKeys {@link Map} {@link Collection} {@link String}
   * @return LocaleRecord
   */
  List<LocaleRecord> getLocaleSchema(String siteId, String locale, String classificationId,
      Map<String, Collection<String>> attributeKeys);

  /**
   * Find All.
   * 
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param itemType {@link LocaleResourceType}
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return Pagination
   */
  Pagination<LocaleRecord> findAll(String siteId, String locale, LocaleResourceType itemType,
      String token, int limit);

  /**
   * Find {@link LocaleRecord}.
   * 
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param itemKey {@link String}
   * @return LocaleRecord
   */
  LocaleRecord find(String siteId, String locale, String itemKey);

  /**
   * Delete {@link LocaleRecord}.
   * 
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param itemKey {@link String}
   * @return boolean
   */
  boolean delete(String siteId, String locale, String itemKey);
}
