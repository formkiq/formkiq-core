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
import java.util.Locale;

/**
 * Locale Services.
 */
public interface LocaleService {

  /**
   * Delete {@link LocaleTypeRecord}.
   * 
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param itemKey {@link String}
   * @return boolean
   */
  boolean delete(String siteId, String locale, String itemKey);

  /**
   * Delete Locale.
   *
   * @param siteId {@link String}
   * @param locale {@link String}
   * @return boolean
   */
  List<ValidationError> deleteLocale(String siteId, String locale);

  /**
   * Find {@link LocaleTypeRecord}.
   * 
   * @param siteId {@link String}
   * @param locale {@link String}
   * @param itemKey {@link String}
   * @return LocaleRecord
   */
  LocaleTypeRecord find(String siteId, String locale, String itemKey);

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
  Pagination<LocaleTypeRecord> findAll(String siteId, String locale, LocaleResourceType itemType,
      String token, int limit);

  /**
   * Find All Locale.
   *
   * @param siteId {@link String}
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return Pagination
   */
  Pagination<LocaleRecord> findLocales(String siteId, String token, int limit);

  /**
   * Save {@link LocaleTypeRecord}.
   *
   * @param siteId {@link String}
   * @param records {@link Collection} {@link LocaleTypeRecord}
   * @return List {@link ValidationError}
   */
  List<ValidationError> save(String siteId, Collection<LocaleTypeRecord> records);

  /**
   * Save {@link LocaleRecord}.
   *
   * @param siteId {@link String}
   * @param locale {@link Locale}
   * @return List {@link ValidationError}
   */
  List<ValidationError> saveLocale(String siteId, String locale);
}
