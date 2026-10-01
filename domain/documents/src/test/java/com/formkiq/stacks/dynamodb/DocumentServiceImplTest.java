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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.dynamodb.DocumentService.SYSTEM_DEFINED_TAGS;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.documents.GetDocumentFind;
import com.formkiq.aws.dynamodb.folders.FolderIndexRecord;
import com.formkiq.aws.dynamodb.folders.GetFolderFilesByNameQuery;
import com.formkiq.aws.dynamodb.folders.PathToFolderIndexRecords;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;
import com.formkiq.aws.dynamodb.model.DocumentTagRecord;
import com.formkiq.aws.dynamodb.model.DocumentTagRecordBuilder;
import com.formkiq.aws.dynamodb.model.SearchQueryBuilder;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.aws.dynamodb.attributes.AttributeType;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceExtension;
import com.formkiq.stacks.dynamodb.attributes.Watermark;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorExtension;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesRequired;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.stacks.dynamodb.schemas.SchemaServiceDynamodb;
import com.formkiq.testutils.aws.TestEnvironment;
import com.formkiq.testutils.aws.TestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.documents.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Unit Tests for {@link DocumentServiceImpl}.
 */
@ExtendWith(DynamoDbExtension.class)
public class DocumentServiceImplTest implements DbKeys {

  /** {@link DocumentSearchService}. */
  private static DocumentSearchService searchService;
  /** {@link DocumentService}. */
  private static DocumentService service;
  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor folderIndexProcessor;
  /** {@link AttributeService}. */
  private static AttributeService attributeService;
  /** {@link DynamoDbService}. */
  private static DynamoDbService db;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {

    var awsCredentialsProvider = TestEnvironment.createCredentials();
    var environment = TestEnvironment.builder()
        .environment("PARENT_LAST_MODIFIED_UPDATE_INTERVAL", "1000").build();
    var awsServiceCache = new AwsServiceCacheBuilder(environment, TestServices.getEndpointMap(),
        awsCredentialsProvider).addService(new DynamoDbAwsServiceRegistry())
        .addService(new S3AwsServiceRegistry()).build();

    awsServiceCache.register(DynamoDbService.class, new DynamoDbServiceExtension());
    awsServiceCache.register(AttributeService.class, new AttributeServiceExtension());
    awsServiceCache.register(FolderIndexProcessor.class, new FolderIndexProcessorExtension());
    awsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    awsServiceCache.register(DocumentSearchService.class, new DocumentSearchServiceExtension());
    awsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServiceCache.register(S3Service.class, new S3ServiceExtension());

    service = awsServiceCache.getExtension(DocumentService.class);
    searchService = awsServiceCache.getExtension(DocumentSearchService.class);
    folderIndexProcessor = awsServiceCache.getExtension(FolderIndexProcessor.class);
    db = awsServiceCache.getExtension(DynamoDbService.class);
    attributeService = awsServiceCache.getExtension(AttributeService.class);

    ApiAuthorization.login(new ApiAuthorization().username("joe"));
  }

