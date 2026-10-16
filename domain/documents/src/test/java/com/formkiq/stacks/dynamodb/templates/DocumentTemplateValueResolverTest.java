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
package com.formkiq.stacks.dynamodb.templates;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.DynamoDbQueryException;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentMetadata;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.schema.DocumentSchema;
import com.formkiq.strings.TemplateSubstitutor;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.google.gson.Gson;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests document value resolution using stored DynamoDB records. */
@ExtendWith(DynamoDbExtension.class)
class DocumentTemplateValueResolverTest {

  /** Test site. */
  private static final String SITE = "site";

  /** The exact artifact being rendered. */
  private static final DocumentArtifact DOCUMENT = DocumentArtifact.of("doc-123", "artifact-1");

  /** Fixed document date. */
  private static final Instant CREATED = Instant.parse("2026-10-15T12:00:00Z");

  /** Real database used for fixture writes and resolver lookups. */
  private DynamoDbService db;

  private Map<String, List<DocumentAttributeRecord>> attributes() {
    Map<String, Object> json =
        Map.of("customer", Map.of("name", "Acme"), "items", List.of(Map.of("description", "First"),
            Map.of("description", "Second"), Map.of("description", "Third")));
    return Map.of("CustomerName", List.of(text("CustomerName", "Acme")), "Reviewer",
        List.of(text("Reviewer", "Bob"), text("Reviewer", "Alice")), "Invoice.Number",
        List.of(text("Invoice.Number", "INV-123")), "Contract",
        List.of(new DocumentAttributeRecord().setUserId("owner").setDocument(DOCUMENT)
            .setKey("Contract").setJsonValue(json).updateValueType()),
        "Approved",
        List.of(new DocumentAttributeRecord().setUserId("owner").setDocument(DOCUMENT)
            .setKey("Approved").setBooleanValue(false).updateValueType()),
        "Amount",
        List.of(new DocumentAttributeRecord().setUserId("owner").setDocument(DOCUMENT)
            .setKey("Amount").setNumberValue(42.0).updateValueType()),
        "Due", List.of(new DocumentAttributeRecord().setUserId("owner").setDocument(DOCUMENT)
            .setKey("Due").setDateValue(Date.from(CREATED)).updateValueType()));
  }

  private ApiAuthorization authorization() {
    return new ApiAuthorization().siteId(SITE).username("mike@example.com")
        .userClaims(Map.of("custom:department", "Sales", "roles", List.of("reviewer", "editor")));
  }

  @BeforeEach
  void beforeEach() throws URISyntaxException {
    this.db =
        new DynamoDbServiceImpl(DynamoDbTestServices.getDynamoDbConnection(), DOCUMENTS_TABLE);
    this.db.putItem(document().getAttributes());
    this.db.putItems(attributes().values().stream().flatMap(List::stream)
        .map(record -> record.getAttributes(SITE)).toList());
  }

  private DocumentRecord document() {
    return DocumentRecord.builder().document(DOCUMENT).path("invoice.pdf").userId("owner")
        .contentType("application/pdf").contentLength(42L).insertedDate(Date.from(CREATED))
        .metadata(List.of(new DocumentMetadata("Department", "Legal", null),
            new DocumentMetadata("Reviewers", null, List.of("Alice", "Bob"))))
        .build(SITE);
  }

  private DocumentTemplateValueResolver resolver() {
    return new DocumentTemplateValueResolver(this.db, SITE, DOCUMENT, authorization());
  }

