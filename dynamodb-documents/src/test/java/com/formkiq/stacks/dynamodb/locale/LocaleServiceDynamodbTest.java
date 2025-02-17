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
package com.formkiq.stacks.dynamodb.locale;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.stacks.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.stacks.dynamodb.schemas.SchemaServiceDynamodb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit Tests for {@link LocaleServiceDynamodb}.
 */
@ExtendWith(DynamoDbExtension.class)
public class LocaleServiceDynamodbTest {
  /** Results Limit. */
  private static final int LIMIT = 100;
  /** {@link LocaleService}. */
  private static LocaleService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {
    DynamoDbConnectionBuilder dbc = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService db = new DynamoDbServiceImpl(dbc, "Documents");
    SchemaService schemaService = new SchemaServiceDynamodb(db);
    service = new LocaleServiceDynamodb(db, schemaService);
  }

  /**
   * Save null.
   */
  @Test
  void testSave01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      List<ValidationError> errors = service.save(siteId, null);

      // then
      assertEquals(1, errors.size());
    }
  }

  /**
   * Save missing type.
   */
  @Test
  void testSave02() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      LocaleTypeRecord r = new LocaleTypeRecord();
      // when
      List<ValidationError> errors = service.save(siteId, List.of(r));

      // then
      assertEquals(1, errors.size());
      assertErrorEquals(errors.get(0), "itemType", "'itemType' is required");
    }
  }

  /**
   * Save INTERFACE type.
   */
  @Test
  void testSave03() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      LocaleTypeRecord r = new LocaleTypeRecord().setItemType(LocaleResourceType.INTERFACE);
      // when
      List<ValidationError> errors = service.save(siteId, List.of(r));

      // then
      final int expected = 3;
      assertEquals(expected, errors.size());
      assertErrorEquals(errors.get(0), "locale", "'locale' is required");
      assertErrorEquals(errors.get(1), "localizedValue", "'localizedValue' is required");
      assertErrorEquals(errors.get(2), "interfaceKey", "'interfaceKey' is required");
    }
  }

  /**
   * Save SCHEMA type.
   */
  @Test
  void testSave04() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      LocaleTypeRecord r = new LocaleTypeRecord().setItemType(LocaleResourceType.SCHEMA);
      // when
      List<ValidationError> errors = service.save(siteId, List.of(r));

      // then
      final int expected = 4;
      int i = 0;
      assertEquals(expected, errors.size());
      assertErrorEquals(errors.get(i++), "locale", "'locale' is required");
      assertErrorEquals(errors.get(i++), "localizedValue", "'localizedValue' is required");
      assertErrorEquals(errors.get(i++), "attributeKey", "'attributeKey' is required");
      assertErrorEquals(errors.get(i), "allowedValue", "'allowedValue' is required");
    }
  }

  /**
   * Save CLASSIFICATION type.
   */
  @Test
  void testSave05() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      LocaleTypeRecord r = new LocaleTypeRecord().setItemType(LocaleResourceType.CLASSIFICATION);
      // when
      List<ValidationError> errors = service.save(siteId, List.of(r));

      // then
      final int expected = 5;
      assertEquals(expected, errors.size());
      int i = 0;
      assertErrorEquals(errors.get(i++), "locale", "'locale' is required");
      assertErrorEquals(errors.get(i++), "localizedValue", "'localizedValue' is required");
      assertErrorEquals(errors.get(i++), "classificationId", "'classificationId' is required");
      assertErrorEquals(errors.get(i++), "attributeKey", "'attributeKey' is required");
      assertErrorEquals(errors.get(i), "allowedValue", "'allowedValue' is required");
    }
  }

  private void assertErrorEquals(final ValidationError e, final String key, final String error) {
    assertEquals(key, e.key());
    assertEquals(error, e.error());
  }

  /**
   * Find all.
   */
  @Test
  void testFindAll01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String locale = "en";
      service.saveLocale(siteId, locale);

      LocaleTypeRecord r0 = new LocaleTypeRecord().setItemType(LocaleResourceType.INTERFACE)
          .setLocale(locale).setInterfaceKey("test1").setLocalizedValue("111");
      LocaleTypeRecord r1 = new LocaleTypeRecord().setItemType(LocaleResourceType.INTERFACE)
          .setLocale(locale).setInterfaceKey("test2").setLocalizedValue("111");
      List<ValidationError> errors = service.save(siteId, List.of(r0, r1));
      assertEquals(0, errors.size());

      // when
      final Pagination<LocaleTypeRecord> fr0 =
          service.findAll(siteId, "fr", LocaleResourceType.INTERFACE, null, 1);
      final Pagination<LocaleTypeRecord> en0 =
          service.findAll(siteId, locale, LocaleResourceType.INTERFACE, null, 1);
      final Pagination<LocaleTypeRecord> en1 =
          service.findAll(siteId, locale, LocaleResourceType.INTERFACE, en0.getNextToken(), LIMIT);
      final Pagination<LocaleTypeRecord> s0 =
          service.findAll(siteId, locale, LocaleResourceType.SCHEMA, null, LIMIT);
      final Pagination<LocaleTypeRecord> c0 =
          service.findAll(siteId, locale, LocaleResourceType.CLASSIFICATION, null, LIMIT);

      // then
      assertEquals(0, fr0.getResults().size());
      assertEquals(1, en0.getResults().size());
      assertEquals(1, en1.getResults().size());
      assertEquals(0, s0.getResults().size());
      assertEquals(0, c0.getResults().size());
    }
  }
}
