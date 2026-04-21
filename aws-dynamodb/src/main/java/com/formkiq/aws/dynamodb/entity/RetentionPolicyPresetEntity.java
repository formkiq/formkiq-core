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

import java.util.List;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.DISPOSITION_DATE;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.DISPOSITION_DATE_IN_DAYS;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_PERIOD_IN_DAYS;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_START_DATE_SOURCE_TYPE;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Retention Policy Preset Entity.
 */
public class RetentionPolicyPresetEntity implements PresetEntity {
  @Override
  public List<String> getAttributeKeys() {
    return List.of(RETENTION_PERIOD_IN_DAYS.getKey(), RETENTION_START_DATE_SOURCE_TYPE.getKey(),
        DISPOSITION_DATE.getKey(), DISPOSITION_DATE_IN_DAYS.getKey());
  }

  public List<DerivedDocumentAttribute> getDerivedAttributes() {
    return List.of(new RetentionEffectiveStartDateAttribute(),
        new RetentionEffectiveEndDateAttribute(), new DispositionDateAttribute(),
        new RetentionEffectiveStatusAttribute());
  }

  @Override
  public String getName() {
    return "RetentionPolicy";
  }

  private Optional<Double> getNumberAttribute(final List<EntityAttribute> attributes,
      final String attributeKey) {
    return findAttribute(attributes, attributeKey)
        .flatMap(a -> notNull(a.getNumberValues()).stream().findFirst());
  }

  public void validateAttributes(final List<EntityAttribute> attributes) {

    ValidationBuilder vb = new ValidationBuilder();

    for (String attributeKey : List.of(RETENTION_PERIOD_IN_DAYS.getKey(),
        RETENTION_START_DATE_SOURCE_TYPE.getKey())) {
      Optional<EntityAttribute> attribute = findAttribute(attributes, attributeKey);
      vb.isRequired(attributeKey, attribute);
    }
    vb.check();

    validateRetentionStartDateSourceType(vb, attributes);
    validateDispositionPeriodInDays(vb, attributes);
    vb.check();
  }

  private void validateDispositionPeriodInDays(final ValidationBuilder vb,
      final List<EntityAttribute> attributes) {
    Optional<Double> retentionPeriod =
        getNumberAttribute(attributes, RETENTION_PERIOD_IN_DAYS.getKey());
    Optional<Double> dispositionPeriod =
        getNumberAttribute(attributes, DISPOSITION_DATE_IN_DAYS.getKey());

    if (retentionPeriod.isPresent() && dispositionPeriod.isPresent()) {
      vb.isRequired(DISPOSITION_DATE_IN_DAYS.getKey(),
          dispositionPeriod.get().compareTo(retentionPeriod.get()) > 0,
          "'DispositionPeriodInDays' must be greater than 'RetentionPeriodInDays'");
    }
  }

  private void validateRetentionStartDateSourceType(final ValidationBuilder vb,
      final List<EntityAttribute> attributes) {
    Optional<EntityAttribute> o =
        findAttribute(attributes, RETENTION_START_DATE_SOURCE_TYPE.getKey());
    o.ifPresent(entityAttribute -> notNull(entityAttribute.getStringValues())
        .forEach(s -> vb.isRequired("key", RetentionStartDateSourceType.fromString(s),
            "invalid value '" + s + "'")));
  }
}