  /**
   * Resolves document fields, metadata, indexed attributes, nested JSON, and authentication claims
   * using the supported path syntax.
   */
  @Test
  void testAgreedReferences() {
    // given
    DocumentTemplateValueResolver resolver = resolver();
    Map<String, String> references = Map.ofEntries(Map.entry("attributes.CustomerName[0]", "Acme"),
        Map.entry("attributes.Reviewer[1]", "Bob"),
        Map.entry("attributes[\"Invoice.Number\"][0]", "INV-123"),
        Map.entry("attributes.Contract[0].customer.name", "Acme"),
        Map.entry("attributes.Contract[0].items[2].description", "Third"),
        Map.entry("document.documentId", "doc-123"), Map.entry("document.artifactId", "artifact-1"),
        Map.entry("document.path", "invoice.pdf"),
        Map.entry("document.contentType", "application/pdf"),
        Map.entry("document.insertedDate", CREATED.toString()),
        Map.entry("metadata.Department", "Legal"), Map.entry("metadata.Reviewers[1]", "Bob"),
        Map.entry("auth.username", "mike@example.com"),
        Map.entry("auth.claims[\"custom:department\"]", "Sales"),
        Map.entry("auth.claims.roles[0]", "reviewer"));
    for (Map.Entry<String, String> reference : references.entrySet()) {
      // when
      String value = resolver.resolve(reference.getKey());
      // then
      assertEquals(reference.getValue(), value, reference.getKey());
    }
  }

  /**
   * Orders indexed attribute values using DynamoDB's UTF-8 sort key ordering.
   */
  @Test
  void testAttributeOrderUsesUtf8SortKeys() {
    // given
    final String privateUse = Character.toString(0xe000);
    final String supplementary = Character.toString(0x1f600);
    this.db.putItem(text("UnicodeReviewer", supplementary).getAttributes(SITE));
    this.db.putItem(text("UnicodeReviewer", privateUse).getAttributes(SITE));
    DocumentTemplateValueResolver resolver = resolver();
    // when
    String first = resolver.resolve("attributes.UnicodeReviewer[0]");
    String second = resolver.resolve("attributes.UnicodeReviewer[1]");
    // then
    assertEquals(privateUse, first);
    assertEquals(supplementary, second);
  }

  /**
   * Resolves authentication values without accessing the document table.
   */
  @Test
  void testAuthValuesDoNotLoadDocumentData() {
    // given
    DynamoDbService missingTable =
        new DynamoDbServiceImpl(this.db.getClient(), "Unused-" + ID.uuid());
    DocumentTemplateValueResolver resolver =
        new DocumentTemplateValueResolver(missingTable, SITE, DOCUMENT, authorization());
    // when
    String result = new TemplateSubstitutor("{{", "}}").substituteWithResolver(
        "{{auth.username}} / {{auth.claims[\"custom:department\"]}}", resolver);
    // then
    assertEquals("mike@example.com / Sales", result);
  }

  /**
   * Keeps cached JSON stable after database updates and isolates each resolver's requesting user.
   */
  @Test
  void testCachedJsonAndOperationIsolation() {
    // given
    DocumentAttributeRecord attribute =
        new DocumentAttributeRecord().setUserId("owner").setDocument(DOCUMENT).setKey("Contract")
            .setJsonValue(Map.of("name", "original")).updateValueType();
    this.db.putItem(attribute.getAttributes(SITE));
    final DocumentTemplateValueResolver first = resolver();
    final DocumentTemplateValueResolver second = new DocumentTemplateValueResolver(this.db, SITE,
        DOCUMENT, authorization().username("jane@example.com"));
    // when
    String original = first.resolve("attributes.Contract[0].name");
    // then
    assertEquals("original", original);

    // given
    this.db.putItem(attribute.setJsonValue(Map.of("name", "changed")).getAttributes(SITE));
    // when
    String cached = first.resolve("attributes.Contract[0].name");
    String fresh = second.resolve("attributes.Contract[0].name");
    // then
    assertEquals("original", cached);
    assertEquals("changed", fresh);
    assertEquals("mike@example.com", first.resolve("auth.username"));
    assertEquals("jane@example.com", second.resolve("auth.username"));
  }

