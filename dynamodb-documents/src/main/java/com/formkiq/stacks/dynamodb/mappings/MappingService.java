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
package com.formkiq.stacks.dynamodb.mappings;

import java.util.Collection;
import java.util.List;

import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

/**
 * {@link MappingRecord} services.
 */
public interface MappingService {

  /**
   * Delete Mapping.
   * 
   * @param siteId {@link String}
   * @param mappingId {@link String}
   * @return boolean
   */
  boolean deleteMapping(String siteId, String mappingId);

  /**
   * Find {@link MappingRecord}.
   * 
   * @param siteId Optional Grouping siteId
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link MappingRecord}
   */
  PaginationResults<MappingRecord> findMappings(String siteId, PaginationMapToken token, int limit);

  /**
   * Get {@link MappingRecord}.
   * 
   * @param siteId {@link String}
   * @param mappingId {@link String}
   * @return {@link MappingRecord}
   */
  MappingRecord getMapping(String siteId, String mappingId);

  /**
   * Save / Set {@link MappingRecord}.
   * 
   * @param siteId {@link String}
   * @param mappingId {@link String}
   * @param mapping {@link Mapping}
   * @return {@link Collection} {@link ValidationError}
   * @throws ValidationException ValidationException
   */
  MappingRecord saveMapping(String siteId, String mappingId, Mapping mapping)
      throws ValidationException;

  /**
   * Get {@link MappingAttribute}.
   * 
   * @param mapping {@link MappingRecord}
   * @return {@link List} {@link MappingAttribute}
   */
  List<MappingAttribute> getAttributes(MappingRecord mapping);
}
