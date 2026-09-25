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
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.aws.dynamodb.attributes.AttributeType;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Tests for {@link SchemaServiceDynamodb}.
 */
@ExtendWith(DynamoDbExtension.class)
public class SchemaServiceDynamodbTest {
  /** Results Limit. */
  private static final int LIMIT = 100;
  /** {@link SchemaService}. */
  private static SchemaService service;
  /** {@link AttributeService}. */
  private static AttributeService attributeService;
  /** {@link DynamoDbService}. */
  private static DynamoDbService db;

  private static void addAttribute(final String siteId, final String attributeKey) {
    attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, attributeKey,
        AttributeDataType.STRING, AttributeType.STANDARD);
  }

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {
    DynamoDbConnectionBuilder dbc = DynamoDbTestServices.getDynamoDbConnection();
    db = new DynamoDbServiceImpl(dbc, "Documents");
    service = new SchemaServiceDynamodb(db);
    attributeService = new AttributeServiceDynamodb(db);
  }

  private static SchemaAttributesOptional createCategoryOptional(final List<String> allowedValues) {
    return new SchemaAttributesOptional().attributeKey("category").allowedValues(allowedValues);
  }

  private static SchemaAttributesRequired createCategoryRequired(final List<String> allowedValues) {
    return new SchemaAttributesRequired().attributeKey("category").allowedValues(allowedValues);
  }

  private static SchemaAttributesRequired createDocTypeRequired() {
    return new SchemaAttributesRequired().attributeKey("docType")
        .allowedValues(List.of("invoice", "receipt"));
  }

  private static List<SearchAttributeCriteria> equalityCriteria(final List<String> keys) {
    return keys.stream().map(key -> new SearchAttributeCriteria(key, null, "value", null, null))
        .toList();
  }

  private static ClassificationRecord setClassification(final String siteId,
      final String classificationId, final String name, final SchemaAttributes schemaAttributes)
      throws ValidationException {
    Schema schema = new Schema().name(name).attributes(schemaAttributes);
    return service.setClassification(siteId, classificationId, name, schema, "joe");
  }

  private static Collection<ValidationError> setSitesSchema(final String siteId,
      final SchemaAttributes schemaAttributes) {
    String name = "somesetschema";
    Schema schema = new Schema().name(name).attributes(schemaAttributes);
    return service.setSitesSchema(siteId, name, schema);
  }

  /**
   * Get Allowed values across site schema and multiple classifications.
   */
  @Test
  void testGetAttributeAllowedValues01() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      addAttribute(siteId, "category");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      setSitesSchema(siteId, new SchemaAttributes().required(List.of(require0)));

      SchemaAttributesRequired require1 = createCategoryRequired(List.of("A", "Z"));
      setClassification(siteId, null, "doc1", new SchemaAttributes().required(List.of(require1)));

      SchemaAttributesRequired require2 = createCategoryRequired(List.of("AA", "BB", "CC", "Z"));
      setClassification(siteId, null, "doc2", new SchemaAttributes().required(List.of(require2)));

      // when
      List<String> allowedValues = service.getAttributeAllowedValues(siteId, "category");

      // then
      final int expected = 6;
      assertEquals(expected, allowedValues.size());
      assertEquals("A,AA,BB,CC,Y,Z", String.join(",", allowedValues));
    }
  }

  /** Choose site or classification composite keys without including another site's definitions. */
  @Test
  public void testGetCompositeKeyBestMatch() {
    // given
    String siteId = ID.uuid();
    String otherSiteId = ID.uuid();
    List<String> siteKeys = List.of("category", "docType");
    List<String> classificationKeys = List.of("region", "category");
    List<SchemaAttributesOptional> optional = List.of("category", "docType", "region").stream()
        .map(key -> new SchemaAttributesOptional().attributeKey(key)).toList();
    for (String site : List.of(siteId, otherSiteId)) {
      for (String key : List.of("category", "docType", "region")) {
        addAttribute(site, key);
      }
    }
    assertTrue(setSitesSchema(siteId,
        new SchemaAttributes().optional(optional)
            .compositeKeys(List.of(new SchemaAttributesCompositeKey().attributeKeys(siteKeys))))
        .isEmpty());
    setClassification(siteId, null, "regional",
        new SchemaAttributes().optional(optional).compositeKeys(
            List.of(new SchemaAttributesCompositeKey().attributeKeys(classificationKeys))));
    List<String> otherSiteKeys = List.of("docType", "category");
    assertTrue(
        setSitesSchema(otherSiteId,
            new SchemaAttributes().optional(optional).compositeKeys(
                List.of(new SchemaAttributesCompositeKey().attributeKeys(otherSiteKeys))))
            .isEmpty());

    // when
    SchemaCompositeKeyRecord record = service.getCompositeKeyBestMatch(siteId,
        equalityCriteria(List.of("region", "docType", "category")));
    SchemaCompositeKeyRecord classification =
        service.getCompositeKeyBestMatch(siteId, equalityCriteria(classificationKeys));

    // then
    assertNotNull(record);
    assertEquals(siteKeys, record.getKeys());
    assertNotNull(classification);
    assertEquals(classificationKeys, classification.getKeys());

    // when
    SchemaCompositeKeyRecord other =
        service.getCompositeKeyBestMatch(otherSiteId, equalityCriteria(siteKeys));

    // then
    assertNotNull(other);
    assertEquals(otherSiteKeys, other.getKeys());

    // when
    SchemaCompositeKeyRecord missing =
        service.getCompositeKeyBestMatch(ID.uuid(), equalityCriteria(siteKeys));
    SchemaCompositeKeyRecord noMatch =
        service.getCompositeKeyBestMatch(otherSiteId, equalityCriteria(classificationKeys));

    // then
    assertNull(missing);
    assertNull(noMatch);
  }

  /** Skip composites with unsupported leading operators and return null when none are usable. */
  @Test
  public void testGetCompositeKeyBestMatchOperators() {
    // given
    String siteId = ID.uuid();
    List<String> pair = List.of("customer", "status");
    List<String> triple = List.of("customer", "status", "region");
    db.putItems(List.of(new SchemaCompositeKeyRecord().keys(pair).getAttributes(siteId),
        new SchemaCompositeKeyRecord().keys(triple).getAttributes(siteId)));
    SearchAttributeCriteria customer =
        new SearchAttributeCriteria("customer", null, "123", null, null);
    SearchAttributeCriteria status =
        new SearchAttributeCriteria("status", "approv", null, null, null);
    SearchAttributeCriteria region = new SearchAttributeCriteria("region", null, "us", null, null);

    // when
    SchemaCompositeKeyRecord record =
        service.getCompositeKeyBestMatch(siteId, List.of(region, status, customer));

    // then
    assertNotNull(record);
    assertEquals(pair, record.getKeys());

    // when
    SchemaCompositeKeyRecord noMatch = service.getCompositeKeyBestMatch(siteId,
        List.of(region, status, new SearchAttributeCriteria("customer", "12", null, null, null)));
    SchemaCompositeKeyRecord existenceOnly = service.getCompositeKeyBestMatch(siteId,
        List.of(region, customer, new SearchAttributeCriteria("status", null, null, null, null)));

    // then
    assertNull(noMatch);
    assertNull(existenceOnly);
  }

  /** Find the largest matching composite even when it is on a later index page. */
  @Test
  public void testGetCompositeKeyBestMatchPagination() {
    // given
    String siteId = ID.uuid();
    List<List<String>> smaller =
        IntStream.range(0, LIMIT).mapToObj(i -> List.of("customer", "attribute" + i)).toList();
    db.putItems(smaller.stream()
        .map(keys -> new SchemaCompositeKeyRecord().keys(keys).getAttributes(siteId)).toList());
    List<String> expected = List.of("zzCustomer", "zzStatus", "zzRegion");
    db.putItems(List.of(new SchemaCompositeKeyRecord().keys(expected).getAttributes(siteId)));
    List<String> keys = Stream.concat(smaller.stream().flatMap(List::stream), expected.stream())
        .distinct().toList();

    // when
    SchemaCompositeKeyRecord record =
        service.getCompositeKeyBestMatch(siteId, equalityCriteria(keys));

    // then
    assertNotNull(record);
    assertEquals(expected, record.getKeys());
  }

  /** Prefer a three-attribute composite over a matching two-attribute composite. */
  @Test
  public void testGetCompositeKeyBestMatchPrefersMoreAttributes() {
    // given
    String siteId = ID.uuid();
    List<String> pair = List.of("customer", "status");
    List<String> triple = List.of("customer", "status", "region");
    db.putItems(List.of(new SchemaCompositeKeyRecord().keys(pair).getAttributes(siteId),
        new SchemaCompositeKeyRecord().keys(triple).getAttributes(siteId)));

    // when
    SchemaCompositeKeyRecord record = service.getCompositeKeyBestMatch(siteId,
        equalityCriteria(List.of("region", "status", "customer")));

    // then
    assertNotNull(record);
    assertEquals(triple, record.getKeys());
  }

  /**
   * Set Classification.
   */
  @Test
  public void testSetClassification01() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      addAttribute(siteId, "category");
      addAttribute(siteId, "docType");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      SchemaAttributesRequired require1 = createDocTypeRequired();

      SchemaAttributesCompositeKey compositeKey =
          new SchemaAttributesCompositeKey().attributeKeys(List.of("category", "docType"));
      SchemaAttributes schemaAttributes = new SchemaAttributes()
          .required(List.of(require0, require1)).compositeKeys(List.of(compositeKey));

      // when
      ClassificationRecord classification =
          setClassification(siteId, null, "doc", schemaAttributes);

      // then
      final String classificationId = classification.getDocumentId();
      Schema sitesSchema = service.getSchema(classification);
      assertNotNull(sitesSchema);

      SchemaCompositeKeyRecord compositeKeyRecord =
          service.getCompositeKeyExactMatch(siteId, List.of("docType", "category"));
      assertNotNull(compositeKeyRecord);
      assertNull(service.getCompositeKeyExactMatch(siteId, List.of("docType", "category123")));

      List<String> allowedValues =
          service.getClassificationAttributeAllowedValues(siteId, classificationId, "category");
      assertEquals(2, allowedValues.size());
      assertEquals("Y,Z", String.join(",", allowedValues));

      // given
      require0.allowedValues(List.of("1", "2", "3"));

      // when
      setClassification(siteId, classificationId, "doc", schemaAttributes);

      // then
      allowedValues =
          service.getClassificationAttributeAllowedValues(siteId, classificationId, "category");

      final int expected = 3;
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));

      allowedValues = service.getAttributeAllowedValues(siteId, "category");
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));
    }
  }

  /**
   * Get Allowed values across site schema and classification.
   */
  @Test
  void testSetClassification02() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      addAttribute(siteId, "category");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      setSitesSchema(siteId, new SchemaAttributes().required(List.of(require0)));

      SchemaAttributesRequired require1 = createCategoryRequired(List.of("A", "Z"));
      ClassificationRecord classification = setClassification(siteId, null, "doc",
          new SchemaAttributes().required(List.of(require1)));

      // when
      List<String> allowedValues = service.getClassificationAttributeAllowedValues(siteId,
          classification.getDocumentId(), "category");

      // then
      final int expected = 3;
      assertEquals(expected, allowedValues.size());
      assertEquals("A,Y,Z", String.join(",", allowedValues));
    }
  }

  /**
   * Set Sites Schema.
   */
  @Test
  public void testSetSitesSchema01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      addAttribute(siteId, "category");
      addAttribute(siteId, "docType");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("A", "B"));
      SchemaAttributesRequired require1 = createDocTypeRequired();

      SchemaAttributesCompositeKey compositeKey =
          new SchemaAttributesCompositeKey().attributeKeys(List.of("category", "docType"));
      SchemaAttributes schemaAttributes = new SchemaAttributes()
          .required(List.of(require0, require1)).compositeKeys(List.of(compositeKey));

      // when
      Collection<ValidationError> errors = setSitesSchema(siteId, schemaAttributes);

      // then
      assertEquals(0, errors.size());
      Schema sitesSchema = service.getSitesSchema(siteId);
      assertNotNull(sitesSchema);

      SchemaCompositeKeyRecord compositeKeyRecord =
          service.getCompositeKeyExactMatch(siteId, List.of("docType", "category"));
      assertNotNull(compositeKeyRecord);
      assertNull(service.getCompositeKeyExactMatch(siteId, List.of("docType", "category123")));

      List<String> allowedValues = service.getSitesSchemaAttributeAllowedValues(siteId, "category");
      assertEquals(2, allowedValues.size());
      assertEquals("A,B", String.join(",", allowedValues));

      // given
      require0.allowedValues(List.of("1", "2", "3"));

      // when
      errors = setSitesSchema(siteId, schemaAttributes);

      // then
      assertEquals(0, errors.size());
      allowedValues = service.getSitesSchemaAttributeAllowedValues(siteId, "category");

      final int expected = 3;
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));

      allowedValues = service.getAttributeAllowedValues(siteId, "category");
      assertEquals(expected, allowedValues.size());
      assertEquals("1,2,3", String.join(",", allowedValues));
    }
  }

  /**
   * Duplicate Attribute Key across site schema.
   */
  @Test
  void testSetSitesSchema02() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      addAttribute(siteId, "category");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      SchemaAttributesOptional optional0 = createCategoryOptional(List.of("Z", "Y"));

      // when
      Collection<ValidationError> errors = setSitesSchema(siteId,
          new SchemaAttributes().required(List.of(require0)).optional(List.of(optional0)));

      // then
      assertEquals(1, errors.size());
      assertEquals("attribute 'category' is in both required & optional lists",
          errors.iterator().next().error());
    }
  }

  /**
   * Set Sites Schema with min / max values.
   */
  @Test
  public void testSetSitesSchema03() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      addAttribute(siteId, "category");
      SchemaAttributesRequired require0 = createCategoryRequired(List.of("A", "B"));
      require0.minNumberOfValues(Double.valueOf("20"));
      require0.maxNumberOfValues(Double.valueOf("2"));
      SchemaAttributes schemaAttributes = new SchemaAttributes().required(List.of(require0));

      // when
      Collection<ValidationError> errors = setSitesSchema(siteId, schemaAttributes);

      // then
      assertEquals(1, errors.size());
      assertEquals("minNumberOfValues cannot be more than maxNumberOfValues",
          errors.iterator().next().error());
    }
  }

  /**
   * Set Sites Schema with min / max values.
   */
  @Test
  public void testSetSitesSchema04() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      addAttribute(siteId, "category");
      SchemaAttributesRequired require0 = createCategoryRequired(List.of("A", "B"));
      require0.minNumberOfValues(Double.valueOf("2"));
      require0.maxNumberOfValues(Double.valueOf("2"));
      SchemaAttributes schemaAttributes = new SchemaAttributes().required(List.of(require0));

      // when
      Collection<ValidationError> errors = setSitesSchema(siteId, schemaAttributes);

      // then
      assertEquals(0, errors.size());

      // given
      require0.maxNumberOfValues(Double.valueOf("-1"));

      // when
      errors = setSitesSchema(siteId, schemaAttributes);

      // then
      assertEquals(0, errors.size());
    }
  }

  /**
   * Delete Sites schema.
   */
  @Test
  void testSitesClassificationDelete01() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      addAttribute(siteId, "category");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      SchemaAttributes schemaAttributes = new SchemaAttributes().required(List.of(require0));

      ClassificationRecord classification =
          setClassification(siteId, null, "doc", schemaAttributes);

      // when
      boolean deleted = service.deleteClassification(siteId, classification.getDocumentId());

      // then
      assertTrue(deleted);

      QueryConfig config = new QueryConfig();
      ClassificationRecord r =
          new ClassificationRecord().setDocumentId(classification.getDocumentId());
      AttributeValue pk = r.fromS(r.pk(siteId));
      QueryResponse response = db.queryBeginsWith(config, pk, null, null, LIMIT);
      assertEquals(0, response.items().size());
    }
  }

  /**
   * Delete Sites schema.
   */
  @Test
  void testSitesSchemaDelete01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      addAttribute(siteId, "category");

      SchemaAttributesRequired require0 = createCategoryRequired(List.of("Z", "Y"));
      Collection<ValidationError> errors =
          setSitesSchema(siteId, new SchemaAttributes().required(List.of(require0)));
      assertTrue(errors.isEmpty());

      // when
      errors = setSitesSchema(siteId, new SchemaAttributes());
      assertTrue(errors.isEmpty());

      // then
      QueryConfig config = new QueryConfig();

      SitesSchemaRecord r = new SitesSchemaRecord();
      AttributeValue pk = r.fromS(r.pk(siteId));
      QueryResponse response = db.queryBeginsWith(config, pk, null, null, LIMIT);
      assertEquals(1, response.items().size());
    }
  }
}
