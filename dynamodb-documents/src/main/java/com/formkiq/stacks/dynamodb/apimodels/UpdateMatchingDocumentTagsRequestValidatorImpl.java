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
package com.formkiq.stacks.dynamodb.apimodels;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import java.util.ArrayList;
import java.util.Collection;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;

/**
 * Implementation of {@link UpdateMatchingDocumentTagsRequestValidator}.
 */
public class UpdateMatchingDocumentTagsRequestValidatorImpl
    implements UpdateMatchingDocumentTagsRequestValidator {

  @Override
  public Collection<ValidationError> validate(final UpdateMatchingDocumentTagsRequest request) {
    Collection<ValidationError> errors = new ArrayList<>();

    if (request.getMatch() == null) {
      errors.add(new ValidationErrorImpl().key("match").error("'match' required"));
    }

    if (request.getUpdate() == null) {
      errors.add(new ValidationErrorImpl().key("update").error("'update' required"));
    }

    if (errors.isEmpty()) {
      validateMatch(request.getMatch(), errors);
      validateUpdate(request.getUpdate(), errors);
    }

    return errors;
  }

  /**
   * Validate {@link UpdateMatchingDocumentTagsRequestUpdate}.
   * 
   * @param update {@link UpdateMatchingDocumentTagsRequestUpdate}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateUpdate(final UpdateMatchingDocumentTagsRequestUpdate update,
      final Collection<ValidationError> errors) {

    if (notNull(update.getTags()).isEmpty()) {
      errors.add(new ValidationErrorImpl().key("update").error("'update' required"));
    } else {
      update.getTags().forEach(t -> {
        if (Strings.isEmpty(t.getKey())) {
          errors.add(
              new ValidationErrorImpl().key("update.keys.key").error("'update.keys.key' required"));
        }
      });
    }
  }

  /**
   * Validate {@link updateMatchingDocumentTagsRequestMatch}.
   * 
   * @param match {@link UpdateMatchingDocumentTagsRequestMatch}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateMatch(final UpdateMatchingDocumentTagsRequestMatch match,
      final Collection<ValidationError> errors) {
    MatchDocumentTag matchTag = match.getTag();
    if (matchTag == null) {
      errors.add(new ValidationErrorImpl().key("match.tag").error("'match.tag' required"));
    } else {

      if (matchTag.getKey() == null) {
        errors
            .add(new ValidationErrorImpl().key("match.tag.key").error("'match.tag.key' required"));
      }
    }
  }

}
