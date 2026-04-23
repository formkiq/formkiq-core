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
package com.formkiq.stacks.api.handler;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.documents.FindDocumentById;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.Entity;
import com.formkiq.client.model.EntityAttribute;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.lambda.s3.DocumentsS3Update;
import com.formkiq.testutils.api.SetBearer;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributesRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.UpdateDocumentRequestBuilder;
import com.formkiq.testutils.api.entity.AddEntityRequestBuilder;
import com.formkiq.testutils.api.entity.AddEntityTypeRequestBuilder;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.s3.S3EventJsonBuilder;
import com.formkiq.urls.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.entity.RetentionStartDateSourceType.DATE_INSERTED;
import static com.formkiq.aws.dynamodb.entity.RetentionStartDateSourceType.DATE_LAST_MODIFIED;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.client.model.AttributeDataType.NUMBER;
import static com.formkiq.client.model.AttributeDataType.STRING;
import static com.formkiq.client.model.AttributeType.STANDARD;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit Tests for request /entities and document attributes. */
public class DocumentsEntityAttributesTest extends AbstractApiClientRequestTest {

  /** {@link DocumentService}. */
  private static DynamoDbService db;

  private static Map<String, Object> createS3Map(final String siteId,
      final DocumentArtifact document) {
    String s3Key =
        SiteIdKeyGenerator.createS3Key(siteId, document.documentId(), document.artifactId());

    return new S3EventJsonBuilder()
        .addRecord(new S3EventJsonBuilder.RecordBuilder().withEventName("ObjectCreated:Put")
            .withS3(new S3EventJsonBuilder.S3Builder().withBucket(BUCKET_NAME).withObject(s3Key)))
        .build();
  }

