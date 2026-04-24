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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.aws.dynamodb.documents.DerivedDocumentAttribute;
import com.formkiq.validation.ValidationBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Preset Entity Interface.
 */
public interface PresetEntity {

  /**
   * Find PresetEntity Attribute.
   * 
   * @param attributes {@link List} {@link EntityAttribute}
   * @param attributeKey {@link String}
   * @return {@link EntityAttribute}
   */
  default Optional<EntityAttribute> findAttribute(List<EntityAttribute> attributes,
      String attributeKey) {
    return notNull(attributes).stream().filter(new EntityAttributeKeyPredicate(attributeKey))
        .findFirst();
  }

  /**
   * Find Derived Attribute.
   *
   * @param derivedAttributeKey {@link String}
   * @return {@link Optional} {@link DerivedDocumentAttribute}
   */
  default Optional<DerivedDocumentAttribute> findDerivedAttribute(String derivedAttributeKey) {
    return getDerivedAttributes().stream()
        .filter(a -> a.getAttributeKey().equals(derivedAttributeKey)).findAny();
  }

  /**
   * Get Attribute Keys.
   *
   * @return {@link List} {@link String}
   */
  List<String> getAttributeKeys();

  /**
   * Get {@link List} {@link DerivedDocumentAttribute}.
   *
   * @return {@link List} {@link DerivedDocumentAttribute}
   */
  default List<DerivedDocumentAttribute> getDerivedAttributes() {
    return Collections.emptyList();
  }

  /**
   * Get Entity Name.
   * 
   * @return {@link String}
   */
  String getName();

  /**
   * Validate Attributes.
   * 
   * @param attributes {@link List} {@link EntityAttribute}
   */
  default void validateAttributes(List<EntityAttribute> attributes) {

    ValidationBuilder vb = new ValidationBuilder();
    for (String attributeKey : getAttributeKeys()) {
      Optional<EntityAttribute> attribute = findAttribute(attributes, attributeKey);
      vb.isRequired(attributeKey, attribute);
    }
    vb.check();
  }
}
