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

import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
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

  private boolean isEmptyRequest(final QueryRequest q) {
    return q == null || q.query() == null;
  }

  private boolean isQueryEmpty(final QueryRequest q) {
    return q.query().tag() == null && q.query().tags() == null && q.query().meta() == null;
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

    } else if (q.query().tag() != null && isEmpty(q.query().tag().key())) {

      errors.add(new ValidationErrorImpl().key("tag/key").error("attribute is required"));

    } else {

      for (SearchTagCriteria tag : Objects.notNull(q.query().tags())) {
        if (StringUtils.isEmpty(tag.key())) {
          errors.add(new ValidationErrorImpl().key("tag/key").error("attribute is required"));
        }
      }
    }

    return errors;
  }
}
