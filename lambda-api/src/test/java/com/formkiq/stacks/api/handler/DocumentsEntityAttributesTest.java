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
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.documents.FindDocumentById;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.Entity;
import com.formkiq.client.model.EntityAttribute;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.testutils.api.SetBearer;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributesRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.entity.AddEntityRequestBuilder;
import com.formkiq.testutils.api.entity.AddEntityTypeRequestBuilder;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.urls.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit Tests for request /entities and document attributes. */
public class DocumentsEntityAttributesTest extends AbstractApiClientRequestTest {

  /** {@link DocumentService}. */
  private static DynamoDbService db;

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

  private DocumentAttribute getDocumentAttribute(final String siteId,
      final DocumentArtifact document) throws ApiException {
    var resp = new GetDocumentAttributeRequestBuilder(document, "RetentionEffectiveStatus")
        .submit(client, siteId).throwIfError().response();
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
      assertEquals("IN_EFFECT", getDocumentAttribute(siteId, document).getStringValue());

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
      var getAttribute =
          new GetAttributeRequestBuilder("RetentionPolicy").submit(client, siteId).throwIfError();
      assertAttribute(Objects.requireNonNull(getAttribute.response().getAttribute()),
          "RetentionPolicy", AttributeType.GOVERNANCE, AttributeDataType.ENTITY);

      DocumentArtifact documentArtifact =
          DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());

      // given
      Date insertedDate = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));

      DocumentRecord document =
          new FindDocumentById().find(db, DOCUMENTS_TABLE, siteId, documentArtifact);
      document = new DocumentRecordBuilder().documentId(document.documentId())
          .insertedDate(insertedDate).lastModifiedDate(new Date()).build(siteId);
      // document = new DocumentRecord(document.key(), documentId, insertedDate, new Date());

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

      // given
      Date lastModifiedDate = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));

      DocumentRecord document =
          new FindDocumentById().find(db, DOCUMENTS_TABLE, siteId, documentArtifact);
      document = new DocumentRecordBuilder().documentId(document.documentId())
          .insertedDate(new Date()).lastModifiedDate(lastModifiedDate).build(siteId);
      // document = new DocumentRecord(document.key(), documentId, new Date(), lastModifiedDate);

      // when
      db.putItem(document.getAttributes());

      // then
      verifyAttributes(siteId, documentArtifact, entityTypeId, entityId, "DATE_LAST_MODIFIED",
          "NOT_IN_EFFECT");
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
      assertEquals(2, notNull(getAttributes.response().getAttributes()).size());

      // when
      var resp = new GetAttributeRequestBuilder("RetentionPeriodInDays").submit(client, siteId)
          .throwIfError();

      // then
      Attribute attribute = resp.response().getAttribute();
      assertNotNull(attribute);
      assertAttribute(attribute, "RetentionPeriodInDays", AttributeType.STANDARD,
          AttributeDataType.NUMBER);

      // when
      resp = new GetAttributeRequestBuilder("RetentionStartDateSourceType").submit(client, siteId)
          .throwIfError();

      // then
      attribute = resp.response().getAttribute();
      assertNotNull(attribute);
      assertAttribute(attribute, "RetentionStartDateSourceType", AttributeType.STANDARD,
          AttributeDataType.STRING);
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

  private void verifyAttributes(final String siteId, final DocumentArtifact document,
      final String entityTypeId, final String entityId, final String sourceType,
      final String retentionEffectiveStatus) throws ApiException {

    var attr = new GetDocumentAttributeRequestBuilder(document, "RetentionPolicy")
        .submit(client, siteId).throwIfError().response().getAttribute();

    assertNotNull(attr);
    assertEquals("RetentionPolicy", attr.getKey());
    assertEquals(entityTypeId + "#" + entityId, attr.getStringValue());
    Entity entity = attr.getEntity();

    assertNotNull(entity);
    List<EntityAttribute> attributes = notNull(entity.getAttributes());
    assertEquals(5, attributes.size());
    assertEntityAttributeEquals(attributes.get(0), "RetentionPeriodInDays", null, "10.0");
    assertEntityAttributeEquals(attributes.get(1), "RetentionStartDateSourceType", sourceType,
        null);
    assertEquals("RetentionEffectiveStartDate", attributes.get(2).getKey());
    assertEquals("RetentionEffectiveEndDate", attributes.get(3).getKey());
    assertEntityAttributeEquals(attributes.get(4), "RetentionEffectiveStatus",
        retentionEffectiveStatus, null);
  }

}
