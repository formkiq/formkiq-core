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
package com.formkiq.plugins.tagschema;

import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.validation.ValidationError;

/**
 * 
 * Document Events.
 *
 */
public interface DocumentTagSchemaPlugin {

  /**
   * Add Composite Keys.
   * 
   * @param <T> {@link TagSchemaInterface}
   * @param tagSchema {@link TagSchemaInterface}
   * @param documentId {@link String}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param userId {@link String}
   * @param validateRequiredTags boolean
   * @param errors {@link Collection} {@link ValidationError}
   * @return {@link Collection} {@link DocumentTag}
   */
  <T extends TagSchemaInterface> Collection<DocumentTag> addCompositeKeys(T tagSchema,
      String documentId, Collection<DocumentTag> tags, String userId, boolean validateRequiredTags,
      Collection<ValidationError> errors);

  /**
   * Create Multi-Value {@link SearchTagCriteria}.
   * 
   * @param query {@link SearchQuery}
   * @return {@link SearchTagCriteria}
   */
  SearchTagCriteria createMultiTagSearch(SearchQuery query);

  /**
   * Get Tag Schema.
   * 
   * @param <T> {@link TagSchemaInterface}
   * @param siteId {@link String}
   * @param tagSchemaId {@link String}
   * @return {@link TagSchemaInterface}
   */
  <T extends TagSchemaInterface> T getTagSchema(String siteId, String tagSchemaId);

  /**
   * Is TagSchema Active.
   * 
   * @return boolean
   */
  boolean isActive();

  /**
   * Update In Use.
   * 
   * @param siteId {@link String}
   * @param tagSchemaId {@link String}
   */
  void updateInUse(String siteId, String tagSchemaId);

  /**
   * Delete {@link DocumentTag} Event.
   * 
   * @param tagSchema {@link TagSchemaInterface}
   * @param <T> {@link TagSchemaInterface}
   * @param tags {@link Collection} {@link String}
   * @return {@link Collection} {@link ValidationError}
   */
  <T extends TagSchemaInterface> Collection<ValidationError> validateRemoveTags(T tagSchema,
      Collection<String> tags);

  /**
   * Replace {@link DocumentTag} Event.
   * 
   * @param tagSchema {@link TagSchemaInterface}
   * @param <T> {@link TagSchemaInterface}
   * @param tags {@link Collection} {@link DocumentTag}
   * @return {@link Collection} {@link ValidationError}
   */
  <T extends TagSchemaInterface> Collection<ValidationError> validateReplaceTags(T tagSchema,
      Collection<DocumentTag> tags);
}
