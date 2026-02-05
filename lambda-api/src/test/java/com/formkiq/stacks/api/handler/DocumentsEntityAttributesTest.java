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
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.FindDocumentById;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.Entity;
import com.formkiq.client.model.EntityAttribute;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.api.SetBearer;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit Tests for request /entities and document attributes. */
public class DocumentsEntityAttributesTest extends AbstractApiClientRequestTest {

  /** {@link DocumentService}. */
  private static DocumentService service;
  /** {@link DocumentService}. */
  private static DynamoDbService db;

  @BeforeAll
  public static void setup() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    service = new DocumentServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE, DOCUMENT_SYNCS_TABLE,
        new DocumentVersionServiceNoVersioning());
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

  private DocumentAttribute getDocumentAttribute(final String siteId, final String documentId,
      final String attributeKey) throws ApiException {
    var resp = new GetDocumentAttributeRequestBuilder(documentId, attributeKey)
        .submit(client, siteId).throwIfError().response();
    return resp.getAttribute();
  }

  /**
   * Put Derived Attribute that does not exist.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testAddDocumentWithDerivedAttributeNotFound() throws ApiException {
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
      new SetBearer().apply(client, siteId);

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_INSERTED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      String documentId = resp.response().getDocumentId();
      verifyAttributes(siteId, documentId, entityTypeId, entityId, "DATE_INSERTED", "IN_EFFECT");
      assertEquals("IN_EFFECT",
          getDocumentAttribute(siteId, documentId, "RetentionEffectiveStatus").getStringValue());
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
      new SetBearer().apply(client, siteId);

      String entityTypeId = addRetentionEntityType(siteId);
      // DATE_LAST_MODIFIED
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_INSERTED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      String documentId = resp.response().getDocumentId();
      verifyAttributes(siteId, documentId, entityTypeId, entityId, "DATE_INSERTED", "IN_EFFECT");
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
      new SetBearer().apply(client, siteId);

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_INSERTED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();


      // then
      String documentId = resp.response().getDocumentId();

      // given
      Date insertedDate = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));

      DocumentRecord document =
          new FindDocumentById().find(db, DOCUMENTS_TABLE, siteId, documentId);
      document = new DocumentRecord(document.key(), documentId, insertedDate, new Date());

      // when
      db.putItem(document.getAttributes());

      // then
      verifyAttributes(siteId, documentId, entityTypeId, entityId, "DATE_INSERTED",
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
      new SetBearer().apply(client, siteId);

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_LAST_MODIFIED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      String documentId = resp.response().getDocumentId();
      verifyAttributes(siteId, documentId, entityTypeId, entityId, "DATE_LAST_MODIFIED",
          "IN_EFFECT");
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
      new SetBearer().apply(client, siteId);

      String entityTypeId = addRetentionEntityType(siteId);
      String entityId = addRetentionEntity(siteId, entityTypeId, "DATE_LAST_MODIFIED");

      // when
      var resp = new AddDocumentRequestBuilder().content()
          .addAttribute("RetentionPolicy", entityTypeId, entityId, EntityTypeNamespace.PRESET)
          .submit(client, siteId).throwIfError();

      // then
      String documentId = resp.response().getDocumentId();

      // given
      Date lastModifiedDate = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));

      DocumentRecord document =
          new FindDocumentById().find(db, DOCUMENTS_TABLE, siteId, documentId);
      document = new DocumentRecord(document.key(), documentId, new Date(), lastModifiedDate);

      // when
      db.putItem(document.getAttributes());

      // then
      verifyAttributes(siteId, documentId, entityTypeId, entityId, "DATE_LAST_MODIFIED",
          "NOT_IN_EFFECT");
    }
  }

  /**
   * Get Derived Attribute that does not exist.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testGetDerivedAttributeNotFound() throws ApiException {
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

  private void verifyAttributes(final String siteId, final String documentId,
      final String entityTypeId, final String entityId, final String sourceType,
      final String retentionEffectiveStatus) throws ApiException {

    var attr = new GetDocumentAttributeRequestBuilder(documentId, "RetentionPolicy")
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
