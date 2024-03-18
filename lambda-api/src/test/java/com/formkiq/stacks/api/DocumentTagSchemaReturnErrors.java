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
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.tagschema.TagSchemaInterface;
import com.formkiq.validation.ValidationError;

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
  public SearchTagCriteria createMultiTagSearch(final SearchQuery query) {
    return query.tag();
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public <T extends TagSchemaInterface> Collection<DocumentTag> addCompositeKeys(final T tagSchema,
      final String siteId, final String documentId, final Collection<DocumentTag> tags,
      final String userId, final boolean validateRequiredTags,
      final Collection<ValidationError> errors) {
    errors.add(new DummyValidationError());
    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends TagSchemaInterface> T getTagSchema(final String siteId,
      final String tagSchemaId) {
    return (T) new TagSchemaInterface() {};
  }

  @Override
  public <T extends TagSchemaInterface> void updateInUse(final String siteId, final T tagSchema) {
    // empty
  }

  @Override
  public <T extends TagSchemaInterface> Collection<ValidationError> validateRemoveTags(
      final T tagSchema, final Collection<String> tags) {
    return Arrays.asList(new DummyValidationError());
  }

  @Override
  public <T extends TagSchemaInterface> Collection<ValidationError> validateReplaceTags(
      final T tagSchema, final Collection<DocumentTag> tags) {
    return Arrays.asList(new DummyValidationError());
  }
}
