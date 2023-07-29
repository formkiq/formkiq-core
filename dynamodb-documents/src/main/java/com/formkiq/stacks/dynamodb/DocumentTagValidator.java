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

import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.validation.ValidationError;

/**
 * {@link DocumentTag} Validator.
 */
public interface DocumentTagValidator {

  /**
   * Validate {@link DocumentTag}.
   * 
   * @param tags {@link Collection} {@link DocumentTag}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validate(Collection<DocumentTag> tags);

  /**
   * Validate {@link DocumentTag}.
   * 
   * @param tag {@link DocumentTag}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validate(DocumentTag tag);

  /**
   * Validate {@link DocumentTags}.
   * 
   * @param tags {@link DocumentTags}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validate(DocumentTags tags);

  /**
   * Validate {@link DocumentTag} Keys.
   * 
   * @param tagKeys {@link DocumentTags}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateKeys(Collection<String> tagKeys);
}
