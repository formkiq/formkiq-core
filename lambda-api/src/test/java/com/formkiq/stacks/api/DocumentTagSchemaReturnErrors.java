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
package com.formkiq.stacks.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.validation.ValidationError;

/**
 * 
 * {@link DocumentTagSchemaPlugin} that returns test {@link ValidationError}.
 *
 */
public class DocumentTagSchemaReturnErrors implements DocumentTagSchemaPlugin {
  private static final class DummyValidationError implements ValidationError {
    /** Test Error Message. */
    private String error = "test error";
    /** Test Key. */
    private String key = "type";

    @Override
    public String error() {
      return this.error;
    }

    @Override
    public String key() {
      return this.key;
    }
  }

  @Override
  public Collection<DocumentTag> addCompositeKeys(final String siteId, final DocumentItem item,
      final Collection<DocumentTag> tags, final String userId, final boolean validateRequiredTags,
      final Collection<ValidationError> errors) {
    errors.add(new DummyValidationError());
    return Collections.emptyList();
  }

  @Override
  public SearchTagCriteria createMultiTagSearch(final SearchQuery query) {
    return query.tag();
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public Collection<ValidationError> validateRemoveTags(final String siteId,
      final DocumentItem item, final Collection<String> tags) {
    return Arrays.asList(new DummyValidationError());
  }

  @Override
  public Collection<ValidationError> validateReplaceTags(final String siteId,
      final DocumentItem item, final Collection<DocumentTag> tags) {
    return Arrays.asList(new DummyValidationError());
  }
}
