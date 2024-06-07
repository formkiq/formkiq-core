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
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link QueryRequestValidator}.
 *
 */
public class QueryRequestValidator {

  private boolean isEmptyRequest(final QueryRequest q) {
    return q == null || q.query() == null;
  }

  private boolean isQueryEmpty(final QueryRequest q) {
    return q.query().getTag() == null && notNull(q.query().getTags()).isEmpty()
        && q.query().getAttribute() == null && notNull(q.query().getAttributes()).isEmpty()
        && q.query().getMeta() == null && StringUtils.isEmpty(q.query().getText());
  }

  private void validateMultiTags(final List<SearchTagCriteria> tags,
      final Collection<ValidationError> errors) {
    if (tags.size() > 1) {
      // every tag must use "eq" except last one
      for (int i = 0; i < tags.size() - 1; i++) {
        if (tags.get(i).beginsWith() != null || tags.get(i).range() != null) {
          errors.add(new ValidationErrorImpl().key("tag/eq")
              .error("'beginsWith','range' is only supported on the last tag"));
        }
      }
    }
  }

  private void validateRange(final SearchTagCriteria tag,
      final Collection<ValidationError> errors) {

    if (tag != null && tag.range() != null) {

      if (StringUtils.isEmpty(tag.range().getStart())) {
        errors.add(new ValidationErrorImpl().key("range/start").error("range start is required"));
      }

      if (StringUtils.isEmpty(tag.range().getEnd())) {
        errors.add(new ValidationErrorImpl().key("range/end").error("range end is required"));
      }
    }
  }

  /**
   * Validate {@link QueryRequest}.
   * 
   * @param q {@link QueryRequest}
   * @return {@link Collection} {@link ValidationError}
   */
  public Collection<ValidationError> validation(final QueryRequest q) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (isEmptyRequest(q)) {

      errors.add(new ValidationErrorImpl().error("invalid body"));

    } else if (isQueryEmpty(q)) {

      errors.add(new ValidationErrorImpl().error("invalid body"));

    } else if (q.query().getTag() != null && isEmpty(q.query().getTag().key())) {

      errors.add(new ValidationErrorImpl().key("tag/key").error("attribute is required"));

    } else {

      List<SearchTagCriteria> tags = notNull(q.query().getTags());

      validateMultiTags(tags, errors);

      for (SearchTagCriteria tag : tags) {

        if (StringUtils.isEmpty(tag.key())) {
          errors.add(new ValidationErrorImpl().key("tag/key").error("attribute is required"));
        }

        validateRange(tag, errors);
      }
    }

    if (errors.isEmpty()) {
      validateRange(q.query().getTag(), errors);
    }

    return errors;
  }
}