  /**
   * Uses a custom formatter for lists while retaining default scalar rendering.
   */
  @Test
  void testCustomFormatter() {
    // given
    DocumentTemplateValueResolver resolver = new DocumentTemplateValueResolver(this.db, SITE,
        DOCUMENT, authorization(), value -> value instanceof List<?> ? new Gson().toJson(value)
            : DocumentTemplateValueResolver.formatScalar(value));
    // when
    String result = resolver.resolve("attributes.Reviewer");
    String username = resolver.resolve("auth.username");
    // then
    assertEquals("[\"Alice\",\"Bob\"]", result);
    assertEquals("mike@example.com", username);
  }

  /**
   * Propagates DynamoDB lookup errors and permits the same resolver to retry after the table is
   * created.
   */
  @Test
  void testDatabaseErrorsPropagateAndAreNotCached() {
    // given
    final String tableName = "TemplateResolver-" + ID.uuid();
    DynamoDbService recoveringDb = new DynamoDbServiceImpl(this.db.getClient(), tableName);
    DocumentTemplateValueResolver resolver =
        new DocumentTemplateValueResolver(recoveringDb, SITE, DOCUMENT, authorization());
    // when
    Executable documentLookup = () -> resolver.resolve("document.path");
    Executable attributeLookup = () -> resolver.resolve("attributes.CustomerName[0]");
    // then
    assertThrows(ResourceNotFoundException.class, documentLookup);
    assertThrows(DynamoDbQueryException.class, attributeLookup);

    new DocumentSchema(this.db.getClient()).createDocumentsTable(tableName);
    try {
      recoveringDb.putItem(document().getAttributes());
      recoveringDb.putItem(text("CustomerName", "Acme").getAttributes(SITE));
      // when
      String path = resolver.resolve("document.path");
      String customer = resolver.resolve("attributes.CustomerName[0]");
      // then
      assertEquals("invoice.pdf", path);
      assertEquals("Acme", customer);
    } finally {
      this.db.getClient().deleteTable(request -> request.tableName(tableName));
    }
  }

  /**
   * Resolves default-site document data with the System username and no custom claims.
   */
  @Test
  void testDefaultSiteAndSystemAuthorization() {
    // given
    this.db.putItem(DocumentRecord.builder().document(DOCUMENT).path("default-site.pdf")
        .build((String) null).getAttributes());
    DocumentTemplateValueResolver resolver = new DocumentTemplateValueResolver(this.db,
        DEFAULT_SITE_ID, DOCUMENT, new ApiAuthorization());
    // when
    String username = resolver.resolve("auth.username");
    String missingClaim = resolver.resolve("auth.claims.missing");
    String path = resolver.resolve("document.path");
    // then
    assertEquals("System", username);
    assertNull(missingClaim);
    assertEquals("default-site.pdf", path);
  }

  /**
   * Scopes document fields, metadata, and attribute lookups to the requested artifact.
   */
  @Test
  void testDocumentAndArtifactLookupsAreIsolated() {
    // given
    DocumentArtifact parent = DocumentArtifact.of(DOCUMENT.documentId(), null);
    DocumentArtifact otherArtifact = DocumentArtifact.of(DOCUMENT.documentId(), "other-artifact");
    this.db.putItem(
        DocumentRecord.builder().document(parent).path("parent.pdf").build(SITE).getAttributes());
    this.db.putItem(DocumentRecord.builder().document(otherArtifact).path("other.pdf").build(SITE)
        .getAttributes());
    this.db.putItem(text("Reviewer", "Parent reviewer").setDocument(parent).getAttributes(SITE));
    this.db
        .putItem(text("Reviewer", "Other reviewer").setDocument(otherArtifact).getAttributes(SITE));
    DocumentTemplateValueResolver resolver = resolver();
    // when
    String path = resolver.resolve("document.path");
    String department = resolver.resolve("metadata.Department");
    Object reviewers = resolver.resolveValue("attributes.Reviewer");
    // then
    assertEquals("invoice.pdf", path);
    assertEquals("Legal", department);
    assertEquals(List.of("Alice", "Bob"), reviewers);
  }

