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

import java.util.Collection;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;

/**
 * 
 * API Key Service.
 *
 */
public interface ApiKeysService {

  /**
   * Create API Key.
   * 
   * @param siteId {@link String}
   * @param name {@link String}
   * @param permissions {@link Collection} {@link ApiKeyPermission}
   * @param userId {@link String}
   * @return {@link String}
   */
  String createApiKey(String siteId, String name, Collection<ApiKeyPermission> permissions,
      String userId);

  /**
   * Delete Api Key.
   * 
   * @param siteId {@link String}
   * @param apiKey {@link String}
   * @return boolean
   */
  boolean deleteApiKey(String siteId, String apiKey);

  /**
   * Get Api Key.
   * 
   * @param apiKey {@link String}
   * @param masked boolean
   * @return boolean
   */
  ApiKey get(String apiKey, boolean masked);

  /**
   * Get List of API Keys.
   * 
   * @param siteId {@link String}
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link String}
   */
  PaginationResults<ApiKey> list(String siteId, PaginationMapToken token, int limit);

  /**
   * Mask Api Key.
   * 
   * @param apiKey {@link String}
   * @return {@link String}
   */
  String mask(String apiKey);
}