  @BeforeAll
  public static void setup() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    db = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
  }

  private String addRetentionEntity(final String siteId, final String entityTypeId,
      final String retentionStartDateSourceType) throws ApiException {
    return new AddEntityRequestBuilder(entityTypeId).name("rt" + ID.ulid())
        .addAttribute("RetentionPeriodInDays", new BigDecimal("10"))
        .addAttribute("RetentionStartDateSourceType", retentionStartDateSourceType)
        .submit(client, siteId).throwIfError().response().getEntityId();
  }

  private String addRetentionEntityType(final String siteId) throws ApiException {
    return new AddEntityTypeRequestBuilder()
        .setEntityType("RetentionPolicy", EntityTypeNamespace.PRESET).submit(client, siteId)
        .throwIfError().response().getEntityTypeId();
  }

  private String addRetentionEntityWithDispositionPeriod(final String siteId,
      final String entityTypeId, final String retentionStartDateSourceType) throws ApiException {
    return new AddEntityRequestBuilder(entityTypeId).name("rt" + ID.ulid())
        .addAttribute("RetentionStartDateSourceType", retentionStartDateSourceType)
        .addAttribute("RetentionPeriodInDays", new BigDecimal("0"))
        .addAttribute("DispositionPeriodInDays", new BigDecimal("11")).submit(client, siteId)
        .throwIfError().response().getEntityId();
  }

  private void assertAttribute(final Attribute attribute, final String key,
      final AttributeType type, final AttributeDataType dataType) {
    assertEquals(key, attribute.getKey());
    assertEquals(type, attribute.getType());
    assertEquals(dataType, attribute.getDataType());

  }

  private void assertEntityAttributeEquals(final EntityAttribute attr, final String key,
      final String stringValue, final String numberValue) {
    assertEquals(key, attr.getKey());

    if (stringValue != null) {
      assertEquals(stringValue, attr.getStringValue());
    }

    if (numberValue != null) {
      assertNotNull(attr.getNumberValue());
      assertEquals(numberValue, attr.getNumberValue().toString());
    }
  }

  private Attribute getAttribute(final String siteId, final String attributeKey)
      throws ApiException {
    return new GetAttributeRequestBuilder(attributeKey).submit(client, siteId).throwIfError()
        .response().getAttribute();
  }

  private DocumentAttribute getDocumentAttribute(final String siteId,
      final DocumentArtifact document, final String attributeKey) throws ApiException {
    var resp = new GetDocumentAttributeRequestBuilder(document, attributeKey).submit(client, siteId)
        .throwIfError().response();
    return resp.getAttribute();
  }

  /**
   * Put Derived Attribute that does not exist.
   *
   */
  @Test
  void testAddDocumentWithDerivedAttributeNotFound() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId);

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionEffectiveStatus", "123").submit(client, siteId);

      // then
      assertNull(resp.response());
      assertNotNull(resp.exception());
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"RetentionEffectiveStatus\","
              + "\"error\":\"attribute key is derived reserved\"}]}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * Add Retention (DATE_INSERTED) to Document.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testAddRetentionEntityToDocument() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_INSERTED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      DocumentArtifact document =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());
      verifyAttributes(siteId, document, entityTypeId, entityId, "DATE_INSERTED", "IN_EFFECT");
      assertEquals("IN_EFFECT",
          getDocumentAttribute(siteId, document, "RetentionEffectiveStatus").getStringValue());

      // given
      new SetBearer().apply(client, siteId);

      // when
      var delete = new DeleteDocumentAttributeRequestBuilder(document, "RetentionPolicy")
          .submit(client, siteId);

      // then
      assertEquals(HttpStatus.BAD_REQUEST, delete.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"RetentionPolicy\","
              + "\"error\":\"attribute 'RetentionPolicy' is an protected attribute, "
              + "can only be changed by Goverance/Admin role\"}]}",
          delete.exception().getResponseBody());

      // given
      new SetBearer().apply(client, siteId + "_govern");

      // when
      delete = new DeleteDocumentAttributeRequestBuilder(document, "RetentionPolicy").submit(client,
          siteId);

      // then
      assertNotNull(delete.response());
      assertNull(delete.exception());
    }
  }

  /**
   * Add Retention (DATE_INSERTED) to Document IN_EFFECT.
   * 
   * @throws ApiException ApiException
   */
  @Test
  void testAddRetentionEntityToDocumentDateInsertedInEffect() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_INSERTED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      DocumentArtifact document =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());
      verifyAttributes(siteId, document, entityTypeId, entityId, "DATE_INSERTED", "IN_EFFECT");

      // when
      new DeleteDocumentRequestBuilder(document).submit(client, siteId).throwIfError();
    }
  }

  /**
   * Add Retention (DATE_INSERTED) to Document NOT_IN_EFFECT.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testAddRetentionEntityToDocumentDateInsertedNotInEffect() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_INSERTED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      var attribute = getAttribute(siteId, "RetentionPolicy");
      assertAttribute(attribute, "RetentionPolicy", AttributeType.GOVERNANCE,
          AttributeDataType.ENTITY);

      DocumentArtifact documentArtifact =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());

      // given
      Date insertedDate = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));

      DocumentRecord document =
          new FindDocumentById().find(db, DOCUMENTS_TABLE, siteId, documentArtifact);
      document = new DocumentRecordBuilder().documentId(document.documentId())
          .insertedDate(insertedDate).lastModifiedDate(new Date()).build(siteId);

      // when
      db.putItem(document.getAttributes());

      // then
      verifyAttributes(siteId, documentArtifact, entityTypeId, entityId, "DATE_INSERTED",
          "NOT_IN_EFFECT");
    }
  }

  /**
   * Add Retention (DATE_LAST_MODIFIED) to Document IN_EFFECT.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testAddRetentionEntityToDocumentDateLastModifiedInEffect() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_LAST_MODIFIED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      DocumentArtifact document =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());
      verifyAttributes(siteId, document, entityTypeId, entityId, "DATE_LAST_MODIFIED", "IN_EFFECT");
    }
  }

  /**
   * Add Retention (DATE_INSERTED) to Document NOT_IN_EFFECT.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testAddRetentionEntityToDocumentDateLastModifiedNotInEffect() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_LAST_MODIFIED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      DocumentArtifact documentArtifact =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());

      final String dispositionDateBeforeUpdate =
          getDocumentAttribute(siteId, documentArtifact, "DispositionDate").getStringValue();

      // given
      Date lastModifiedDate = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));

      DocumentRecord document =
          new FindDocumentById().find(db, DOCUMENTS_TABLE, siteId, documentArtifact);
      document = new DocumentRecordBuilder().documentId(document.documentId())
          .insertedDate(new Date()).lastModifiedDate(lastModifiedDate).build(siteId);

      // when
      db.putItem(document.getAttributes());

      // then
      verifyAttributes(siteId, documentArtifact, entityTypeId, entityId, "DATE_LAST_MODIFIED",
          "NOT_IN_EFFECT");
      assertEquals(dispositionDateBeforeUpdate,
          getDocumentAttribute(siteId, documentArtifact, "DispositionDate").getStringValue());
    }
  }

  /**
   * Add RetentionPolicy with disposition period to Document.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testAddRetentionEntityToDocumentStoresDispositionDate() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId =
          addRetentionEntityWithDispositionPeriod(siteId, entityTypeId, "DATE_INSERTED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      DocumentArtifact document =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());
      DocumentRecord documentRecord =
          new FindDocumentById().find(db, DOCUMENTS_TABLE, siteId, document);
      Date expectedDispositionDate =
          Date.from(documentRecord.insertedDate().toInstant().plus(11, ChronoUnit.DAYS));
      String expectedDispositionDateValue =
          DateUtil.getIsoDateFormatter().format(expectedDispositionDate);

      assertEquals(expectedDispositionDateValue,
          getDocumentAttribute(siteId, document, "DispositionDate").getStringValue());
    }
  }

  @Test
  void testAddRetentionEntityType() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      addRetentionEntityType(siteId);

      // when
      var getAttributes = new GetAttributesRequestBuilder().submit(client, siteId).throwIfError();

      // then
      assertEquals(4, notNull(getAttributes.response().getAttributes()).size());

      // when
      Attribute attribute = getAttribute(siteId, "RetentionPeriodInDays");

      // then
      assertNotNull(attribute);
      assertAttribute(attribute, "RetentionPeriodInDays", STANDARD, NUMBER);

      // when
      attribute = getAttribute(siteId, "RetentionStartDateSourceType");

      // then
      assertNotNull(attribute);
      assertAttribute(attribute, "RetentionStartDateSourceType", STANDARD, STRING);

      // when
      attribute = getAttribute(siteId, "DispositionDate");

      // then
      assertNotNull(attribute);
      assertAttribute(attribute, "DispositionDate", STANDARD, STRING);

      // when
      attribute = getAttribute(siteId, "DispositionPeriodInDays");

      // then
      assertNotNull(attribute);
      assertAttribute(attribute, "DispositionPeriodInDays", STANDARD, NUMBER);
    }
  }

  /**
   * Get Derived Attribute that does not exist.
   *
   */
  @Test
  void testGetDerivedAttributeNotFound() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId);

      // when
      var resp = new AddDocumentRequestBuilder().content().submit(client, siteId);

      // then
      String documentId = resp.response().getDocumentId();
      var getResp = new GetDocumentAttributeRequestBuilder(documentId, "RetentionEffectiveStatus")
          .submit(client, siteId);
      assertEquals(HttpStatus.NOT_FOUND, getResp.exception().getCode());
      assertEquals("{\"message\":\"attribute 'RetentionEffectiveStatus' not found on document '"
          + documentId + "'\"}", getResp.exception().getResponseBody());
    }
  }

  /**
   * Add RetentionPolicy as invalid Entity String value.
   *
   */
  @Test
  void testRetentionPolicyInvalidEntityValue() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", "asdasd#dfsdfds").submit(client, siteId);

      // then
      assertNull(resp.response());
      assertNotNull(resp.exception());
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"entityTypeId\",\"error\":\"EntityTypeId does not exist\"},"
              + "{\"key\":\"entityId\",\"error\":\"EntityId does not exist\"}]}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * Add RetentionPolicy as invalid String value.
   *
   */
  @Test
  void testRetentionPolicyInvalidStringValue() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      // when
      var resp = new AddDocumentRequestBuilder().content().addAttribute("RetentionPolicy", "asdasd")
          .submit(client, siteId);

      // then
      assertNull(resp.response());
      assertNotNull(resp.exception());
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"stringValue\",\"error\":\"invalid 'stringValue' for Entity\"}]}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * Update Document Content with Retention (DATE_INSERTED_DATE).
   *
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  @Test
  void testUpdateDocumentContentWithDateInsertedDateRetentionUpdatesDispositionDate()
      throws ApiException, InterruptedException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      final String entityTypeId = addRetentionEntityType(siteId);
      final String entityId =
          addRetentionEntityWithDispositionPeriod(siteId, entityTypeId, DATE_INSERTED.name());

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      DocumentArtifact document =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());

      final var doc =
          new GetDocumentRequestBuilder(document).submit(client, siteId).throwIfError().response();
      final String originalDispositionDate =
          getDocumentAttribute(siteId, document, "DispositionDate").getStringValue();

      // when
      Thread.sleep(1000);
      new UpdateDocumentRequestBuilder(document).content().submit(client, siteId).throwIfError();
      new DocumentsS3Update(getAwsServices()).handleRequest(createS3Map(siteId, document), null);

      // then
      var updatedDoc =
          new GetDocumentRequestBuilder(document).submit(client, siteId).throwIfError().response();
      assertNotEquals(doc.getLastModifiedDate(), updatedDoc.getLastModifiedDate());

      String updatedDispositionDate =
          getDocumentAttribute(siteId, document, "DispositionDate").getStringValue();
      assertEquals(originalDispositionDate, updatedDispositionDate);
    }
  }

  /**
   * Update Document Content with Retention (DATE_LAST_MODIFIED).
   *
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  @Test
  void testUpdateDocumentContentWithDateLastModifiedRetentionUpdatesDispositionDate()
      throws ApiException, InterruptedException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearer().apply(client, siteId + "_govern");

      final String entityTypeId = addRetentionEntityType(siteId);
      final String entityId =
          addRetentionEntityWithDispositionPeriod(siteId, entityTypeId, DATE_LAST_MODIFIED.name());

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      DocumentArtifact document =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());

      final var doc =
          new GetDocumentRequestBuilder(document).submit(client, siteId).throwIfError().response();
      final String originalDispositionDate =
          getDocumentAttribute(siteId, document, "DispositionDate").getStringValue();

      // when - update document content
      Thread.sleep(1000);
      new UpdateDocumentRequestBuilder(document).content().submit(client, siteId).throwIfError();
      new DocumentsS3Update(getAwsServices()).handleRequest(createS3Map(siteId, document), null);

      // then
      var updatedDoc =
          new GetDocumentRequestBuilder(document).submit(client, siteId).throwIfError().response();
      assertNotEquals(doc.getLastModifiedDate(), updatedDoc.getLastModifiedDate());

      var updatedDispositionDate =
          getDocumentAttribute(siteId, document, "DispositionDate").getStringValue();
      assertNotEquals(originalDispositionDate, updatedDispositionDate);
    }
  }

  private void verifyAttributes(final String siteId, final DocumentArtifact document,
      final String entityTypeId, final String entityId, final String sourceType,
      final String retentionEffectiveStatus) throws ApiException {

    // when
    var attr = getDocumentAttribute(siteId, document, "RetentionPolicy");

    // then
    assertEquals("RetentionPolicy", attr.getKey());
    assertEquals(entityTypeId + "#" + entityId, attr.getStringValue());

    Entity entity = attr.getEntity();
    assertNotNull(entity);

    Map<String, EntityAttribute> attributes = notNull(entity.getAttributes()).stream()
        .collect(Collectors.toMap(a -> Objects.requireNonNull(a.getKey()), Function.identity()));

    assertEquals(5, attributes.size());
    assertEntityAttributeEquals(Objects.requireNonNull(attributes.get("RetentionPeriodInDays")),
        "RetentionPeriodInDays", null, "10.0");

    assertEntityAttributeEquals(
        Objects.requireNonNull(attributes.get("RetentionStartDateSourceType")),
        "RetentionStartDateSourceType", sourceType, null);

    assertEquals("RetentionEffectiveStartDate",
        Objects.requireNonNull(attributes.get("RetentionEffectiveStartDate")).getKey());
    assertEquals("RetentionEffectiveEndDate",
        Objects.requireNonNull(attributes.get("RetentionEffectiveEndDate")).getKey());

    assertEntityAttributeEquals(Objects.requireNonNull(attributes.get("RetentionEffectiveStatus")),
        "RetentionEffectiveStatus", retentionEffectiveStatus, null);
  }

  // add schema test...
}
