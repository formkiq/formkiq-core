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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationDocumentContentTypes;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * 
 * {@link DocumentValidator} implementation.
 *
 */
public class DocumentValidatorImpl implements DocumentValidator {

  /** Maximum number of Meta Data entries. */
  private static final int MAX_METADATA = 25;
  /** Maximum number of Meta Data Size. */
  private static final int MAX_METADATA_SIZE = 1000;

  @Override
  public Collection<ValidationError> validate(final Collection<DocumentMetadata> metadata) {
    final Collection<ValidationError> errors = new ArrayList<>();

    if (metadata != null) {
      if (metadata.size() > MAX_METADATA) {
        errors.add(
            new ValidationErrorImpl().key("metadata").error("maximum number is " + MAX_METADATA));
      }

      metadata.forEach(m -> {

        if (m.getValues() != null) {
          String s = m.getValues().stream().collect(Collectors.joining(","));
          if (s.length() > MAX_METADATA_SIZE) {
            errors.add(new ValidationErrorImpl().key(m.getKey())
                .error("value cannot exceed " + MAX_METADATA_SIZE));
          }
        } else if (m.getValue() != null && m.getValue().length() > MAX_METADATA_SIZE) {
          errors.add(new ValidationErrorImpl().key(m.getKey())
              .error("value cannot exceed " + MAX_METADATA_SIZE));
        }
      });
    }

    return errors;
  }

  @Override
  public void validateContentType(final SiteConfiguration config, final String contentType,
      final ValidationBuilder vb) {

    if (config != null && config.document() != null) {

      SiteConfigurationDocumentContentTypes contentTypes = config.document().contentTypes();
      List<String> allowlist = notNull(contentTypes.allowlist());
      List<String> denylist = notNull(contentTypes.denylist());

      if (!allowlist.isEmpty() && !allowlist.contains(contentType)) {
        vb.addError("contentType", "Content type '" + contentType + "' is not allowed");
      } else if (!denylist.isEmpty() && denylist.contains(contentType)) {
        vb.addError("contentType", "Content type '" + contentType + "' is not allowed");
      }
    }
  }
}