  private static void createAttributeString(final String siteId, final String attributeKey) {
    attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, attributeKey,
        AttributeDataType.STRING, AttributeType.STANDARD);
  }

  private static List<DocumentAttributeRecord> getDocumentAttributes(final String siteId,
      final DocumentArtifact document) {
    return service.findDocumentAttributes(siteId, document, null, 2).getResults();
  }

  private static List<DocumentTag> getDocumentTags(final String siteId,
      final DocumentArtifact document, final int tagCount) {
    return service.findDocumentTags(siteId, document, null, tagCount).getResults();
  }

  private static boolean hasSoftDeletedArtifactDocuments(final String siteId,
      final String documentId, final String hardDeletedArtifactId) throws Exception {
    Method method = DocumentServiceImpl.class.getDeclaredMethod("hasSoftDeletedArtifactDocuments",
        String.class, String.class, String.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(service, siteId, documentId, hardDeletedArtifactId);
  }

  private static Date makeFolderStale(final String siteId, final String path) throws IOException {
    Date staleDate = new Date(0);
    List<FolderIndexRecord> records = folderIndexProcessor.getFolderIndexRecords(siteId, path);
    FolderIndexRecord folder = records.getLast();
    folder.insertedDate(staleDate);
    folder.lastModifiedDate(staleDate);
    db.putItem(folder.getAttributes(siteId));
    return staleDate;
  }

  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  private void assertLastEvaluationKey(final Map<String, AttributeValue> map, final String gsi1Pk,
      final String gsi1Sk) {
    assertEquals("document", DynamoDbTypes.toString(map.get(SK)));
    assertEquals(gsi1Pk, DynamoDbTypes.toString(map.get(GSI1_PK)));
    assertEquals(gsi1Sk, DynamoDbTypes.toString(map.get(GSI1_SK)));
  }

  /**
   * Before Test.
   *
   */
  @BeforeEach
  public void before() {
    this.df.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * Create Document.
   *
   * @param siteId {@link String}
   * @param uuid {@link String}
   * @param date {@link ZonedDateTime}
   * @return {@link DocumentRecord}
   */
  private DocumentRecord createDocument(final String siteId, final String uuid,
      final ZonedDateTime date) {

    return new DocumentRecordBuilder().documentId(uuid).contentType("text/plain").path("test.txt")
        .userId(ID.uuid()).checksum(ID.uuid()).contentLength(2L)
        .insertedDate(Date.from(date.toInstant())).build(siteId);
  }

  private DocumentAttributeRecord createDocumentAttribute(final DocumentArtifact document) {
    return DocumentAttributeRecord.builder().userId("jsmith").document(document).key("myattr")
        .stringValue("123").valueType(DocumentAttributeValueType.STRING).build();
  }

  /**
   * Create {@link DocumentRecordSet} with child documents.
   *
   * @param siteId {@link String}
   * @param now {@link Date}
   * @return {@link DocumentRecordSet}
   */
  private DocumentRecordSet createSubDocuments(final String siteId, final Date now) {
    String username = UUID.randomUUID() + "@formkiq.com";

    var doc = new DocumentRecordBuilder().documentId(ID.uuid()).userId(username).insertedDate(now)
        .contentType("text/plain").build(siteId);

    var docRecord1 =
        new DocumentRecordBuilder().belongsToDocumentId(doc.documentId()).documentId(ID.uuid())
            .userId(username).insertedDate(now).contentType("text/html").build(siteId);
    var docTag1 = new DocumentTagRecordBuilder().document(docRecord1.document()).tagKey("category1")
        .insertedDate(now).userId(username).type(DocumentTagType.USERDEFINED).build(siteId);

    var doc1 = new DocumentRecordSet(docRecord1, null, docTag1, null);

    var docRecord2 =
        new DocumentRecordBuilder().belongsToDocumentId(doc.documentId()).documentId(ID.uuid())
            .userId(username).insertedDate(now).contentType("application/json").build(siteId);

    var docTag2 = new DocumentTagRecordBuilder().document(docRecord2.document()).tagKey("category2")
        .insertedDate(now).userId(username).type(DocumentTagType.USERDEFINED).build(siteId);
    var doc2 = new DocumentRecordSet(docRecord2, null, docTag2, null);

    return new DocumentRecordSet(doc, null, null, List.of(doc1, doc2));
  }

  /**
   * Create Test {@link DocumentRecord}.
   *
   * @param siteId DynamoDB PK Prefix
   * @return {@link List} {@link DocumentRecord}
   */
  private List<DocumentRecord> createTestData(final String siteId) {
    return createTestData(siteId, Integer.MAX_VALUE);
  }

  private List<DocumentRecord> createTestData(final String siteId, final int count) {

    List<String> dates = Arrays.asList("2020-01-30T00:00:00", "2020-01-30T01:20:00",
        "2020-01-30T02:20:00", "2020-01-30T05:20:00", "2020-01-30T11:45:00", "2020-01-30T13:22:00",
        "2020-01-30T17:02:00", "2020-01-30T19:10:00", "2020-01-30T20:54:00", "2020-01-30T23:59:59",
        "2020-01-31T00:00:00", "2020-01-31T03:00:00", "2020-01-31T05:00:00", "2020-01-31T07:00:00",
        "2020-01-31T08:00:00", "2020-01-31T09:00:00", "2020-01-31T10:00:00", "2020-01-31T11:00:00",
        "2020-01-31T23:00:00");

    List<DocumentRecord> items = new ArrayList<>();

    dates.stream().limit(count).forEach(date -> {
      ZonedDateTime zdate = DateUtil.toDateTimeFromString(date, null);
      items.add(createDocument(siteId, ID.uuid(), zdate));
    });

    items.forEach(item -> {
      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(item.document()).tagKey("status").tagValue("active")
              .insertedDate(new Date()).userId("testuser").build(siteId);
      try {
        service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
            new SaveDocumentOptions());
      } catch (ValidationException e) {
        throw new RuntimeException(e);
      }
    });

    return items;
  }

  /**
   * Add Tag Name with TAG DELIMINATOR.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testAddTags01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentRecord document = createTestData(siteId, 1).getFirst();
      String documentId = document.documentId();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);
      String tagKey = "tag" + TAG_DELIMINATOR;
      String tagValue = ID.uuid();

      List<DocumentTagRecord> tags = new DocumentTagRecordBuilder().document(documentArtifact)
          .tagKey(tagKey).tagValue(tagValue).build(siteId);

      // when
      service.addTags(siteId, documentArtifact, tags, null);

      // then
      Pagination<DocumentTag> results =
          service.findDocumentTags(siteId, documentArtifact, null, MAX_RESULTS);
      assertNull(results.getNextToken());
      assertEquals(2, results.getResults().size());
      assertEquals("status", results.getResults().getFirst().getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(0).getType());
      assertEquals("active", results.getResults().get(0).getValue());

      assertEquals(tagKey, results.getResults().get(1).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(1).getType());
      assertEquals(tagValue, results.getResults().get(1).getValue());

      assertEquals(tagValue, service.findDocumentTag(siteId, documentArtifact, tagKey).getValue());

      SearchTagCriteria s = new SearchTagCriteria(tagKey, null, null, null, null);
      SearchQuery q = new SearchQuery(null, null, null, null, s, null, null, null, null);

      Pagination<DocumentSearchResult> list =
          searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertNull(list.getNextToken());
      assertEquals(1, list.getResults().size());
      assertEquals(documentId, list.getResults().getFirst().documentRecord().documentId());
      assertEquals(tagKey, list.getResults().getFirst().matchedTag().getKey());
      assertEquals(tagValue, list.getResults().getFirst().matchedTag().getValue());
    }
  }

  /**
   * Add Tag Name only.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testAddTags02() throws ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "tag";
      Date now = new Date();
      String userId = "jsmith";

      DocumentRecord item = DocumentRecord.builder().documentId(ID.uuid()).insertedDate(now)
          .userId(userId).build(siteId);
      final String documentId = item.documentId();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);

      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(DocumentArtifact.of(documentId, null)).tagKey(tagKey)
              .tagValue(null).insertedDate(now).userId(userId).build(siteId);

      // when
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      // then
      Pagination<DocumentTag> results =
          service.findDocumentTags(siteId, documentArtifact, null, MAX_RESULTS);
      assertNull(results.getNextToken());
      assertEquals(1, results.getResults().size());
      assertEquals(tagKey, results.getResults().getFirst().getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().getFirst().getType());
      assertEquals("", results.getResults().getFirst().getValue());

      assertEquals("", service.findDocumentTag(siteId, documentArtifact, tagKey).getValue());

      SearchTagCriteria s = new SearchTagCriteria(tagKey, null, null, null, null);
      SearchQuery q = new SearchQueryBuilder().tag(s).build();

      Pagination<DocumentSearchResult> list =
          searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertNull(list.getNextToken());
      assertEquals(1, list.getResults().size());
      assertEquals(documentId, list.getResults().getFirst().documentRecord().documentId());
      assertEquals("tag", list.getResults().getFirst().matchedTag().getKey());
      assertEquals("", list.getResults().getFirst().matchedTag().getValue());
    }
  }

  /**
   * Test trying to save a "system tag".
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testAddTags03() throws ValidationException {
    // given
    String documentId = ID.uuid();
    DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);
    DocumentRecord item = DocumentRecord.builder().documentId(documentId).insertedDate(new Date())
        .userId("joe").build((String) null);
    List<DocumentTagRecord> tags = SYSTEM_DEFINED_TAGS.stream()
        .flatMap(tag -> DocumentTagRecord.builder().document(documentArtifact).tagKey(tag)
            .tagValue("A").insertedDate(new Date()).userId("joe").build((String) null).stream())
        .toList();

    // when
    service.saveDocument(null, new DocumentRecordSet(item, null, tags, null),
        new SaveDocumentOptions());

    // then
    assertEquals(0, getDocumentTags(null, documentArtifact, MAX_RESULTS).size());
  }

  /**
   * Add Tag with Values.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testAddTags04() throws ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentRecord document = createTestData(siteId, 1).getFirst();
      String documentId = document.documentId();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);
      String tagKey = "category";
      List<String> tagValues = Arrays.asList("ABC", "XYZ");

      List<DocumentTagRecord> tags = new DocumentTagRecordBuilder().document(documentArtifact)
          .tagKey(tagKey).tagValues(tagValues).build(siteId);

      // when
      service.addTags(siteId, documentArtifact, tags, null);

      // then
      final int count = 2;
      Pagination<DocumentTag> results =
          service.findDocumentTags(siteId, documentArtifact, null, MAX_RESULTS);
      assertNull(results.getNextToken());
      assertEquals(count, results.getResults().size());

      assertEquals(tagKey, results.getResults().getFirst().getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().getFirst().getType());
      assertNull(results.getResults().get(0).getValue());
      assertEquals(tagValues, results.getResults().get(0).getValues());

      assertEquals("status", results.getResults().get(1).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(1).getType());
      assertEquals("active", results.getResults().get(1).getValue());
      assertNull(results.getResults().get(1).getValues());

      assertEquals(tagValues,
          service.findDocumentTag(siteId, documentArtifact, tagKey).getValues());

      SearchTagCriteria s = new SearchTagCriteria(tagKey, null, null, null, null);
      SearchQuery q = new SearchQueryBuilder().tag(s).build();

      Pagination<DocumentSearchResult> list =
          searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertNull(list.getNextToken());
      assertEquals(1, list.getResults().size());
      assertEquals(documentId, list.getResults().getFirst().documentRecord().documentId());
      assertEquals(tagKey, list.getResults().getFirst().matchedTag().getKey());
      assertNull(list.getResults().getFirst().matchedTag().getValue());
      assertEquals(Arrays.asList("XYZ", "ABC"),
          list.getResults().getFirst().matchedTag().getValues());
    }
  }

  /**
   * Add a tag to a lot of documents.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  void testAddTags05() throws ValidationException {
    // given
    final int count = 200;
    final String tagKey = "category123";
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      Map<DocumentArtifact, Collection<DocumentTagRecord>> tagMap = new HashMap<>();

      for (int i = 0; i < count; i++) {
        String documentId = ID.uuid();
        DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);
        var tags = new DocumentTagRecordBuilder().document(documentArtifact).tagKey(tagKey)
            .tagValue("person").type(DocumentTagType.USERDEFINED).build(siteId);

        tagMap.put(documentArtifact, tags);
      }

      // when
      service.addTags(siteId, tagMap, null);

      // then
      for (DocumentArtifact document : tagMap.keySet()) {
        Pagination<DocumentTag> results =
            service.findDocumentTags(siteId, document, null, MAX_RESULTS);
        assertEquals(1, results.getResults().size());
        assertEquals(tagKey, results.getResults().getFirst().getKey());
        assertEquals("person", results.getResults().getFirst().getValue());
      }

      SearchQuery query = new SearchQueryBuilder()
          .meta(new SearchMetaCriteria(null, null, null, "tags", null)).build();
      Pagination<DocumentSearchResult> results =
          searchService.search(siteId, query, null, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      assertEquals(tagKey, results.getResults().getFirst().value());
    }
  }

  /**
   * Test add Tag with Values then change to Value.
   */
  @Test
  void testAddTags06() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = ID.uuid();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);

      List<DocumentTagRecord> tags0 = DocumentTagRecord.builder().document(documentArtifact)
          .tagKey("category").tagValues(List.of("person1", "person2"))
          .type(DocumentTagType.USERDEFINED).build(siteId);

      List<DocumentTagRecord> tags1 = DocumentTagRecord.builder().document(documentArtifact)
          .tagKey("category").tagValue("person0").type(DocumentTagType.USERDEFINED).build(siteId);
      service.saveDocument(siteId, DocumentRecord.builder().documentId(documentId)
          .insertedDate(new Date()).userId("joe").build(siteId), new SaveDocumentOptions());

      // when
      service.addTags(siteId, documentArtifact, tags0, null);
      service.addTags(siteId, documentArtifact, tags1, null);

      // then
      Pagination<DocumentTag> results =
          service.findDocumentTags(siteId, documentArtifact, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      assertEquals("category", results.getResults().getFirst().getKey());
      assertEquals("person0", results.getResults().getFirst().getValue());
    }
  }

  /**
   * Delete Document.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testDeleteDocument01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date now = new Date();
      String userId = "jsmith";

      for (boolean softDelete : Arrays.asList(Boolean.FALSE, Boolean.TRUE)) {

        DocumentRecord item = DocumentRecord.builder().documentId(ID.uuid()).insertedDate(now)
            .userId(userId).path("a/test.txt").build(siteId);
        String documentId = item.documentId();
        final DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);

        List<DocumentTagRecord> tag = DocumentTagRecord.builder()
            .document(DocumentArtifact.of(documentId, null)).tagKey("status").tagValue("active")
            .insertedDate(now).userId(ID.uuid()).build(siteId);

        service.saveDocument(siteId, new DocumentRecordSet(item, null, tag, null),
            new SaveDocumentOptions());

        Pagination<DocumentTag> results =
            service.findDocumentTags(siteId, documentArtifact, null, MAX_RESULTS);
        assertEquals(1, results.getResults().size());

        // when
        service.deleteDocument(siteId, documentArtifact, softDelete);

        // then
        assertNull(service.findDocument(siteId, documentArtifact));

        results = service.findDocumentTags(siteId, documentArtifact, null, MAX_RESULTS);
        assertEquals(0, results.getResults().size());

        SearchQuery q = new SearchQueryBuilder()
            .meta(new SearchMetaCriteria(null, "", null, null, null)).build();
        Pagination<DocumentSearchResult> folders =
            searchService.search(siteId, q, null, null, MAX_RESULTS);
        assertEquals(1, folders.getResults().size());

        q = new SearchQueryBuilder().meta(new SearchMetaCriteria(null, "a", null, null, null))
            .build();
        folders = searchService.search(siteId, q, null, null, MAX_RESULTS);

        assertEquals(0, folders.getResults().size());
      }
    }
  }

  /**
   * Delete / restore Document.
   * 
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testDeleteDocument02() throws IOException, ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      final int tagCount = 200;
      Date now = new Date();
      String userId = "jsmith";
      createAttributeString(siteId, "myattr");

      DocumentRecord item = DocumentRecord.builder().documentId(ID.uuid()).insertedDate(now)
          .userId(userId).path("a/test.txt").build(siteId);
      DocumentArtifact document = DocumentArtifact.of(item.documentId(), null);

      List<DocumentTagRecord> tags = new ArrayList<>();

      for (int i = 0; i < tagCount; i++) {
        List<DocumentTagRecord> tag = DocumentTagRecord.builder()
            .document(DocumentArtifact.of(document.documentId(), null)).tagKey("status_" + i)
            .tagValue("active").insertedDate(now).userId(ID.uuid()).build(siteId);
        tags.addAll(tag);
      }

      Collection<DocumentAttributeRecord> attrs = List.of(createDocumentAttribute(document));
      service.saveDocument(siteId, new DocumentRecordSet(item, attrs, tags, null),
          new SaveDocumentOptions());
      assertNotNull(service.findDocument(siteId, document));
      assertNull(service.findDocument(siteId, document).deletedDate());
      assertFalse(getDocumentTags(siteId, document, tagCount).isEmpty());
      assertFalse(getDocumentAttributes(siteId, document).isEmpty());

      boolean softDelete = true;

      // when
      boolean deleted = service.deleteDocument(siteId, document, softDelete);

      // then
      assertTrue(deleted);
      assertNull(service.findDocument(siteId, document));
      assertTrue(getDocumentTags(siteId, document, tagCount).isEmpty());
      assertTrue(getDocumentAttributes(siteId, document).isEmpty());

      List<DocumentRecord> results =
          service.findSoftDeletedDocuments(siteId, null, tagCount).getResults();
      assertFalse(results.isEmpty());
      assertEquals(document.documentId(), results.getFirst().documentId());
      assertNotNull(results.getFirst().deletedDate());

      // when
      boolean restored = service.restoreSoftDeletedDocument(siteId, document);

      // then
      assertTrue(restored);
      results = service.findSoftDeletedDocuments(siteId, null, tagCount).getResults();
      assertEquals(0, results.size());

      assertNotNull(service.findDocument(siteId, document));
      assertNull(service.findDocument(siteId, document).deletedDate());

      Map<String, Object> map = folderIndexProcessor.getIndex(siteId, item.path());
      assertEquals("test.txt", map.get("path"));

      assertEquals(tagCount, getDocumentTags(siteId, document, tagCount).size());
      List<DocumentAttributeRecord> documentAttributes = getDocumentAttributes(siteId, document);
      assertEquals(1, documentAttributes.size());
      assertEquals("myattr", documentAttributes.getFirst().getKey());
    }
  }

  /**
   * Delete Soft then Delete Document.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testDeleteDocument03() throws ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date now = new Date();
      String userId = "jsmith";

      DocumentRecord item = DocumentRecord.builder().documentId(ID.uuid()).insertedDate(now)
          .userId(userId).path("a/test52.txt").build(siteId);
      String documentId = item.documentId();
      final DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);

      List<DocumentTagRecord> tag = DocumentTagRecord.builder()
          .document(DocumentArtifact.of(documentId, null)).tagKey("status").tagValue("active")
          .insertedDate(now).userId(ID.uuid()).build(siteId);

      service.saveDocument(siteId, new DocumentRecordSet(item, null, tag, null),
          new SaveDocumentOptions());

      assertNotNull(service.findDocument(siteId, documentArtifact));
      assertEquals(1, getDocumentTags(siteId, documentArtifact, MAX_RESULTS).size());

      // when
      boolean softDeleted = service.deleteDocument(siteId, documentArtifact, true);

      // then
      assertTrue(softDeleted);
      assertNull(service.findDocument(siteId, documentArtifact));
      assertEquals(0, getDocumentTags(siteId, documentArtifact, MAX_RESULTS).size());

      List<DocumentRecord> results =
          service.findSoftDeletedDocuments(siteId, null, MAX_RESULTS).getResults();
      assertEquals(documentId, results.getFirst().documentId());

      // given

      // when
      boolean hardDeleted = service.deleteDocument(siteId, documentArtifact, false);

      // then
      assertTrue(hardDeleted);
      results = service.findSoftDeletedDocuments(siteId, null, MAX_RESULTS).getResults();
      assertEquals(0, results.size());

      assertNull(service.findDocument(siteId, documentArtifact));
      assertEquals(0, getDocumentTags(siteId, documentArtifact, MAX_RESULTS).size());
    }
  }

  /**
   * Delete Document when path is missing.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testDeleteDocument04() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date now = new Date();
      String userId = "jsmith";

      DocumentRecord item = DocumentRecord.builder().documentId(ID.uuid()).insertedDate(now)
          .userId(userId).path("a/test.txt").build(siteId);

      // when
      service.saveDocument(siteId, item, new SaveDocumentOptions());

      // when
      var indexRecords = new PathToFolderIndexRecords(db).apply(siteId, item.path());

      // then
      assertEquals(2, indexRecords.size());

      // given
      FolderIndexRecord ir = indexRecords.get(1);
      DynamoDbKey key = new DynamoDbKey(ir.pk(siteId), ir.sk(), null, null, null, null);

      // when
      db.deleteItem(key);
      boolean deleted =
          service.deleteDocument(siteId, DocumentArtifact.of(item.documentId(), null), false);

      // then
      assertTrue(deleted);
    }
  }

  /**
   * Test situation where the document is deleted, but still exists in path index.
   */
  @Test
  void testDeleteDocumentOnlyPath() {
    // given

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      DocumentRecord item = DocumentRecord.builder().documentId(ID.uuid()).insertedDate(new Date())
          .userId("joe").path("a/mytest333.txt").build(siteId);
      service.saveDocument(siteId, item, new SaveDocumentOptions());

      // when
      var indexRecords = new PathToFolderIndexRecords(db).apply(siteId, item.path());

      // then
      assertEquals(2, indexRecords.size());
      DocumentArtifact documentArtifact = DocumentArtifact.of(item.documentId(), null);

      // when
      boolean deleted = service.deleteDocument(siteId, documentArtifact, false);

      // then
      assertTrue(deleted);

      // given
      // Resave the folder index.
      db.putItem(indexRecords.get(1).getAttributes(siteId));
      var get = new GetFolderFilesByNameQuery(false, "mytest");

      // when
      var results = get.query(db, db.getTableName(), siteId, null, 2);

      // then
      assertEquals(1, results.items().size());

      // when
      boolean deletedAgain = service.deleteDocument(siteId, documentArtifact, false);

      // then
      assertTrue(deletedAgain);
      results = get.query(db, db.getTableName(), siteId, null, 2);
      assertEquals(0, results.items().size());
    }
  }

  /**
   * Test document exists or not.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testExists01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId0 = ID.uuid();
      String documentId1 = ID.uuid();
      DocumentRecord item0 = createDocument(siteId, documentId0, ZonedDateTime.now());

      // when
      service.saveDocument(siteId, item0, new SaveDocumentOptions());

      // then
      assertTrue(service.exists(siteId, DocumentArtifact.of(documentId0, null)));
      assertFalse(service.exists(siteId, DocumentArtifact.of(documentId1, null)));
    }
  }

  /** Find valid document. */
  @Test
  public void testFindDocument01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentRecord document = createTestData(siteId, 1).getFirst();
      String documentId = document.documentId();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);

      // when
      DocumentRecord item = service.findDocument(siteId, documentArtifact);

      // then
      assertEquals(documentId, item.documentId());
      assertNotNull(item.insertedDate());
      assertNotNull(item.lastModifiedDate());
      assertEquals(document.insertedDate(), item.insertedDate());
      assertEquals(document.insertedDate(), item.lastModifiedDate());
      assertNotNull(item.path());
      assertEquals("text/plain", item.contentType());
      assertNotNull(item.checksum());
      assertNotNull(item.userId());
      assertNotNull(item.contentLength());
    }
  }

  /**
   * Test FindDocument with child documents pagination.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testFindDocument02() throws ValidationException {
    Date now = new Date();
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final Collection<String> list = new HashSet<>();
      DocumentRecordSet doc = createSubDocuments(siteId, now);
      DocumentArtifact documentArtifact =
          DocumentArtifact.of(doc.documentRecord().documentId(), null);
      service.saveDocument(siteId, doc, new SaveDocumentOptions());

      // when
      Pagination<DocumentRecordSet> result =
          service.findDocument(siteId, documentArtifact, true, null, 1);

      // then
      DocumentRecordSet item = result.getResults().getFirst();
      assertEquals(documentArtifact.documentId(), item.documentRecord().documentId());
      Collection<DocumentRecordSet> documents = item.children();
      assertEquals(1, documents.size());

      list.add(documents.iterator().next().documentRecord().documentId());
      assertNotNull(result.getNextToken());

      // when
      result = service.findDocument(siteId, documentArtifact, true, result.getNextToken(), 1);

      // then
      item = result.getResults().getFirst();
      assertEquals(documentArtifact.documentId(), item.documentRecord().documentId());
      documents = item.children();

      list.add(documents.iterator().next().documentRecord().documentId());
      assertEquals(1, documents.size());
      assertNotNull(result.getNextToken());

      // when
      result = service.findDocument(siteId, documentArtifact, true, result.getNextToken(), 1);

      // then
      item = result.getResults().getFirst();
      assertEquals(documentArtifact.documentId(), item.documentRecord().documentId());
      documents = item.children();
      assertTrue(documents.isEmpty());
      assertNull(result.getNextToken());

      assertEquals(2, list.size());
    }
  }

  /**
   * FindDocumentAttributesByType.
   */
  @Test
  public void testFindDocumentAttributesByType01() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      createAttributeString(siteId, "key");
      attributeService.addWatermarkAttribute(siteId, "wm1",
          new Watermark("watermark1", null, null, null, null, null));
      attributeService.addWatermarkAttribute(siteId, "wm2",
          new Watermark("watermark2", null, null, null, null, null));

      String documentId = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      Collection<DocumentAttributeRecord> attributes = new ArrayList<>();
      attributes.add(new DocumentAttributeRecord().setKey("key").setDocument(document)
          .setValueType(DocumentAttributeValueType.STRING).setStringValue("13").setUserId("joe"));
      attributes.add(new DocumentAttributeRecord().setKey("wm1").setDocument(document)
          .setValueType(DocumentAttributeValueType.WATERMARK).setUserId("joe"));
      attributes.add(new DocumentAttributeRecord().setKey("wm2").setDocument(document)
          .setValueType(DocumentAttributeValueType.WATERMARK).setUserId("joe"));

      DocumentRecord item = DocumentRecord.builder().documentId(documentId).insertedDate(new Date())
          .userId("joe").build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(item, attributes, null, null),
          new SaveDocumentOptions());

      // when
      List<DocumentAttributeRecord> docAttributes =
          notNull(service.findDocumentAttributesByType(siteId, document,
              DocumentAttributeValueType.WATERMARK, null, MAX_RESULTS).getResults());

      // then
      assertEquals(2, docAttributes.size());
      assertEquals("wm1", docAttributes.get(0).getKey());
      assertEquals("wm2", docAttributes.get(1).getKey());
    }
  }

  /** Test Finding Document's Tag && Remove one. */
  @Test
  public void testFindDocumentTags01() {
    createTestData("finance", 1);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = createTestData(siteId, 1).getFirst().documentId();
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      // when
      Pagination<DocumentTag> results =
          service.findDocumentTags(siteId, document, null, MAX_RESULTS);

      // then
      assertNull(results.getNextToken());
      assertEquals(1, results.getResults().size());

      assertEquals("status", results.getResults().getFirst().getKey());
      assertEquals("active", results.getResults().getFirst().getValue());
      assertNotNull(results.getResults().getFirst().getInsertedDate());
      assertNotNull(results.getResults().getFirst().getUserId());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().getFirst().getType());

      // given
      List<String> tags = List.of("status");

      // when
      service.removeTags(siteId, document, tags);

      // then
      results = service.findDocumentTags(siteId, document, null, MAX_RESULTS);

      assertNull(results.getNextToken());
      assertEquals(0, results.getResults().size());
    }
  }

  /** Test Finding Document's particular Tag. */
  @Test
  public void testFindDocumentTags02() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "status";
      String tagValue = "active";
      String documentId = createTestData(siteId, 1).getFirst().documentId();
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      // when
      String result = service.findDocumentTag(siteId, document, tagKey).getValue();

      // then
      assertEquals(tagValue, result);
    }
  }

  /** Test Finding Document's Tag does not exist. */
  @Test
  public void testFindDocumentTags03() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "status";
      String documentId = createTestData(siteId, 1).getFirst().documentId();
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      // when
      DocumentTag result = service.findDocumentTag(siteId, document, tagKey + "!");

      // then
      assertNull(result);
    }
  }

  /** Find documents. */
  @Test
  public void testFindDocuments01() {
    createTestData("finance", 1);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Iterator<DocumentRecord> itr = createTestData(siteId, 3).iterator();
      DocumentRecord d0 = itr.next();
      DocumentRecord d1 = itr.next();
      DocumentRecord d2 = itr.next();

      List<DocumentArtifact> documents = Arrays.asList(DocumentArtifact.of(d0.documentId(), null),
          DocumentArtifact.of(d1.documentId(), null), DocumentArtifact.of(d2.documentId(), null));

      // when
      List<DocumentRecord> items = service.findDocuments(siteId, documents);

      // then
      int i = 0;
      assertEquals(items.size(), documents.size());
      assertEquals(d0.documentId(), items.get(i).documentId());
      assertNotNull(items.get(i).insertedDate());
      assertNotNull(items.get(i).lastModifiedDate());
      assertEquals(d0.insertedDate(), items.get(i++).insertedDate());

      assertEquals(d1.documentId(), items.get(i).documentId());
      assertNotNull(items.get(i).insertedDate());
      assertNotNull(items.get(i).lastModifiedDate());
      assertEquals(d1.insertedDate(), items.get(i++).insertedDate());

      assertEquals(d2.documentId(), items.get(i).documentId());
      assertNotNull(items.get(i).insertedDate());
      assertNotNull(items.get(i).lastModifiedDate());
      assertEquals(d2.insertedDate(), items.get(i).insertedDate());
    }
  }

  /** Find all documents. */
  @Test
  public void testFindDocuments02() {
    createTestData("finance", 1);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      List<DocumentArtifact> documents = createTestData(siteId).stream()
          .map(d -> DocumentArtifact.of(d.documentId(), d.artifactId())).toList();

      // when
      List<DocumentRecord> items = service.findDocuments(siteId, documents);

      // then
      assertEquals(items.size(), documents.size());
      assertNotNull(items.getFirst().documentId());
      assertNotNull(items.getFirst().insertedDate());
      assertNotNull(items.getFirst().lastModifiedDate());
    }
  }

  /**
   * Test finding 10 documents all created in one day.
   *
   */
  @Test
  public void testFindDocumentsByDate01() {
    createTestData("finance", 1);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData(siteId);

      List<String> expected = Arrays.asList("2020-01-30T00:00Z[UTC]", "2020-01-30T01:20Z[UTC]",
          "2020-01-30T02:20Z[UTC]", "2020-01-30T05:20Z[UTC]", "2020-01-30T11:45Z[UTC]",
          "2020-01-30T13:22Z[UTC]", "2020-01-30T17:02Z[UTC]", "2020-01-30T19:10Z[UTC]",
          "2020-01-30T20:54Z[UTC]", "2020-01-30T23:59:59Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-29T18:00:00", "-600");

      // when
      Pagination<DocumentRecord> results =
          service.findDocumentsByDate(siteId, date, null, MAX_RESULTS);

      // then
      assertEquals(MAX_RESULTS, results.getResults().size());
      List<String> resultDates = results.getResults().stream().map(
          r -> ZonedDateTime.ofInstant(r.insertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .toList();

      assertArrayEquals(expected.toArray(new String[0]), resultDates.toArray(new String[0]));
      assertNull(results.getNextToken());
    }
  }

  /**
   * Test paging through results.
   *
   */
  @Test
  public void testFindDocumentsByDate02() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData(siteId);

      final int max = 3;
      List<String> expected0 = Arrays.asList("2020-01-30T00:00Z[UTC]", "2020-01-30T01:20Z[UTC]",
          "2020-01-30T02:20Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-29T18:00:00", "-600");

      // when
      Pagination<DocumentRecord> results = service.findDocumentsByDate(siteId, date, null, max);

      // then
      assertEquals(max, results.getResults().size());

      List<String> resultDates = results.getResults().stream().map(
          r -> ZonedDateTime.ofInstant(r.insertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .toList();

      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      String documentId = results.getResults().getLast().documentId();
      Map<String, AttributeValue> map =
          new StringToMapAttributeValue().apply(results.getNextToken());

      String gsi1Pk = siteId != null ? siteId + "/docts#2020-01-30" : "docts#2020-01-30";
      assertLastEvaluationKey(map, gsi1Pk, "2020-01-30T02:20:00+0000#" + documentId);

      // given
      final List<String> expected1 = Arrays.asList("2020-01-30T05:20Z[UTC]",
          "2020-01-30T11:45Z[UTC]", "2020-01-30T13:22Z[UTC]");

      // when
      // Get the next page.
      results = service.findDocumentsByDate(siteId, date, results.getNextToken(), max);

      // then
      assertEquals(max, results.getResults().size());
      assertNotNull(results.getNextToken());
      resultDates = results.getResults().stream().map(
          r -> ZonedDateTime.ofInstant(r.insertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .toList();

      assertArrayEquals(expected1.toArray(new String[0]), resultDates.toArray(new String[0]));
    }
  }

  /**
   * Test paging through results over multiple days from a different TZ.
   *
   */
  @Test
  public void testFindDocumentsByDate03() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData(siteId);

      List<String> expected0 = Arrays.asList("2020-01-30T19:10Z[UTC]", "2020-01-30T20:54Z[UTC]",
          "2020-01-30T23:59:59Z[UTC]", "2020-01-31T00:00Z[UTC]", "2020-01-31T03:00Z[UTC]",
          "2020-01-31T05:00Z[UTC]", "2020-01-31T07:00Z[UTC]", "2020-01-31T08:00Z[UTC]",
          "2020-01-31T09:00Z[UTC]", "2020-01-31T10:00Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-30T12:00:00", "-600");

      // when
      Pagination<DocumentRecord> results =
          service.findDocumentsByDate(siteId, date, null, MAX_RESULTS);

      // then
      assertEquals(expected0.size(), results.getResults().size());

      List<String> resultDates = results.getResults().stream().map(
          r -> ZonedDateTime.ofInstant(r.insertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .toList();
      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      String documentId = results.getResults().getLast().documentId();

      Map<String, AttributeValue> map =
          new StringToMapAttributeValue().apply(results.getNextToken());

      String gsi1Pk = siteId != null ? siteId + "/docts#2020-01-31" : "docts#2020-01-31";
      assertLastEvaluationKey(map, gsi1Pk, "2020-01-31T10:00:00+0000#" + documentId);

      // given
      List<String> expected1 = List.of("2020-01-31T11:00Z[UTC]");

      // when
      results = service.findDocumentsByDate(siteId, date, results.getNextToken(), MAX_RESULTS);

      // then
      assertEquals(expected1.size(), results.getResults().size());
      resultDates = results.getResults().stream().map(
          r -> ZonedDateTime.ofInstant(r.insertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .toList();

      assertArrayEquals(expected1.toArray(new String[0]), resultDates.toArray(new String[0]));
      assertNull(results.getNextToken());
    }
  }

  /**
   * Test fetching 10 results over 2 days from a different TZ.
   *
   */
  @Test
  public void testFindDocumentsByDate04() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData(siteId);

      List<String> expected0 = Arrays.asList("2020-01-30T20:54Z[UTC]", "2020-01-30T23:59:59Z[UTC]",
          "2020-01-31T00:00Z[UTC]", "2020-01-31T03:00Z[UTC]", "2020-01-31T05:00Z[UTC]",
          "2020-01-31T07:00Z[UTC]", "2020-01-31T08:00Z[UTC]", "2020-01-31T09:00Z[UTC]",
          "2020-01-31T10:00Z[UTC]", "2020-01-31T11:00Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-30T14:00:00", "-600");

      // when
      Pagination<DocumentRecord> results =
          service.findDocumentsByDate(siteId, date, null, MAX_RESULTS);

      // then
      assertEquals(expected0.size(), results.getResults().size());

      List<String> resultDates = results.getResults().stream().map(
          r -> ZonedDateTime.ofInstant(r.insertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .toList();
      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      assertNull(results.getNextToken());
    }
  }

  /**
   * Find Documents by date with document that has child documents.
   *
   */
  @Test
  public void testFindDocumentsByDate05() {
    // given
    Date now = new Date();
    DocumentRecordSet documentRecordSet = createSubDocuments(null, now);
    service.saveDocument(null, documentRecordSet, new SaveDocumentOptions());
    ZonedDateTime date = service.findMostDocumentDate();
    assertNotNull(date);

    // when
    Pagination<DocumentRecord> results = service.findDocumentsByDate(null, date, null, MAX_RESULTS);

    // then
    assertEquals(3, results.getResults().size());
    String parentDocumentId = documentRecordSet.documentRecord().documentId();
    assertEquals(1, results.getResults().stream()
        .filter(document -> document.belongsToDocumentId() == null).count());
    assertEquals(2, results.getResults().stream()
        .filter(document -> parentDocumentId.equals(document.belongsToDocumentId())).count());
  }

  /**
   * Find Documents Tags.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testFindDocumentsTags01() throws ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date now = new Date();
      String userId = "jsmith";
      String documentId = ID.uuid();
      DocumentRecord document = DocumentRecord.builder().documentId(documentId).insertedDate(now)
          .userId(userId).build(siteId);
      String tagKey0 = "category";
      String tagValue0 = "person";
      String tagKey1 = "playerId";
      List<String> tagValue1 = Arrays.asList("111", "222");
      List<DocumentTagRecord> tags = new ArrayList<>();
      tags.addAll(DocumentTagRecord.builder().document(document.document()).tagKey(tagKey0)
          .tagValue(tagValue0).insertedDate(now).userId(userId).build(siteId));
      tags.addAll(DocumentTagRecord.builder().document(document.document()).tagKey(tagKey1)
          .tagValues(tagValue1).insertedDate(now).userId(userId).build(siteId));

      service.saveDocument(siteId, new DocumentRecordSet(document, null, tags, null),
          new SaveDocumentOptions());

      // when
      final Map<String, Collection<DocumentTag>> tagMap0 = service.findDocumentsTags(siteId,
          Collections.singletonList(documentId), Arrays.asList(tagKey0, tagKey1));
      final Map<String, Collection<DocumentTag>> tagMap1 = service.findDocumentsTags(siteId,
          Collections.singletonList(documentId), List.of(tagKey0));
      final Map<String, Collection<DocumentTag>> tagMap2 = service.findDocumentsTags(siteId,
          Collections.singletonList(documentId), List.of(tagKey1));

      // then
      assertEquals(1, tagMap0.size());
      Collection<DocumentTag> tags0 = tagMap0.get(documentId);
      assertEquals(2, tags0.size());

      Collection<DocumentTag> tags1 = tagMap1.get(documentId);
      assertEquals(1, tags1.size());
      DocumentTag next = tags1.iterator().next();
      assertEquals(documentId, next.getDocumentId());
      assertEquals(tagKey0, next.getKey());
      assertEquals(tagValue0, next.getValue());
      assertNull(next.getValues());

      Collection<DocumentTag> tags2 = tagMap2.get(documentId);
      assertEquals(1, tags2.size());
      next = tags2.iterator().next();
      assertEquals(documentId, next.getDocumentId());
      assertEquals(tagKey1, next.getKey());
      assertNull(next.getValue());
      assertEquals("[111, 222]", next.getValues().toString());
    }
  }

  /**
   * Find Documents more than 100 combination of Tags.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testFindDocumentsTags02() throws ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final int count = 1000;
      Date now = new Date();
      String userId = "jsmith";
      String documentId = ID.uuid();
      DocumentRecord document = DocumentRecord.builder().documentId(documentId).insertedDate(now)
          .userId(userId).build(siteId);

      String tagKey0 = "category";
      String tagValue0 = "person";
      String tagKey1 = "playerId";

      List<String> tagValue1 = Arrays.asList("111", "222");
      List<DocumentTagRecord> tags = new ArrayList<>();
      tags.addAll(DocumentTagRecord.builder().document(document.document()).tagKey(tagKey0)
          .tagValue(tagValue0).insertedDate(now).userId(userId).build(siteId));
      tags.addAll(DocumentTagRecord.builder().document(document.document()).tagKey(tagKey1)
          .tagValues(tagValue1).insertedDate(now).userId(userId).build(siteId));

      service.saveDocument(siteId, new DocumentRecordSet(document, null, tags, null),
          new SaveDocumentOptions());

      List<String> documentIds = new ArrayList<>();
      documentIds.add(documentId);
      for (int i = 0; i < count; i++) {
        documentIds.add(ID.uuid());
      }

      // when
      Map<String, Collection<DocumentTag>> tagMap =
          service.findDocumentsTags(siteId, documentIds, Arrays.asList(tagKey0, tagKey1));

      // then
      assertEquals(count + 1, tagMap.size());
      Collection<DocumentTag> tags0 = tagMap.get(documentId);
      assertEquals(2, tags0.size());
    }
  }

  /**
   * Test finding most recent documents date.
   *
   */
  @Test
  public void testFindMostDocumentDate01() {
    createTestData("finance", 1);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData(siteId);

      // when
      ZonedDateTime date = service.findMostDocumentDate();

      // then
      final int year = 2020;
      final int day = 31;
      assertEquals(year, date.getYear());
      assertEquals(Month.JANUARY, date.getMonth());
      assertEquals(day, date.getDayOfMonth());
      assertEquals(ZoneOffset.UTC, date.getZone());
    }
  }

  /**
   * Test finding most recent documents date (no data).
   *
   */
  @Test
  public void testFindMostDocumentDate02() {
    // given
    // when
    ZonedDateTime date = service.findMostDocumentDate();

    // then
    assertNull(date);
  }

  /**
   * Test Find Preset Tag.
   * 
   * @deprecated method needs to be updated
   */
  @Test
  @Deprecated
  public void testFindPresetTags01() {
    // given
    String type = "tagging";
    String presetId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      PresetTag tag = new PresetTag();

      try {
        tag.setInsertedDate(new Date());
        tag.setKey(ID.uuid());
        tag.setUserId("joe");

        service.savePreset(siteId, presetId, type, null, List.of(tag));

        // when
        Pagination<PresetTag> results = service.findPresetTags(siteId, presetId, null, MAX_RESULTS);
        Optional<PresetTag> ptag = service.findPresetTag(siteId, presetId, tag.getKey());

        // then
        assertEquals(1, results.getResults().size());
        assertTrue(ptag.isPresent());
        assertEquals(tag.getKey(), ptag.get().getKey());
        assertEquals("joe", ptag.get().getUserId());
        assertNotNull(ptag.get().getInsertedDate());

      } finally {
        service.deletePresetTag(siteId, presetId, tag.getKey());
        assertEquals(0,
            service.findPresetTags(siteId, presetId, null, MAX_RESULTS).getResults().size());
      }
    }
  }

  /**
   * Find / Save Presets.
   * 
   * @deprecated method needs to be updated
   */
  @Test
  @Deprecated
  public void testFindPresets01() {

    String type = "tagging";

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      List<String> ids = new ArrayList<>();

      try {
        // given
        final int count = 19;
        Preset preset = new Preset();

        for (int i = 0; i < count; i++) {

          String id = ID.uuid();
          ids.add(id);

          preset = new Preset();
          preset.setId(id);
          preset.setName(ID.uuid());
          preset.setType(type);
          preset.setInsertedDate(new Date());
          preset.setUserId("joe");

          service.savePreset(siteId, id, type, preset, null);
        }

        // when
        Pagination<Preset> p0 = service.findPresets(siteId, null, type, null, null, MAX_RESULTS);

        // then
        assertEquals(MAX_RESULTS, p0.getResults().size());
        assertNotNull(p0.getNextToken());

        // when
        Pagination<Preset> p1 =
            service.findPresets(siteId, preset.getId(), type, preset.getName(), null, MAX_RESULTS);

        // then
        assertEquals(1, p1.getResults().size());
        assertEquals(preset.getName(), p1.getResults().getFirst().getName());
        assertEquals(type, p1.getResults().getFirst().getType());
        assertNotNull(p1.getResults().getFirst().getInsertedDate());
        assertNotNull(p1.getResults().getFirst().getId());
        assertNull(p1.getResults().getFirst().getUserId());

        // when
        p0 = service.findPresets(siteId, null, type, null, p0.getNextToken(), MAX_RESULTS);

        // then
        assertEquals(ids.size() - MAX_RESULTS, p0.getResults().size());

        // when
        Optional<Preset> p = service.findPreset(siteId, preset.getId());

        // then
        assertTrue(p.isPresent());
        assertEquals(preset.getId(), p.get().getId());

      } finally {
        ids.forEach(id -> service.deletePreset(siteId, id));

        Pagination<Preset> p0 = service.findPresets(siteId, null, type, null, null, MAX_RESULTS);
        assertEquals(0, p0.getResults().size());
      }
    }
  }

  /**
   * Find legacy soft-deleted documents without GSI2 keys.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testFindSoftDeletedDocumentsLegacyNoGsi2() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date now = new Date();
      String userId = "jsmith";

      DocumentRecord item = DocumentRecord.builder().documentId(ID.uuid()).insertedDate(now)
          .userId(userId).build(siteId);
      DocumentArtifact document = DocumentArtifact.of(item.documentId(), null);

      service.saveDocument(siteId, item, new SaveDocumentOptions());
      assertTrue(service.deleteDocument(siteId, document, true));

      var key = new DocumentRecordBuilder().document(document).buildSoftDeleteKey(siteId);
      db.updateItem(UpdateItemRequest.builder().tableName(DOCUMENTS_TABLE)
          .key(Map.of(PK, AttributeValue.fromS(key.pk()), SK, AttributeValue.fromS(key.sk())))
          .updateExpression("REMOVE GSI2PK, GSI2SK").build());

      // when
      List<DocumentRecord> results =
          service.findSoftDeletedDocuments(siteId, null, MAX_RESULTS).getResults();
      List<DocumentRecord> rangeResults =
          service.findSoftDeletedDocuments(siteId, now, new Date(), "DESC", null, MAX_RESULTS)
              .getResults();

      // then
      assertEquals(1, results.size());
      assertEquals(document.documentId(), results.getFirst().documentId());
      assertEquals(0, rangeResults.size());
      assertTrue(service.deleteDocument(siteId, document, false));
    }
  }

  @Test
  void testGetDocumentFind() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      service.saveDocument(siteId, createDocument(siteId, documentId, ZonedDateTime.now()),
          new SaveDocumentOptions());

      // when
      var result0 = new GetDocumentFind().find(db, DOCUMENTS_TABLE, siteId, documentId);
      var result1 = new GetDocumentFind().find(db, DOCUMENTS_TABLE, siteId, ID.uuid());

      // then
      assertNotNull(result0);
      assertNull(result1);
    }
  }

  /**
   * Soft-deleted artifact check ignores the artifact being hard deleted.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHasSoftDeletedArtifactDocuments01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date now = new Date();
      String userId = "jsmith";
      String documentId = ID.uuid();
      String artifactId = ID.uuid();

      DocumentRecord documentItem = DocumentRecord.builder().documentId(documentId)
          .insertedDate(now).userId(userId).path("a/test53.txt").build(siteId);
      service.saveDocument(siteId, documentItem, new SaveDocumentOptions());

      DocumentRecord artifactItem =
          DocumentRecord.builder().documentId(documentId).insertedDate(now).userId(userId)
              .artifactId(artifactId).path("a/test53-artifact.txt").build(siteId);
      service.saveDocument(siteId, artifactItem, new SaveDocumentOptions());

      DocumentArtifact artifact = DocumentArtifact.of(documentId, artifactId);
      assertTrue(service.deleteDocument(siteId, artifact, true));

      // when
      boolean result = hasSoftDeletedArtifactDocuments(siteId, documentId, artifactId);

      // then
      assertFalse(result);
    }
  }

  /**
   * Test Remove 1 tag value from a 2 multi-value Document Tag.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testRemoveTag01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "category";
      String docid = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(docid, null);
      DocumentRecord item = DocumentRecord.builder().documentId(docid).insertedDate(new Date())
          .userId("jsmith").build(siteId);

      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(DocumentArtifact.of(docid, null)).tagKey(tagKey)
              .tagValues(Arrays.asList("abc", "xyz")).insertedDate(new Date()).userId("jsmith")
              .build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      List<DocumentTag> results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertEquals("[abc, xyz]", results.getFirst().getValues().toString());
      assertNull(results.getFirst().getValue());

      // when
      boolean removed = service.removeTag(siteId, document, tagKey, "xyz");

      // then
      assertTrue(removed);
      results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertNull(results.getFirst().getValues());
      assertEquals("abc", results.getFirst().getValue());
    }
  }

  /**
   * Test Remove 1 tag value from a 3 multi-value Document Tag.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testRemoveTag02() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "category";
      String docid = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(docid, null);
      DocumentRecord item = DocumentRecord.builder().documentId(docid).insertedDate(new Date())
          .userId("jsmith").build(siteId);

      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(DocumentArtifact.of(docid, null)).tagKey(tagKey)
              .tagValues(Arrays.asList("abc", "mno", "xyz")).insertedDate(new Date())
              .userId("jsmith").build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      List<DocumentTag> results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertEquals("[abc, mno, xyz]", results.getFirst().getValues().toString());
      assertNull(results.getFirst().getValue());

      // when
      boolean removed = service.removeTag(siteId, document, tagKey, "xyz");

      // then
      assertTrue(removed);
      results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertEquals("[abc, mno]", results.getFirst().getValues().toString());
      assertNull(results.getFirst().getValue());
    }
  }

  /**
   * Test Remove 1 tag value from a 1 multi-value Document Tag.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testRemoveTag03() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "category";
      String docid = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(docid, null);
      DocumentRecord item = DocumentRecord.builder().documentId(docid).insertedDate(new Date())
          .userId("jsmith").build(siteId);

      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(DocumentArtifact.of(docid, null)).tagKey(tagKey)
              .tagValues(List.of("xyz")).insertedDate(new Date()).userId("jsmith").build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      List<DocumentTag> results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertEquals("[xyz]", results.getFirst().getValues().toString());
      assertNull(results.getFirst().getValue());

      // when
      boolean removed = service.removeTag(siteId, document, tagKey, "xyz");

      // then
      assertTrue(removed);
      results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(0, results.size());
    }
  }

  /**
   * Test Remove tag value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testRemoveTag04() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "category";
      String tagValue = "person";
      String docid = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(docid, null);
      DocumentRecord item = DocumentRecord.builder().documentId(docid).insertedDate(new Date())
          .userId("jsmith").build(siteId);

      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(DocumentArtifact.of(docid, null)).tagKey(tagKey)
              .tagValue(tagValue).insertedDate(new Date()).userId("jsmith").build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      List<DocumentTag> results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertNull(results.getFirst().getValues());
      assertEquals(tagValue, results.getFirst().getValue());

      // when
      boolean removed = service.removeTag(siteId, document, tagKey, tagValue);

      // then
      assertTrue(removed);
      results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(0, results.size());
    }
  }

  /**
   * Test Remove wrong tag value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testRemoveTag05() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String tagKey = "category";
      String tagValue = "person";
      String docid = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(docid, null);
      DocumentRecord item = DocumentRecord.builder().documentId(docid).insertedDate(new Date())
          .userId("jsmith").build(siteId);

      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(DocumentArtifact.of(docid, null)).tagKey(tagKey)
              .tagValue(tagValue).insertedDate(new Date()).userId("jsmith").build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      List<DocumentTag> results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertNull(results.getFirst().getValues());
      assertEquals(tagValue, results.getFirst().getValue());

      // when
      boolean removed = service.removeTag(siteId, document, tagKey, tagValue + "!");

      // then
      assertFalse(removed);
      results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
    }
  }

  /**
   * Test Remove Tags from Document.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testRemoveTags01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String docid = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(docid, null);
      DocumentRecord item = DocumentRecord.builder().documentId(docid).insertedDate(new Date())
          .userId("jsmith").build(siteId);

      List<DocumentTagRecord> tags =
          DocumentTagRecord.builder().document(document).tagKey("untagged").tagValue("true")
              .insertedDate(new Date()).userId("jsmith").build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      // when
      service.removeTags(siteId, document, Collections.singletonList(tags.getFirst().tagKey()));

      // then
      assertEquals(0, getDocumentTags(siteId, document, MAX_RESULTS).size());
    }
  }

  /**
   * Test Remove 'VALUES' Tags from Document.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testRemoveTags02() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String docid = ID.uuid();
      final DocumentArtifact document = DocumentArtifact.of(docid, null);
      DocumentRecord item = DocumentRecord.builder().documentId(docid).insertedDate(new Date())
          .userId("jsmith").build(siteId);

      List<DocumentTagRecord> tag0 =
          DocumentTagRecord.builder().document(DocumentArtifact.of(docid, null)).tagKey("category")
              .tagValues(Arrays.asList("abc", "xyz")).insertedDate(new Date()).userId("jsmith")
              .build(siteId);
      List<DocumentTagRecord> tag1 =
          DocumentTagRecord.builder().document(DocumentArtifact.of(docid, null)).tagKey("category2")
              .tagValues(Arrays.asList("abc2", "xyz2")).insertedDate(new Date()).userId("jsmith")
              .build(siteId);
      List<DocumentTagRecord> tags = new ArrayList<>(tag0);
      tags.addAll(tag1);
      service.saveDocument(siteId, new DocumentRecordSet(item, null, tags, null),
          new SaveDocumentOptions());

      assertEquals(2, getDocumentTags(siteId, document, MAX_RESULTS).size());

      // when
      service.removeTags(siteId, document, Collections.singletonList(tags.getFirst().tagKey()));

      // then
      List<DocumentTag> results = getDocumentTags(siteId, document, MAX_RESULTS);
      assertEquals(1, results.size());
      assertEquals("category2", results.getFirst().getKey());
      assertEquals("[abc2, xyz2]", results.getFirst().getValues().toString());
    }
  }

  /**
   * Test Save {@link DocumentRecord} with {@link DocumentMetadata}.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithMetadata01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final String username = UUID.randomUUID() + "@formkiq.com";

      DocumentMetadata m0 = new DocumentMetadata("some", "thing", null);
      DocumentMetadata m1 = new DocumentMetadata("playerId", null, Arrays.asList("111", "222"));
      DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);

      DocumentRecord doc = DocumentRecord.builder().document(document).userId(username)
          .metadata(List.of(m0, m1)).build(siteId);

      // when
      service.saveDocument(siteId, doc, new SaveDocumentOptions());

      // then
      DocumentRecord item = service.findDocument(siteId, document);
      assertNotNull(item);
      List<DocumentMetadata> metadata = new ArrayList<>(item.metadata());
      metadata.sort(new DocumentMetadataComparator());
      assertEquals(2, metadata.size());
      assertEquals("playerId", metadata.get(0).key());
      assertEquals("[111, 222]", metadata.get(0).values().toString());
      assertEquals("some", metadata.get(1).key());
      assertEquals("thing", metadata.get(1).value());
    }
  }

  /**
   * Test Save {@link DocumentRecord} with {@link DocumentTag}.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag01() throws ValidationException {
    final int year = Calendar.getInstance().get(Calendar.YEAR);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();

      DocumentRecord documentRecord =
          new DocumentRecordBuilder().documentId(documentId).userId(username).build(siteId);

      // when
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, null, null),
          new SaveDocumentOptions());

      // then
      DocumentArtifact document = DocumentArtifact.of(documentId, null);
      DocumentRecord item = service.findDocument(siteId, document);
      assertNotNull(item);

      ZoneId timeZone = ZoneId.systemDefault();
      LocalDate lastModifiedDate =
          item.lastModifiedDate().toInstant().atZone(timeZone).toLocalDate();
      assertEquals(year, lastModifiedDate.get(ChronoField.YEAR_OF_ERA));

      Pagination<DocumentTag> tags = service.findDocumentTags(siteId, document, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());
    }
  }

  /**
   * Test Save {@link DocumentRecord} with {@link DocumentTag} with tags.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag02() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();

      DocumentRecord documentRecord =
          new DocumentRecordBuilder().documentId(documentId).userId(username).build(siteId);

      for (String tagValue : Arrays.asList("person", "thing")) {

        Collection<DocumentTagRecord> addTags = new DocumentTagRecordBuilder()
            .document(DocumentArtifact.of(documentId, null)).tagKey("category").tagValue(tagValue)
            .userId(username).type(DocumentTagType.USERDEFINED).build(siteId);

        // when
        service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, addTags, null),
            new SaveDocumentOptions());

        // then
        DocumentArtifact document = DocumentArtifact.of(documentId, null);
        DocumentRecord item = service.findDocument(siteId, document);
        assertNotNull(item);

        Pagination<DocumentTag> tags =
            service.findDocumentTags(siteId, document, null, MAX_RESULTS);
        assertEquals(1, tags.getResults().size());
        assertEquals("category", tags.getResults().getFirst().getKey());
        assertEquals(tagValue, tags.getResults().getFirst().getValue());
        assertEquals(DocumentTagType.USERDEFINED, tags.getResults().getFirst().getType());
        assertEquals(username, tags.getResults().getFirst().getUserId());
        assertEquals(item.documentId(), tags.getResults().getFirst().getDocumentId());
        assertNotNull(tags.getResults().getFirst().getInsertedDate());
      }
    }
  }

  /**
   * Test Save Document with SubDocument.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag03() throws ValidationException {
    Date now = new Date();
    ZonedDateTime nowDate = ZonedDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault());

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentRecordSet doc = createSubDocuments(siteId, now);
      String documentId = doc.documentRecord().documentId();
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      // when
      service.saveDocument(siteId, doc, new SaveDocumentOptions());

      // then
      var iter = doc.children().iterator();
      final String doc1 = iter.next().documentRecord().documentId();
      final String doc2 = iter.next().documentRecord().documentId();

      Pagination<DocumentRecordSet> result =
          service.findDocument(siteId, document, true, null, MAX_RESULTS);
      assertNull(result.getNextToken());

      DocumentRecordSet drs = result.getResults().getFirst();
      DocumentRecord ditem = drs.documentRecord();
      assertNotNull(ditem);
      assertEquals("text/plain", ditem.contentType());
      assertEquals(2, drs.children().size());

      var childrens = drs.children().stream().map(DocumentRecordSet::documentRecord)
          .sorted(Comparator.comparing(DocumentRecord::contentType)).toList();

      assertEquals(documentId, childrens.get(0).belongsToDocumentId());
      assertEquals("application/json", childrens.get(0).contentType());

      assertEquals(documentId, childrens.get(1).belongsToDocumentId());
      assertEquals("text/html", childrens.get(1).contentType());

      List<DocumentTag> tags =
          getDocumentTags(siteId, DocumentArtifact.of(ditem.documentId(), null), MAX_RESULTS);
      assertEquals(0, tags.size());

      DocumentRecord item = service.findDocument(siteId, DocumentArtifact.of(doc1, null));
      assertNotNull(item);
      assertEquals("text/html", item.contentType());
      assertEquals(documentId, item.belongsToDocumentId());

      tags = getDocumentTags(siteId, DocumentArtifact.of(item.documentId(), null), MAX_RESULTS);
      assertEquals(1, tags.size());
      assertEquals("category1", tags.getFirst().getKey());

      item = service.findDocument(siteId, DocumentArtifact.of(doc2, null));
      assertNotNull(item);
      assertEquals("application/json", item.contentType());
      assertEquals(documentId, item.belongsToDocumentId());

      tags = getDocumentTags(siteId, DocumentArtifact.of(item.documentId(), null), MAX_RESULTS);
      assertEquals(1, tags.size());
      assertEquals("category2", tags.getFirst().getKey());

      assertEquals(3,
          service.findDocumentsByDate(siteId, nowDate, null, MAX_RESULTS).getResults().size());
    }
  }

  /**
   * Test Save Document with SubDocument and tags.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag04() throws ValidationException {
    Date now = new Date();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentRecordSet doc = createSubDocuments(siteId, now);
      DocumentRecord documentRecord = doc.documentRecord();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentRecord.documentId(), null);

      Collection<DocumentTagRecord> addTags = DocumentTagRecord.builder().document(documentArtifact)
          .tagKey("category2").insertedDate(now).userId(documentRecord.userId())
          .type(DocumentTagType.USERDEFINED).build(siteId);
      doc = new DocumentRecordSet(documentRecord, doc.documentAttributeRecords(), addTags,
          doc.children());

      // when
      service.saveDocument(siteId, doc, new SaveDocumentOptions());

      // then
      Pagination<DocumentRecordSet> result =
          service.findDocument(siteId, documentArtifact, true, null, MAX_RESULTS);
      assertNull(result.getNextToken());

      DocumentRecordSet item = result.getResults().getFirst();
      assertEquals(2, item.children().size());

      List<DocumentTag> tags =
          getDocumentTags(siteId, item.documentRecord().document(), MAX_RESULTS);
      assertEquals(1, tags.size());
      assertEquals("category2", tags.getFirst().getKey());
      assertEquals("", tags.getFirst().getValue());
    }
  }

  /**
   * Test Save sSubDocument.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag05() throws ValidationException {
    Date now = new Date();
    String belongsToDocumentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentRecord documentRecord =
          new DocumentRecordBuilder().documentId(documentId).userId(username).insertedDate(now)
              .belongsToDocumentId(belongsToDocumentId).contentType("text/plain").build(siteId);

      // when
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, null, null),
          new SaveDocumentOptions());

      // then
      Pagination<DocumentRecordSet> result = service.findDocument(siteId,
          DocumentArtifact.of(documentId, null), true, null, MAX_RESULTS);
      assertNull(result.getNextToken());
      DocumentRecordSet item = result.getResults().getFirst();

      assertNotNull(item);
      assertNotNull(item.documentRecord().belongsToDocumentId());
      assertEquals("text/plain", item.documentRecord().contentType());
    }
  }

  /**
   * Test Save {@link DocumentRecord} with {@link DocumentTag} and TTL.
   * 
   * @throws URISyntaxException URISyntaxException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag06() throws URISyntaxException, ValidationException {
    // given
    String ttl = "1612058378";

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentRecord documentRecord = new DocumentRecordBuilder().documentId(documentId)
          .userId(username).timeToLive(ttl).build(siteId);

      // when
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, null, null),
          new SaveDocumentOptions());

      // then
      GetItemRequest r = GetItemRequest.builder().key(documentRecord.key().toMap())
          .tableName(DOCUMENTS_TABLE).build();

      try (DynamoDbClient dbClient = DynamoDbTestServices.getDynamoDbConnection().build()) {

        Map<String, AttributeValue> result = dbClient.getItem(r).item();
        assertEquals(ttl, result.get("TimeToLive").n());
      }
    }
  }

  /**
   * Test Save lots of tag records and duplicates.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag07() throws ValidationException {
    final int year = Calendar.getInstance().get(Calendar.YEAR);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final int numberOfTags = MAX_RESULTS * MAX_RESULTS + 1;
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);
      DocumentRecord documentRecord =
          new DocumentRecordBuilder().documentId(documentId).userId(username).build(siteId);

      Collection<DocumentTagRecord> addTags = new ArrayList<>();

      for (int j = 0; j < numberOfTags; j++) {
        for (int i = 0; i < 2; i++) { // add duplicate tags

          addTags
              .addAll(new DocumentTagRecordBuilder().document(DocumentArtifact.of(documentId, null))
                  .tagKey("category" + j).type(DocumentTagType.USERDEFINED).build(siteId));
        }
      }

      // when
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, addTags, null),
          new SaveDocumentOptions());

      // then
      DocumentRecord item = service.findDocument(siteId, documentArtifact);
      assertNotNull(item);

      ZoneId timeZone = ZoneId.systemDefault();
      LocalDate lastModifiedDate =
          item.lastModifiedDate().toInstant().atZone(timeZone).toLocalDate();
      assertEquals(year, lastModifiedDate.get(ChronoField.YEAR_OF_ERA));

      Pagination<DocumentTag> tags = service.findDocumentTags(siteId,
          DocumentArtifact.of(item.documentId(), null), null, MAX_RESULTS * MAX_RESULTS);
      assertEquals(MAX_RESULTS * MAX_RESULTS, tags.getResults().size());
    }
  }

  /**
   * Test Save 'path', 'userId' tags.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag08() throws ValidationException {
    final int year = Calendar.getInstance().get(Calendar.YEAR);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      final DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);
      DocumentRecord documentRecord = new DocumentRecordBuilder().documentId(documentId)
          .userId(username).path("test.pdf").build(siteId);

      Collection<DocumentTagRecord> addTags = new ArrayList<>();

      addTags.addAll(new DocumentTagRecordBuilder().document(DocumentArtifact.of(documentId, null))
          .tagKey("path").tagValue("test.pdf").userId(username).type(DocumentTagType.USERDEFINED)
          .build(siteId));

      addTags.addAll(new DocumentTagRecordBuilder().document(DocumentArtifact.of(documentId, null))
          .tagKey("userId").tagValue("test").userId(username).type(DocumentTagType.USERDEFINED)
          .build(siteId));

      // when
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, addTags, null),
          new SaveDocumentOptions());

      // then
      DocumentRecord item = service.findDocument(siteId, documentArtifact);
      assertNotNull(item);

      ZoneId timeZone = ZoneId.systemDefault();
      LocalDate lastModifiedDate =
          item.lastModifiedDate().toInstant().atZone(timeZone).toLocalDate();
      assertEquals(year, lastModifiedDate.get(ChronoField.YEAR_OF_ERA));

      Pagination<DocumentTag> tags = service.findDocumentTags(siteId,
          DocumentArtifact.of(item.documentId(), null), null, MAX_RESULTS);
      assertEquals(2, tags.getResults().size());
      assertEquals("path", tags.getResults().get(0).getKey());
      assertEquals("userId", tags.getResults().get(1).getKey());
    }
  }

  /**
   * Test Save case insensitive tag keys.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveDocumentItemWithTag10() throws ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentArtifact documentArtifact = DocumentArtifact.of(documentId, null);
      DocumentRecord documentRecord = new DocumentRecordBuilder().documentId(documentId)
          .userId(username).path("test.pdf").build(siteId);

      List<String> tagKeys = Arrays.asList("filetype", "fileType");

      Collection<DocumentTagRecord> addTags = new ArrayList<>();

      for (String tagKey : tagKeys) {
        addTags.addAll(new DocumentTagRecordBuilder()
            .document(DocumentArtifact.of(documentId, null)).tagKey(tagKey).tagValue("test")
            .userId(username).type(DocumentTagType.USERDEFINED).build(siteId));
      }

      // when
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, addTags, null),
          new SaveDocumentOptions());

      // then
      DocumentRecord item = service.findDocument(siteId, documentArtifact);
      assertNotNull(item);
    }
  }

  /** Test save document when site schema has no optional attributes. */
  @Test
  public void testSaveDocumentRequiredOnlySchema() throws ValidationException {
    // given
    String siteId = ID.uuid();
    String attributeKey = "myattr";
    createAttributeString(siteId, attributeKey);

    SchemaService schemaService = new SchemaServiceDynamodb(db);
    Schema schema = new Schema().attributes(new SchemaAttributes()
        .required(List.of(new SchemaAttributesRequired().attributeKey(attributeKey))));
    assertEquals(0, schemaService.setSitesSchema(siteId, "requiredOnly", schema).size());

    DocumentRecord item = createDocument(siteId, ID.uuid(), ZonedDateTime.now());
    DocumentArtifact document = DocumentArtifact.of(item.documentId(), null);
    Collection<DocumentAttributeRecord> attributes = List.of(createDocumentAttribute(document));

    // when
    service.saveDocument(siteId, new DocumentRecordSet(item, attributes, null, null),
        new SaveDocumentOptions());

    // then
    assertNotNull(service.findDocument(siteId, document));
  }

  /**
   * Test Saving / updating folders.
   * 
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSaveFolders01() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String userId0 = "joe";
      String documentId = ID.uuid();
      DocumentRecord item = DocumentRecord.builder().documentId(documentId).insertedDate(new Date())
          .userId(userId0).path("a/b/test.txt").build(siteId);
      service.saveDocument(siteId, item, new SaveDocumentOptions());

      // when
      SearchQuery q =
          new SearchQueryBuilder().meta(new SearchMetaCriteria(null, "", null, null, null)).build();

      // then
      Pagination<DocumentSearchResult> folders =
          searchService.search(siteId, q, null, null, MAX_RESULTS);

      assertEquals(1, folders.getResults().size());
      DocumentSearchResult dsr = folders.getResults().getFirst();
      DocumentRecord result = dsr.documentRecord();
      assertEquals(result.insertedDate(), result.lastModifiedDate());
      assertEquals(Boolean.TRUE, dsr.folderIndex().folder());
      assertEquals("a", result.path());
      assertEquals(userId0, result.userId());

      makeFolderStale(siteId, "a/");

      // given
      String userId1 = "frank";
      documentId = ID.uuid();
      item = DocumentRecord.builder().documentId(documentId).insertedDate(new Date())
          .userId(userId1).path("a/something.txt").build(siteId);
      service.saveDocument(siteId, item, new SaveDocumentOptions());

      // when
      q = new SearchQueryBuilder().meta(new SearchMetaCriteria(null, "", null, null, null)).build();

      // then
      folders = searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, folders.getResults().size());
      dsr = folders.getResults().getFirst();
      result = dsr.documentRecord();
      assertEquals(Boolean.TRUE, dsr.folderIndex().folder());
      assertEquals("a", result.path());
      assertEquals(userId0, result.userId());
      assertNotEquals(result.insertedDate(), result.lastModifiedDate());
    }
  }

  /**
   * Test Saving / adding folders.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSaveFolders02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String userId0 = "joe";
      String documentId = ID.uuid();
      DocumentRecord item = DocumentRecord.builder().documentId(documentId).insertedDate(new Date())
          .userId(userId0).path("a/b/test.txt").build(siteId);
      service.saveDocument(siteId, item, new SaveDocumentOptions());

      // when
      service.addFolderIndex(siteId, item.path(), item.userId());
      SearchQuery q =
          new SearchQueryBuilder().meta(new SearchMetaCriteria(null, "", null, null, null)).build();

      // then
      Pagination<DocumentSearchResult> folders =
          searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, folders.getResults().size());
      DocumentSearchResult dsr = folders.getResults().getFirst();
      DocumentRecord result = dsr.documentRecord();
      assertEquals(result.insertedDate(), result.lastModifiedDate());
      assertEquals(Boolean.TRUE, dsr.folderIndex().folder());
      assertEquals("a", result.path());
      assertEquals(userId0, result.userId());

      makeFolderStale(siteId, "a/");

      // given
      String userId1 = "frank";
      documentId = ID.uuid();
      item = DocumentRecord.builder().documentId(documentId).insertedDate(new Date())
          .userId(userId1).path("a/something.txt").build(siteId);
      service.saveDocument(siteId, item, new SaveDocumentOptions());

      // when
      q = new SearchQueryBuilder().meta(new SearchMetaCriteria(null, "", null, null, null)).build();

      // then
      folders = searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, folders.getResults().size());
      dsr = folders.getResults().getFirst();
      result = dsr.documentRecord();
      assertEquals(Boolean.TRUE, dsr.folderIndex().folder());
      assertEquals("a", result.path());
      assertEquals(userId0, result.userId());
      assertNotEquals(result.insertedDate(), result.lastModifiedDate());
    }
  }

  /**
   * Test Saving multiple files/folders.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSaveFolders03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String path = "a/b/test.txt";
      String userId0 = "joe";
      String documentId0 = ID.uuid();
      DocumentRecord item0 = DocumentRecord.builder().documentId(documentId0).insertedDate(null)
          .userId(userId0).path(path).build(siteId);
      service.saveDocument(siteId, item0, new SaveDocumentOptions());

      SearchMetaCriteria smc = new SearchMetaCriteria(null, "", null, null, null);
      SearchQuery q = new SearchQueryBuilder().meta(smc).build();
      final Date parentFolderDate = searchService.search(siteId, q, null, null, MAX_RESULTS)
          .getResults().getFirst().documentRecord().lastModifiedDate();

      final Date staleDate = makeFolderStale(siteId, "a/b/");

      String documentId1 = ID.uuid();
      DocumentRecord item1 = DocumentRecord.builder().documentId(documentId1).insertedDate(null)
          .userId(userId0).path(path).build(siteId);

      // when
      service.saveDocument(siteId, item1, new SaveDocumentOptions());

      // then
      Pagination<DocumentSearchResult> items =
          searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, items.getResults().size());
      DocumentSearchResult result = items.getResults().getFirst();
      assertEquals("a", result.documentRecord().path());
      assertEquals(parentFolderDate, result.documentRecord().lastModifiedDate());

      smc = new SearchMetaCriteria(null, "a", null, null, null);
      q = new SearchQueryBuilder().meta(smc).build();
      items = searchService.search(siteId, q, null, null, MAX_RESULTS);

      assertEquals(1, items.getResults().size());
      result = items.getResults().getFirst();
      assertEquals("b", result.documentRecord().path());
      assertNotEquals(staleDate, result.documentRecord().lastModifiedDate());

      smc = new SearchMetaCriteria(null, "a/b", null, null, null);
      q = new SearchQueryBuilder().meta(smc).build();
      items = searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(2, items.getResults().size());
      assertEquals("a/b/test (" + items.getResults().get(0).documentRecord().documentId() + ").txt",
          items.getResults().get(0).documentRecord().path());
      assertEquals("a/b/test.txt", items.getResults().get(1).documentRecord().path());
    }
  }

  /**
   * Test Saving same document multiple times.
   *
   */
  @Test
  public void testSaveFolders04() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String path = "a/test.txt";
      String userId0 = "joe";
      String documentId0 = ID.uuid();
      DocumentRecord item0 = DocumentRecord.builder().documentId(documentId0)
          .insertedDate(new Date()).userId(userId0).path(path).build(siteId);

      // when
      service.saveDocument(siteId, item0, new SaveDocumentOptions());
      service.saveDocument(siteId, item0, new SaveDocumentOptions());

      // then
      SearchMetaCriteria smc = new SearchMetaCriteria(null, "", null, null, null);
      SearchQuery q = new SearchQueryBuilder().meta(smc).build();

      Pagination<DocumentSearchResult> items =
          searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, items.getResults().size());
      DocumentSearchResult result = items.getResults().getFirst();
      assertEquals("a", result.documentRecord().path());

      smc = new SearchMetaCriteria(null, "a", null, null, null);
      q = new SearchQueryBuilder().meta(smc).build();
      items = searchService.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, items.getResults().size());
      result = items.getResults().getFirst();
      assertEquals("a/test.txt", result.documentRecord().path());
    }
  }

  /**
   * Update Document.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void updateDocument01() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(documentId, null);
      DocumentRecord item = DocumentRecord.builder().documentId(documentId).insertedDate(new Date())
          .userId("joe").path("test.pdf").build(siteId);
      service.saveDocument(siteId, item, new SaveDocumentOptions());
      Map<String, AttributeValue> newAttributes =
          Map.of("path", AttributeValue.fromS("sample.pdf"));

      // when
      service.updateDocument(siteId, document, newAttributes);
      service.updateDocument(siteId, document, newAttributes);

      // then
      assertEquals("sample.pdf", service.findDocument(siteId, document).path());
    }
  }
}
