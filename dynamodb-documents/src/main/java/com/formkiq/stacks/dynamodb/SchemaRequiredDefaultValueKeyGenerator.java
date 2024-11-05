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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesRequired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * {@link Function} to create Required {@link DocumentAttributeRecord} with default values from a
 * {@link Schema}.
 */
public class SchemaRequiredDefaultValueKeyGenerator {

  /** {@link Date}. */
  private final Date now = new Date();
  /** {@link AttributeService}. */
  private final AttributeService attributeservice;

  /**
   * constructor.
   * 
   * @param attributeService {@link AttributeService}
   */
  public SchemaRequiredDefaultValueKeyGenerator(final AttributeService attributeService) {
    this.attributeservice = attributeService;
  }

  /**
   * Generate a list of {@link DocumentAttributeRecord} from a {@link SchemaAttributes} and
   * CompositeKeys.
   * 
   * @param schemaAttributes {@link Collection} {@link SchemaAttributes}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributesWithValues Attributes with values.
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  public Collection<DocumentAttributeRecord> apply(
      final Collection<SchemaAttributes> schemaAttributes, final String siteId,
      final String documentId, final Collection<String> attributesWithValues) {

    String username = ApiAuthorization.getAuthorization().getUsername();

    List<SchemaAttributesRequired> missingRequiredAttributes =
        notNull(schemaAttributes).stream().flatMap(r -> notNull(r.getRequired()).stream())
            .filter(r -> !attributesWithValues.contains(r.getAttributeKey())).toList();

    Collection<String> attributeKeys = missingRequiredAttributes.stream()
        .map(SchemaAttributesRequired::getAttributeKey).collect(Collectors.toSet());

    Map<String, AttributeRecord> attributeRecordMap =
        this.attributeservice.getAttributes(siteId, attributeKeys);

    return missingRequiredAttributes.stream().filter(r -> {
      AttributeRecord rr = attributeRecordMap.get(r.getAttributeKey());
      return AttributeDataType.KEY_ONLY.equals(rr.getDataType()) || r.getDefaultValue() != null
          || !notNull(r.getDefaultValues()).isEmpty();
    }).flatMap(r -> createDefaultValues(attributeRecordMap.get(r.getAttributeKey()), r, documentId,
        username).stream()).toList();
  }

  private Collection<DocumentAttributeRecord> createDefaultValues(
      final AttributeRecord attributeRecord, final SchemaAttributesRequired r,
      final String documentId, final String username) {

    Collection<DocumentAttributeRecord> list = new ArrayList<>();

    if (AttributeDataType.KEY_ONLY.equals(attributeRecord.getDataType())) {
      list.add(createDocumentAttributeRecord(documentId, attributeRecord, null, username));
    } else {
      if (r.getDefaultValue() != null) {
        list.add(createDocumentAttributeRecord(documentId, attributeRecord, r.getDefaultValue(),
            username));
      }

      notNull(r.getDefaultValues()).forEach(
          v -> list.add(createDocumentAttributeRecord(documentId, attributeRecord, v, username)));
    }

    return list;
  }

  private DocumentAttributeRecord createDocumentAttributeRecord(final String documentId,
      final AttributeRecord attributeRecord, final String defaultValue, final String username) {

    DocumentAttributeRecord r = new DocumentAttributeRecord();
    r.setKey(attributeRecord.getKey());
    r.setDocumentId(documentId);
    r.setInsertedDate(this.now);
    r.setUserId(username);

    switch (attributeRecord.getDataType()) {
      case BOOLEAN -> {
        r.setValueType(DocumentAttributeValueType.BOOLEAN);
        r.setBooleanValue(Boolean.valueOf(defaultValue));
      }
      case KEY_ONLY -> r.setValueType(DocumentAttributeValueType.KEY_ONLY);
      case NUMBER -> {
        r.setValueType(DocumentAttributeValueType.NUMBER);
        r.setNumberValue(Double.valueOf(defaultValue));
      }
      case STRING -> {
        r.setValueType(DocumentAttributeValueType.STRING);
        r.setStringValue(defaultValue);
      }
      case PUBLICATION -> {
        r.setValueType(DocumentAttributeValueType.PUBLICATION);
        r.setStringValue(defaultValue);
      }
      case RELATIONSHIP -> {
        r.setValueType(DocumentAttributeValueType.RELATIONSHIPS);
        r.setStringValue(defaultValue);
      }
      default -> throw new IllegalArgumentException(
          "Unsupported AttributeDataType: " + attributeRecord.getDataType());
    }


    return r;
  }
}
