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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.DocumentService.SYSTEM_DEFINED_TAGS;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;

/**
 * 
 * Implemenation for {@link DocumentTagValidator}.
 *
 */
public class DocumentTagValidatorImpl implements DocumentTagValidator {

  @Override
  public Collection<ValidationError> validate(final Collection<DocumentTag> tags) {
    List<String> tagKeys = tags.stream().map(t -> t.getKey()).collect(Collectors.toList());
    return validateKeys(tagKeys);
  }

  @Override
  public Collection<ValidationError> validate(final DocumentTag tag) {
    return validate(Arrays.asList(tag));
  }

  @Override
  public Collection<ValidationError> validate(final DocumentTags tags) {
    return validate(tags.getTags());
  }

  @Override
  public Collection<ValidationError> validateKeys(final Collection<String> tagKeys) {
    List<ValidationError> errors = Collections.emptyList();
    List<String> invalidTags = notNull(tagKeys).stream()
        .filter(tag -> SYSTEM_DEFINED_TAGS.contains(tag)).collect(Collectors.toList());

    if (!invalidTags.isEmpty()) {
      errors = invalidTags.stream()
          .map(tagKey -> new ValidationErrorImpl().key(tagKey).error("unallowed tag key"))
          .collect(Collectors.toList());
    }

    return errors;
  }
}