  /**
   * Reuses cached document fields, metadata, and attribute values, including missing values, after
   * database changes.
   */
  @Test
  void testDocumentAndAttributeValuesAreCached() {
    // given
    final String template = "${document.path}/${metadata.Department}/${attributes.Reviewer[0]}/"
        + "${attributes.Reviewer[1]}/${attributes.Missing[0]}";
    final String original = "invoice.pdf/Legal/Alice/Bob/${attributes.Missing[0]}";
    TemplateSubstitutor substitutor = new TemplateSubstitutor("${", "}");
    DocumentTemplateValueResolver resolver = resolver();
    // when
    String first = substitutor.substituteWithResolver(template, resolver);
    // then
    assertEquals(original, first);

    // given
    this.db.putItem(DocumentRecord.builder().document(DOCUMENT).path("updated.pdf")
        .metadata(List.of(new DocumentMetadata("Department", "Sales", null))).build(SITE)
        .getAttributes());
    this.db.deleteItem(DynamoDbKey.fromAttributeMap(text("Reviewer", "Alice").getAttributes(SITE)));
    this.db.deleteItem(DynamoDbKey.fromAttributeMap(text("Reviewer", "Bob").getAttributes(SITE)));
    this.db.putItem(text("Reviewer", "Carol").getAttributes(SITE));
    this.db.putItem(text("Missing", "Present").getAttributes(SITE));
    // when
    String cached = substitutor.substituteWithResolver(template, resolver);
    String fresh = substitutor.substituteWithResolver(template, resolver());
    // then
    assertEquals(original, cached);
    assertEquals("updated.pdf/Sales/Carol/${attributes.Reviewer[1]}/Present", fresh);
  }

  /**
   * Scopes document, metadata, and attribute lookups to the explicitly supplied site.
   */
  @Test
  void testExplicitSiteOverridesAuthorizationDefaultForAllLookups() {
    // given
    final String otherSite = "other-site";
    this.db.putItem(DocumentRecord.builder().document(DOCUMENT).path("other-site.pdf")
        .metadata(List.of(new DocumentMetadata("Department", "Other", null))).build(otherSite)
        .getAttributes());
    this.db.putItem(text("Reviewer", "Site reviewer").getAttributes(otherSite));
    DocumentTemplateValueResolver resolver =
        new DocumentTemplateValueResolver(this.db, otherSite, DOCUMENT, authorization());
    // when
    String path = resolver.resolve("document.path");
    String department = resolver.resolve("metadata.Department");
    String reviewer = resolver.resolve("attributes.Reviewer[0]");
    // then
    assertEquals("other-site.pdf", path);
    assertEquals("Other", department);
    assertEquals("Site reviewer", reviewer);
    assertEquals("invoice.pdf", resolver().resolve("document.path"));
    assertEquals("Alice", resolver().resolve("attributes.Reviewer[0]"));
  }

  /**
   * Loads every attribute page so values beyond the first 100 records remain accessible.
   */
  @Test
  void testFollowsAllAttributePages() {
    // given
    this.db
        .putItems(
            IntStream.range(0, 101)
                .mapToObj(index -> text("PagedReviewer",
                    String.format(Locale.ROOT, "reviewer-%03d", index)).getAttributes(SITE))
                .toList());
    DocumentTemplateValueResolver resolver = resolver();
    // when
    String last = resolver.resolve("attributes.PagedReviewer[100]");
    String first = resolver.resolve("attributes.PagedReviewer[0]");
    Object reviewers = resolver.resolveValue("attributes.PagedReviewer");
    // then
    assertEquals("reviewer-100", last);
    assertEquals("reviewer-000", first);
    assertEquals(101, assertInstanceOf(List.class, reviewers).size());
  }

  /**
   * Rejects malformed paths, negative indices, and attribute paths without a key.
   */
  @Test
  void testInvalidPathsAndMissingAttributeKey() {
    // given
    DocumentTemplateValueResolver resolver = resolver();
    for (String expression : Arrays.asList(null, "", "document..path", "attributes.Reviewer[-1]",
        "attributes")) {
      // when
      Executable lookup = () -> resolver.resolve(expression);
      // then
      assertThrows(IllegalArgumentException.class, lookup);
    }
  }

