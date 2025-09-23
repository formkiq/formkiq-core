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
package com.formkiq.stacks.dynamodb.schemas;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;

/**
 * Generates a {@link Collection} of missing required {@link DocumentAttributeRecord}.
 */
public class SchemaMissingRequiredAttributes
    implements Function<Collection<DocumentAttributeRecord>, Collection<DocumentAttributeRecord>> {

  /** Document Id. */
  private final String docId;
  /** {@link SchemaAttributes}. */
  private final SchemaAttributes attributes;
  /** {@link AttributeService}. */
  private final AttributeService service;
  /** SiteId. */
  private final String site;

  /**
   * constructor.
   * 
   * @param attributeService {@link AttributeService}
   * @param schemaAttributes {@link SchemaAttributes}
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  public SchemaMissingRequiredAttributes(final AttributeService attributeService,
      final SchemaAttributes schemaAttributes, final String siteId, final String documentId) {
    this.docId = documentId;
    this.attributes = schemaAttributes;
    this.service = attributeService;
    this.site = siteId;
  }

  @Override
  public Collection<DocumentAttributeRecord> apply(final Collection<DocumentAttributeRecord> c) {

    List<DocumentAttributeRecord> missingAttributes = Collections.emptyList();

    if (attributes != null) {
      Set<String> keys =
          c.stream().map(DocumentAttributeRecord::getKey).collect(Collectors.toSet());

      List<SchemaAttributesRequired> requiredAttributes = notNull(this.attributes.getRequired())
          .stream().filter(a -> !keys.contains(a.getAttributeKey())).toList();

      if (!requiredAttributes.isEmpty()) {

        List<String> attributeKeys =
            requiredAttributes.stream().map(SchemaAttributesRequired::getAttributeKey).toList();

        Map<String, AttributeRecord> attributeMap =
            this.service.getAttributes(this.site, attributeKeys);

        // filter out anything that doesn't have a default value or KEY_ONLY datatype
        missingAttributes =
            requiredAttributes.stream()
                .filter(
                    a -> !isEmpty(a.getDefaultValue()) || !notNull(a.getDefaultValues()).isEmpty()
                        || AttributeDataType.KEY_ONLY
                            .equals(attributeMap.get(a.getAttributeKey()).getDataType()))
                .flatMap(a -> {

                  String attributeKey = a.getAttributeKey();
                  AttributeDataType dataType = attributeMap.get(attributeKey).getDataType();
                  return createRequiredAttributes(a, this.docId, attributeKey, dataType).stream();
                }).toList();
      }
    }

    return missingAttributes;
  }

  private Double convertToDouble(final String value) {
    try {
      return Double.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Collection<DocumentAttributeRecord> createRequiredAttributes(
      final SchemaAttributesRequired attribute, final String documentId, final String attributeKey,
      final AttributeDataType dataType) {

    Collection<DocumentAttributeRecord> records = new ArrayList<>();

    List<String> values =
        !notNull(attribute.getDefaultValues()).isEmpty() ? attribute.getDefaultValues()
            : Collections.singletonList(attribute.getDefaultValue());

    for (String value : values) {

      String stringValue = AttributeDataType.STRING.equals(dataType) ? value : null;
      Boolean booleanValue =
          AttributeDataType.BOOLEAN.equals(dataType) ? Boolean.valueOf(value) : null;
      Double numberValue =
          AttributeDataType.NUMBER.equals(dataType) ? convertToDouble(value) : null;

      DocumentAttributeRecord a = new DocumentAttributeRecord().setKey(attributeKey)
          .setDocumentId(documentId).setStringValue(stringValue).setBooleanValue(booleanValue)
          .setNumberValue(numberValue);
      a.updateValueType();

      records.add(a);
    }

    return records;
  }
}
