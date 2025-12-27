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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.formkiq.aws.dynamodb.model.QueryRequest;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link QueryRequestValidator}.
 *
 */
public class QueryRequestValidator {

  private boolean isAttributesEmpty(final QueryRequest q) {
    return q.query().attribute() == null && notNull(q.query().attributes()).isEmpty();
  }

  private boolean isEmptyRequest(final QueryRequest q) {
    return q == null || q.query() == null;
  }

  private boolean isFilenameEmpty(final QueryRequest q) {
    return q.query().filename() == null || isEmpty(q.query().filename().beginsWith());
  }

  private boolean isMetaDataEmpty(final QueryRequest q) {
    return isTagsEmpty(q) && isAttributesEmpty(q) && isQueryMetaDataEmpty(q) && isFilenameEmpty(q);
  }

  private boolean isQueryEmpty(final QueryRequest q) {
    return isMetaDataEmpty(q) && isEmpty(q.query().text())
        && Objects.isEmpty(q.query().documentIds());
  }

  private boolean isQueryMetaDataEmpty(final QueryRequest q) {
    return q.query().meta() == null;
  }

  private boolean isTagsEmpty(final QueryRequest q) {
    return q.query().tag() == null && notNull(q.query().tags()).isEmpty();
  }

  private void validateMultiTags(final List<SearchTagCriteria> tags,
      final Collection<ValidationError> errors) {
    if (tags.size() > 1) {
      errors.add(new ValidationErrorImpl().key("tags").error("multiple tags search not supported"));
    }
  }

  private void validateRange(final SearchTagCriteria tag,
      final Collection<ValidationError> errors) {

    if (tag != null && tag.range() != null) {

      if (StringUtils.isEmpty(tag.range().start())) {
        errors.add(new ValidationErrorImpl().key("range/start").error("range start is required"));
      }

      if (StringUtils.isEmpty(tag.range().end())) {
        errors.add(new ValidationErrorImpl().key("range/end").error("range end is required"));
      }
    }
  }

  private void validateTag(final SearchTagCriteria tag, final Collection<ValidationError> errors) {
    if (tag != null && isEmpty(tag.key())) {
      errors.add(new ValidationErrorImpl().key("tag/key").error("tag 'key' is required"));
    }

    validateRange(tag, errors);
  }

  /**
   * Validate {@link QueryRequest}.
   * 
   * @param q {@link QueryRequest}
   * @return {@link Collection} {@link ValidationError}
   */
  public Collection<ValidationError> validation(final QueryRequest q) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (isEmptyRequest(q) || isQueryEmpty(q)) {

      errors.add(new ValidationErrorImpl().error("invalid body"));

    } else {

      boolean isTagsEmpty = isTagsEmpty(q);
      boolean isAttributesEmpty = isAttributesEmpty(q);
      boolean isMetaEmpty = isQueryMetaDataEmpty(q);
      boolean hasText = !isEmpty(q.query().text());

      if (!isMetaEmpty && (!isTagsEmpty || !isAttributesEmpty)) {
        errors.add(new ValidationErrorImpl()
            .error("'meta' cannot be combined with 'tags' or 'attributes'"));
      }

      if (hasText) {
        if (!isTagsEmpty) {
          errors
              .add(new ValidationErrorImpl().error("'text' search cannot be combined with 'tags'"));
        }

        if (!isAttributesEmpty) {
          errors.add(new ValidationErrorImpl()
              .error("'text' search cannot be combined with 'attributes'"));
        }
      }

      List<SearchTagCriteria> tags = notNull(q.query().tags());

      validateTag(q.query().tag(), errors);
      tags.forEach(tag -> validateTag(tag, errors));
      validateMultiTags(tags, errors);
    }

    return errors;
  }
}