  /**
   * Returns null for missing values, indices outside the available range, and paths incompatible
   * with a value's type.
   */
  @Test
  void testMissingAndIncompatibleValues() {
    // given
    DocumentTemplateValueResolver resolver = resolver();
    for (String expression : List.of("attributes.Missing[0]", "attributes.Reviewer[9]",
        "attributes.Reviewer[999999999999999999999]", "auth.claims.missing", "metadata.Missing",
        "document.missing", "unknown.value", "attributes[0]", "document.path.child")) {
      // when
      String result = resolver.resolve(expression);
      // then
      assertNull(result, expression);
    }
  }

  /**
   * Caches a missing document result until a new resolver is created.
   */
  @Test
  void testMissingDocumentIsCached() {
    // given
    this.db.deleteItem(DocumentRecord.builder().document(DOCUMENT).buildKey(SITE));
    DocumentTemplateValueResolver resolver = resolver();
    // when
    String path = resolver.resolve("document.path");
    // then
    assertNull(path);

    // given
    this.db.putItem(document().getAttributes());
    // when
    String cached = resolver.resolve("document.path");
    String department = resolver.resolve("metadata.Department");
    String fresh = resolver().resolve("document.path");
    // then
    assertNull(cached);
    assertNull(department);
    assertEquals("invoice.pdf", fresh);
  }

  /**
   * Preserves native number, boolean, and date values and renders them as scalar text.
   */
  @Test
  void testNativeTypesAndScalarFormatting() {
    // given
    DocumentTemplateValueResolver resolver = resolver();
    // when
    Object number = resolver.resolveValue("attributes.Amount[0]");
    Object approved = resolver.resolveValue("attributes.Approved[0]");
    final Object date = resolver.resolveValue("document.insertedDate");
    final String numberText = resolver.resolve("attributes.Amount[0]");
    final String approvedText = resolver.resolve("attributes.Approved[0]");
    final String dateText = resolver.resolve("attributes.Due[0]");
    // then
    assertInstanceOf(Double.class, number);
    assertEquals(42.0, number);
    assertEquals(false, approved);
    assertEquals(Date.from(CREATED), date);
    assertEquals("42.0", numberText);
    assertEquals("false", approvedText);
    assertEquals(attributes().get("Due").getFirst().getDateValueAsString(), dateText);
  }

  /**
   * Accepts rooted paths and surrounding whitespace while distinguishing document ownership from
   * the requesting username.
   */
  @Test
  void testRootedPathsAndWhitespace() {
    // given
    DocumentTemplateValueResolver resolver = resolver();
    // when
    String path = resolver.resolve(" $.document.path ");
    String owner = resolver.resolve("document.userId");
    String user = resolver.resolve("auth.username");
    // then
    assertEquals("invoice.pdf", path);
    assertEquals("owner", owner);
    assertEquals("mike@example.com", user);
  }

  /**
   * Returns structured values through {@link DocumentTemplateValueResolver#resolveValue(String)}
   * and requires an explicit formatter to render entire maps or lists.
   */
  @Test
  void testStructuredValuesRequireExplicitFormatting() {
    // given
    DocumentTemplateValueResolver resolver = resolver();
    // when
    Object json = resolver.resolveValue("attributes.Contract[0]");
    Object reviewers = resolver.resolveValue("attributes.Reviewer");
    Executable objectText = () -> resolver.resolve("attributes.Contract[0]");
    final Executable listText = () -> resolver.resolve("attributes.Reviewer");
    // then
    assertInstanceOf(Map.class, json);
    assertEquals(List.of("Alice", "Bob"), reviewers);
    assertThrows(IllegalArgumentException.class, objectText);
    assertThrows(IllegalArgumentException.class, listText);
  }

  private DocumentAttributeRecord text(final String key, final String value) {
    return new DocumentAttributeRecord().setDocument(DOCUMENT).setUserId("owner").setKey(key)
        .setStringValue(value).updateValueType();
  }
}
