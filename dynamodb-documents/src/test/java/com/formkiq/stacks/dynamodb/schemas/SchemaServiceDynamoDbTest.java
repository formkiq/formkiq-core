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

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URISyntaxException;
import java.util.List;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * Unit Test for {@link SchemaServiceDynamodb}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
public class SchemaServiceDynamoDbTest {

  /** {@link SchemaService}. */
  private static SchemaService service;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService db = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
    service = new SchemaServiceDynamodb(db);
  }

  /**
   * Merge SiteSchema Required attributes and Classification required.
   */
  @Test
  void testMerge01() {
    // given
    Schema from = createSchema(createRequired("type", null, null), null);
    Schema to = createSchema(createRequired("other", null, null), null);

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(0, result.getAttributes().getOptional().size());
    assertEquals(2, result.getAttributes().getRequired().size());
    assertEquals("other", result.getAttributes().getRequired().get(0).getAttributeKey());
    assertEquals("type", result.getAttributes().getRequired().get(1).getAttributeKey());
  }

  /**
   * Merge SiteSchema optional attributes and Classification optional.
   */
  @Test
  void testMerge02() {
    // given
    Schema from = createSchema(null, createOptional("type", null));
    Schema to = createSchema(null, createOptional("other", null));

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(0, result.getAttributes().getRequired().size());
    assertEquals(2, result.getAttributes().getOptional().size());
    assertEquals("other", result.getAttributes().getOptional().get(0).getAttributeKey());
    assertEquals("type", result.getAttributes().getOptional().get(1).getAttributeKey());
  }

  /**
   * Merge SiteSchema required attributes and Classification optional.
   */
  @Test
  void testMerge03() {
    // given
    Schema from = createSchema(createRequired("type", null, null), null);
    Schema to = createSchema(null, createOptional("other", null));

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(1, result.getAttributes().getRequired().size());
    assertEquals(1, result.getAttributes().getOptional().size());
    assertEquals("type", result.getAttributes().getRequired().get(0).getAttributeKey());
    assertEquals("other", result.getAttributes().getOptional().get(0).getAttributeKey());
  }

  /**
   * Merge SiteSchema/Classification required allowed values and default value.
   */
  @Test
  void testMerge04() {
    // given
    SchemaAttributesRequired r0 = createRequired("type", List.of("1", "2"), null);
    Schema from = createSchema(r0, null);

    SchemaAttributesRequired r1 = createRequired("type", List.of("3", "4"), null);
    Schema to = createSchema(r1, null);

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(1, result.getAttributes().getRequired().size());
    assertEquals(0, result.getAttributes().getOptional().size());
    assertEquals("type", result.getAttributes().getRequired().get(0).getAttributeKey());
    assertEquals("1,2,3,4",
        String.join(",", result.getAttributes().getRequired().get(0).getAllowedValues()));

    // given
    r0.defaultValue("1");
    r1.defaultValue(null);

    // when
    result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(1, result.getAttributes().getRequired().size());
    assertEquals(0, result.getAttributes().getOptional().size());
    assertEquals("1", result.getAttributes().getRequired().get(0).getDefaultValue());

    // given
    r0.defaultValue(null);
    r1.defaultValue("2");

    // when
    result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(1, result.getAttributes().getRequired().size());
    assertEquals(0, result.getAttributes().getOptional().size());
    assertEquals("2", result.getAttributes().getRequired().get(0).getDefaultValue());

    // given
    r0.defaultValue("3");
    r1.defaultValue("2");

    // when
    result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(1, result.getAttributes().getRequired().size());
    assertEquals(0, result.getAttributes().getOptional().size());
    assertEquals("2", result.getAttributes().getRequired().get(0).getDefaultValue());
  }

  /**
   * Merge SiteSchema/Classification optional allowed values.
   */
  @Test
  void testMerge05() {
    // given
    SchemaAttributesOptional o0 = createOptional("type", List.of("1", "2"));
    Schema from = createSchema(null, o0);

    SchemaAttributesOptional o1 = createOptional("type", List.of("3", "4"));
    Schema to = createSchema(null, o1);

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(0, result.getAttributes().getRequired().size());
    assertEquals(1, result.getAttributes().getOptional().size());
    assertEquals("type", result.getAttributes().getOptional().get(0).getAttributeKey());
    assertEquals("1,2,3,4",
        String.join(",", result.getAttributes().getOptional().get(0).getAllowedValues()));
  }

  /**
   * Merge required / optional keys with same name.
   */
  @Test
  void testMerge06() {
    // given
    Schema from = createSchema(createRequired("type", List.of("1", "2"), null), null);
    Schema to = createSchema(null, createOptional("type", null));

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(1, result.getAttributes().getRequired().size());
    assertEquals(0, result.getAttributes().getOptional().size());
    assertEquals("type", result.getAttributes().getRequired().get(0).getAttributeKey());
    assertEquals("1,2",
        String.join(",", result.getAttributes().getRequired().get(0).getAllowedValues()));
  }

  /**
   * Merge composite keys.
   */
  @Test
  void testMerge07() {
    // given
    Schema from = createSchemaCompositeKeys(List.of("type", "category"));
    Schema to = createSchemaCompositeKeys(List.of("id", "category"));

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(2, result.getAttributes().getCompositeKeys().size());
    assertEquals("type,category,id,category", String.join(",", result.getAttributes()
        .getCompositeKeys().stream().map(c -> String.join(",", c.getAttributeKeys())).toList()));
  }

  /**
   * Merge duplicate composite keys.
   */
  @Test
  void testMerge08() {
    // given
    Schema from = createSchemaCompositeKeys(List.of("type", "category"));
    Schema to = createSchemaCompositeKeys(List.of("type", "category"));

    // when
    Schema result = service.mergeSchemaIntoClassification(from, to);

    // then
    assertEquals(1, result.getAttributes().getCompositeKeys().size());
    assertEquals("type,category", String.join(",", result.getAttributes().getCompositeKeys()
        .stream().map(c -> String.join(",", c.getAttributeKeys())).toList()));
  }

  private Schema createSchemaCompositeKeys(final List<String> compositeKeys) {
    List<SchemaAttributesCompositeKey> keys =
        List.of(new SchemaAttributesCompositeKey().attributeKeys(compositeKeys));
    return new Schema().attributes(new SchemaAttributes().compositeKeys(keys));
  }

  private Schema createSchema(final SchemaAttributesRequired required,
      final SchemaAttributesOptional optional) {
    SchemaAttributes sa = new SchemaAttributes();

    if (required != null) {
      sa.required(List.of(required));
    }

    if (optional != null) {
      sa.optional(List.of(optional));
    }

    return new Schema().attributes(sa);
  }

  private SchemaAttributesRequired createRequired(final String attributeKey,
      final List<String> allowedValues, final String defaultValue) {
    return new SchemaAttributesRequired().attributeKey(attributeKey).allowedValues(allowedValues)
        .defaultValue(defaultValue);
  }

  private SchemaAttributesOptional createOptional(final String attributeKey,
      final List<String> allowedValues) {
    return new SchemaAttributesOptional().attributeKey(attributeKey).allowedValues(allowedValues);
  }
}
