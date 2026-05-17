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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.validation.ValidationBuilder;

/**
 * 
 * Implemenation for {@link DocumentTagValidator}.
 *
 */
public class DocumentTagValidatorImpl implements DocumentTagValidator {

  @Override
  public void validate(final ValidationBuilder vb, final Collection<DocumentTag> tags) {
    List<String> tagKeys = tags.stream().map(DocumentTag::getKey).collect(Collectors.toList());
    validateKeys(vb, tagKeys);
  }

  @Override
  public void validate(final ValidationBuilder vb, final DocumentTag tag) {
    validate(vb, List.of(tag));
  }

  @Override
  public void validate(final ValidationBuilder vb, final DocumentTags tags) {
    validate(vb, tags.getTags());
  }

  @Override
  public void validateKeys(final ValidationBuilder vb, final Collection<String> tagKeys) {

    var invalidTags = notNull(tagKeys).stream().filter(SYSTEM_DEFINED_TAGS::contains).toList();

    if (!invalidTags.isEmpty()) {
      invalidTags.forEach(tagKey -> vb.addError(tagKey, "unallowed tag key"));
    }
  }
}
