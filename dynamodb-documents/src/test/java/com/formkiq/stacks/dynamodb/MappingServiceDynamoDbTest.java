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

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.mappings.Mapping;
import com.formkiq.stacks.dynamodb.mappings.MappingAttribute;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeLabelMatchingType;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeSourceType;
import com.formkiq.validation.ValidationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.stacks.dynamodb.mappings.MappingService;
import com.formkiq.stacks.dynamodb.mappings.MappingServiceDynamodb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationError;

/**
 * 
 * Unit Test for {@link MappingServiceDynamodb}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
class MappingServiceDynamoDbTest implements DbKeys {

  /** {@link AttributeService}. */
  private static AttributeService attributeService;
  /** {@link MappingService}. */
  private static MappingService service;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService db = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
    service = new MappingServiceDynamodb(db);
    attributeService = new AttributeServiceDynamodb(db);
  }

  /**
   * Delete mapping not exists.
   */
  @Test
  void testDelete01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = ID.uuid();

      // when
      boolean deleted = service.deleteMapping(siteId, documentId);

      // then
      assertFalse(deleted);
    }
  }

  /**
   * Delete mapping.
   */
  @Test
  void testDelete02() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = ID.uuid();

      Mapping mapping = createMapping();
      attributeService.addAttribute(AttributeValidationAccess.DELETE, siteId, "number", null, null);
      service.saveMapping(siteId, documentId, mapping);
      assertNotNull(service.getMapping(siteId, documentId));

      // when
      boolean deleted = service.deleteMapping(siteId, documentId);

      // then
      assertTrue(deleted);
      assertNull(service.getMapping(siteId, documentId));
    }
  }

  private Mapping createMapping() {
    Mapping mapping = new Mapping().setName("test");
    MappingAttribute a = new MappingAttribute().setAttributeKey("number")
        .setLabelMatchingType(MappingAttributeLabelMatchingType.EXACT).setLabelTexts(List.of("PO"))
        .setSourceType(MappingAttributeSourceType.CONTENT);
    mapping.setAttributes(Collections.singletonList(a));
    return mapping;
  }

  /**
   * Saving mapping with errors.
   */
  @Test
  void testSave01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      Mapping mapping = new Mapping();

      // when
      try {
        service.saveMapping(siteId, null, mapping);
        fail();
      } catch (ValidationException e) {
        // then
        final int expected = 2;
        assertEquals(expected, e.errors().size());
        Iterator<ValidationError> itr = e.errors().iterator();

        ValidationError error = itr.next();
        assertEquals("name", error.key());
        assertEquals("'name' is required", error.error());

        error = itr.next();
        assertEquals("attributes", error.key());
        assertEquals("'attributes' is required", error.error());
      }
    }
  }

  /**
   * Saving mapping.
   */
  @Test
  void testSave02() throws ValidationException {
    // given
    final int count = 5;

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "number", null, null);

      // when
      for (int i = 0; i < count; i++) {
        Mapping mapping = createMapping().setName("test_" + i);
        service.saveMapping(siteId, null, mapping);
      }

      // then
      PaginationResults<MappingRecord> mappings = service.findMappings(siteId, null, 2);
      assertEquals(2, mappings.getResults().size());

      assertEquals("test_0", mappings.getResults().get(0).getName());
      assertEquals("test_1", mappings.getResults().get(1).getName());

      mappings = service.findMappings(siteId, mappings.getToken(), 2);
      assertEquals(2, mappings.getResults().size());

      assertEquals("test_2", mappings.getResults().get(0).getName());
      assertEquals("test_3", mappings.getResults().get(1).getName());
    }
  }

  /**
   * Saving mapping with attributes errors.
   */
  @Test
  void testSave03() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      Mapping mapping =
          new Mapping().setName("test").setAttributes(List.of(new MappingAttribute()));

      // when
      try {
        service.saveMapping(siteId, null, mapping);
        fail();
      } catch (ValidationException e) {
        // then
        final int expected = 4;
        assertEquals(expected, e.errors().size());
        Iterator<ValidationError> itr = e.errors().iterator();

        ValidationError error = itr.next();
        assertEquals("attribute[0].attributeKey", error.key());
        assertEquals("'attributeKey' is required", error.error());

        error = itr.next();
        assertEquals("attribute[0].sourceType", error.key());
        assertEquals("'sourceType' is required", error.error());

        error = itr.next();
        assertEquals("attribute[0].labelMatchingType", error.key());
        assertEquals("'labelMatchingType' is required", error.error());

        error = itr.next();
        assertEquals("attribute[0].labelTexts", error.key());
        assertEquals("'labelTexts' is required", error.error());
      }
    }
  }

  /**
   * Saving mapping duplicate attributes.
   */
  @Test
  void testSave04() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "number", null, null);

      // when
      Mapping mapping = createMapping();
      MappingAttribute attribute = mapping.getAttributes().get(0);
      mapping.setAttributes(Arrays.asList(attribute, attribute));

      try {
        service.saveMapping(siteId, null, mapping);
        fail();
      } catch (ValidationException e) {
        // then
        final int expected = 1;
        assertEquals(expected, e.errors().size());
        Iterator<ValidationError> itr = e.errors().iterator();

        ValidationError error = itr.next();
        assertNull(error.key());
        assertEquals("duplicate attributes in mapping", error.error());
      }
    }
  }

  /**
   * Saving mapping with sourceType METADATA with attributes errors.
   */
  @Test
  void testSave05() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice", null,
          null);

      Mapping mapping = new Mapping().setName("test")
          .setAttributes(List.of(new MappingAttribute()
              .setSourceType(MappingAttributeSourceType.METADATA).setAttributeKey("invoice")
              .setLabelMatchingType(MappingAttributeLabelMatchingType.EXACT)
              .setLabelTexts(List.of("test"))));

      // when
      try {
        service.saveMapping(siteId, null, mapping);
        fail();
      } catch (ValidationException e) {
        // then
        final int expected = 1;
        assertEquals(expected, e.errors().size());
        Iterator<ValidationError> itr = e.errors().iterator();

        ValidationError error = itr.next();
        assertEquals("attribute[0].metadataField", error.key());
        assertEquals("'metadataField' is required", error.error());
      }
    }
  }

  /**
   * Saving mapping with sourceType MANUAL with errors.
   */
  @Test
  void testSave06() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      Mapping mapping = new Mapping().setName("test").setAttributes(
          List.of(new MappingAttribute().setSourceType(MappingAttributeSourceType.MANUAL)));

      // when
      try {
        service.saveMapping(siteId, null, mapping);
        fail();
      } catch (ValidationException e) {
        // then
        final int expected = 2;
        assertEquals(expected, e.errors().size());
        Iterator<ValidationError> itr = e.errors().iterator();

        ValidationError error = itr.next();
        assertEquals("attribute[0].attributeKey", error.key());
        assertEquals("'attributeKey' is required", error.error());

        error = itr.next();
        assertEquals("attribute[0].defaultValue", error.key());
        assertEquals("'defaultValue' or 'defaultValues' is required", error.error());
      }
    }
  }

  /**
   * Saving mapping with sourceType MANUAL.
   */
  @Test
  void testSave07() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice", null,
          null);

      Mapping mapping = new Mapping().setName("test").setAttributes(
          List.of(new MappingAttribute().setSourceType(MappingAttributeSourceType.MANUAL)
              .setAttributeKey("invoice").setDefaultValue("123")));

      // when
      MappingRecord mappingRecord = service.saveMapping(siteId, null, mapping);

      // then
      assertNotNull(mappingRecord);
    }
  }
}
