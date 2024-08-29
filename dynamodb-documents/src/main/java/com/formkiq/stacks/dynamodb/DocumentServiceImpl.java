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

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResult;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.ReadRequestBuilder;
import com.formkiq.aws.dynamodb.WriteRequestBuilder;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidation;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidator;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidatorImpl;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.attributes.DynamicObjectToDocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.documents.DocumentPublicationRecord;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesCompositeKey;
import com.formkiq.stacks.dynamodb.schemas.SchemaMissingRequiredAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.stacks.dynamodb.schemas.SchemaServiceDynamodb;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest.Builder;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.dynamodb.objects.Strings.isUuid;
import static com.formkiq.aws.dynamodb.objects.Strings.removeQuotes;
import static com.formkiq.stacks.dynamodb.attributes.AttributeRecord.ATTR;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/** Implementation of the {@link DocumentService}. */
public class DocumentServiceImpl implements DocumentService, DbKeys {

  /** Maximum number of Records DynamoDb can be queries for at a time. */
  private static final int MAX_QUERY_RECORDS = 100;
  /** {@link AttributeService}. */
  private final AttributeService attributeService;
  /** {@link AttributeValidator}. */
  private final AttributeValidator attributeValidator;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** {@link DynamoDbService}. */
  private final DynamoDbService dbService;
  /** {@link SimpleDateFormat} in ISO Standard format. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Documents Table Name. */
  private final String documentTableName;
  /** {@link FolderIndexProcessor}. */
  private final FolderIndexProcessor folderIndexProcessor;
  /** {@link GlobalIndexService}. */
  private final GlobalIndexService indexWriter;
  /** Last Short Date. */
  private String lastShortDate = null;
  /** {@link SchemaService}. */
  private final SchemaService schemaService;
  /** {@link DocumentVersionService}. */
  private final DocumentVersionService versionsService;
  /** {@link SimpleDateFormat} YYYY-mm-dd format. */
  private final SimpleDateFormat yyyymmddFormat;
  /** {@link DateTimeFormatter}. */
  private final DateTimeFormatter yyyymmddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   * @param documentVersionsService {@link DocumentVersionService}
   */
  public DocumentServiceImpl(final DynamoDbConnectionBuilder connection,
      final String documentsTable, final DocumentVersionService documentVersionsService) {

    if (documentsTable == null) {
      throw new IllegalArgumentException("'documentsTable' is null");
    }

    this.indexWriter = new GlobalIndexService(connection, documentsTable);
    this.versionsService = documentVersionsService;
    this.dbClient = connection.build();
    this.documentTableName = documentsTable;
    this.folderIndexProcessor = new FolderIndexProcessorImpl(connection, documentsTable);
    this.dbService = new DynamoDbServiceImpl(connection, documentsTable);
    this.attributeValidator = new AttributeValidatorImpl(this.dbService);
    this.attributeService = new AttributeServiceDynamodb(this.dbService);
    this.yyyymmddFormat = new SimpleDateFormat("yyyy-MM-dd");
    this.schemaService = new SchemaServiceDynamodb(this.dbService);

    TimeZone tz = TimeZone.getTimeZone("UTC");
    this.yyyymmddFormat.setTimeZone(tz);
  }

  private Collection<DocumentAttributeRecord> addCompositeAndRequiredKeys(final String siteId,
      final String documentId, final Collection<DocumentAttributeRecord> documentAttributeRecords,
      final List<SchemaAttributes> schemaAttributes,
      final AttributeValidationAccess validationAccess) {

    Collection<DocumentAttributeRecord> documentAttributes = documentAttributeRecords;
    String userId =
        !documentAttributes.isEmpty() ? documentAttributes.iterator().next().getUserId() : "System";

    for (SchemaAttributes schemaAttribute : notNull(schemaAttributes)) {

      if (AttributeValidationAccess.ADMIN_CREATE.equals(validationAccess)
          || AttributeValidationAccess.CREATE.equals(validationAccess)) {

        Collection<DocumentAttributeRecord> missingRequired =
            addMissingRequired(schemaAttribute, siteId, documentId, documentAttributes);

        if (!missingRequired.isEmpty()) {
          documentAttributes =
              Stream.concat(documentAttributes.stream(), missingRequired.stream()).toList();
        }
      }

      Collection<DocumentAttributeRecord> compositeKeys =
          generateCompositeKeys(schemaAttribute, siteId, documentId, documentAttributes);

      documentAttributes =
          Stream.concat(documentAttributes.stream(), compositeKeys.stream()).toList();
    }

    documentAttributes.stream().filter(a -> a.getUserId() == null)
        .forEach(a -> a.setUserId(userId));

    return documentAttributes;
  }

  @Override
  public void addFolderIndex(final String siteId, final String path, final String userId) {

    Date now = new Date();

    List<FolderIndexRecordExtended> list =
        this.folderIndexProcessor.get(siteId, path, "folder", userId, now);
    List<Map<String, AttributeValue>> folderIndex =
        list.stream().filter(FolderIndexRecordExtended::isChanged)
            .map(r -> r.record().getAttributes(siteId)).collect(Collectors.toList());

    WriteRequestBuilder writeBuilder =
        new WriteRequestBuilder().appends(this.documentTableName, folderIndex);

    writeBuilder.batchWriteItem(this.dbClient);
  }

  private void addMetadata(final DocumentItem document,
      final Map<String, AttributeValue> pkvalues) {
    notNull(document.getMetadata()).forEach(m -> {
      if (!notNull(m.getValues()).isEmpty()) {
        addL(pkvalues, PREFIX_DOCUMENT_METADATA + m.getKey(), m.getValues());
      } else {
        addS(pkvalues, PREFIX_DOCUMENT_METADATA + m.getKey(), m.getValue());
      }
    });
  }

  private Collection<DocumentAttributeRecord> addMissingRequired(
      final SchemaAttributes schemaAttributes, final String siteId, final String documentId,
      final Collection<DocumentAttributeRecord> documentAttributes) {

    return new SchemaMissingRequiredAttributes(attributeService, schemaAttributes, siteId,
        documentId).apply(documentAttributes);
  }

  @Override
  public void addTags(final String siteId, final Map<String, Collection<DocumentTag>> tags,
      final String timeToLive) {

    Collection<String> tagKeys = new HashSet<>();
    List<Map<String, AttributeValue>> items = new ArrayList<>();

    for (Map.Entry<String, Collection<DocumentTag>> e : tags.entrySet()) {
      List<Map<String, AttributeValue>> attributes =
          getSaveTagsAttributes(siteId, e.getKey(), e.getValue(), timeToLive);
      items.addAll(attributes);

      tagKeys.addAll(e.getValue().stream().map(DocumentTag::getKey).toList());
    }

    if (!items.isEmpty()) {

      WriteRequestBuilder writeBuilder =
          new WriteRequestBuilder().appends(this.documentTableName, items);

      writeBuilder.batchWriteItem(this.dbClient);

      this.indexWriter.writeTagIndex(siteId, tagKeys);
    }
  }

  @Override
  public void addTags(final String siteId, final String documentId,
      final Collection<DocumentTag> tags, final String timeToLive) {
    addTags(siteId, Map.of(documentId, tags), timeToLive);
  }

  /**
   * Append Search Attributes.
   *
   * @param writeBuilder {@link WriteRequestBuilder}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param documentAttributeRecords {@link Collection} {@link DocumentAttributeRecord}
   * @param isUpdate is document update
   * @param validation {@link AttributeValidation}
   * @param validationAccess {@link AttributeValidationAccess}
   * @throws ValidationException ValidationException
   */
  private void appendDocumentAttributes(final WriteRequestBuilder writeBuilder, final String siteId,
      final String documentId, final Collection<DocumentAttributeRecord> documentAttributeRecords,
      final boolean isUpdate, final AttributeValidation validation,
      final AttributeValidationAccess validationAccess) throws ValidationException {

    if (documentAttributeRecords != null) {

      List<SchemaAttributes> schemaAttributes =
          getSchemaAttributes(siteId, documentAttributeRecords);

      Collection<DocumentAttributeRecord> documentAttributes = addCompositeAndRequiredKeys(siteId,
          documentId, documentAttributeRecords, schemaAttributes, validationAccess);

      Date date = new Date();
      documentAttributeRecords.stream().filter(a -> a.getInsertedDate() == null)
          .forEach(a -> a.setInsertedDate(date));

      validateDocumentAttributes(schemaAttributes, siteId, documentId, documentAttributes, isUpdate,
          validation, validationAccess);

      Collection<String> attributeKeys = documentAttributes.stream()
          .map(DocumentAttributeRecord::getKey).collect(Collectors.toSet());
      insertAttributeIfExists(attributeKeys, siteId, AttributeKeyReserved.RELATIONSHIPS);

      // when updating attributes remove existing attribute keys
      if (AttributeValidationAccess.ADMIN_UPDATE.equals(validationAccess)
          || AttributeValidationAccess.UPDATE.equals(validationAccess)) {

        for (String attributeKey : attributeKeys) {
          List<DocumentAttributeRecord> deletedValues = deleteDocumentAttribute(siteId, documentId,
              attributeKey, AttributeValidation.NONE, AttributeValidationAccess.NONE);

          this.versionsService.addRecords(dbClient, siteId, deletedValues);
        }
      }

      // when setting attributes remove existing attribute
      if (AttributeValidationAccess.ADMIN_SET.equals(validationAccess)
          || AttributeValidationAccess.SET.equals(validationAccess)) {

        List<DocumentAttributeRecord> deletedValues =
            deleteDocumentAttributes(siteId, documentId, validationAccess);
        this.versionsService.addRecords(dbClient, siteId, deletedValues);
      }

      writeBuilder.appends(this.documentTableName,
          documentAttributes.stream().map(a -> a.getAttributes(siteId)).toList());
    }
  }

  /**
   * Insert Attribute If doesn't exist.
   * 
   * @param attributeKeys {@link Collection}
   * @param siteId {@link String}
   * @param attributeKey {@link AttributeKeyReserved}
   */
  private void insertAttributeIfExists(final Collection<String> attributeKeys, final String siteId,
      final AttributeKeyReserved attributeKey) throws ValidationException {

    String key = attributeKey.getKey();
    if (attributeKeys.contains(key)) {

      if (!attributeService.existsAttribute(siteId, key)) {
        Collection<ValidationError> errors = attributeService.addAttribute(siteId, key,
            AttributeDataType.STRING, AttributeType.STANDARD, true);
        if (!errors.isEmpty()) {
          throw new ValidationException(errors);
        }
      }
    }
  }

  private List<SchemaAttributes> getSchemaAttributes(final String siteId,
      final Collection<DocumentAttributeRecord> documentAttributeRecords) {

    Schema schema = getSchame(siteId);

    List<Schema> schemas = documentAttributeRecords.stream()
        .filter(a -> DocumentAttributeValueType.CLASSIFICATION.equals(a.getValueType()))
        .map(a -> this.schemaService.findClassification(siteId, a.getStringValue()))
        .map(this.schemaService::getSchema)
        .map(a -> this.schemaService.mergeSchemaIntoClassification(schema, a)).toList();

    if (schema != null && notNull(schemas).isEmpty()) {
      schemas = List.of(schema);
    }

    return schemas.stream().map(Schema::getAttributes).toList();
  }

  /**
   * Build DynamoDB Search Map.
   *
   * @param siteId {@link String}
   * @param pk {@link String}
   * @param skMin {@link String}
   * @param skMax {@link String}
   * @return {@link Map}
   */
  private Map<String, String> createSearchMap(final String siteId, final String pk,
      final String skMin, final String skMax) {
    Map<String, String> map = new HashMap<>();
    map.put("pk", createDatabaseKey(siteId, pk));
    map.put("skMin", skMin);
    map.put("skMax", skMax);

    return map;
  }

  /**
   * Delete Record.
   *
   * @param key {@link Map}
   * @return {@link DeleteItemResponse}
   */
  private DeleteItemResponse delete(final Map<String, AttributeValue> key) {
    DeleteItemRequest deleteItemRequest =
        DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

    return this.dbClient.deleteItem(deleteItemRequest);
  }

  @Override
  public boolean deleteDocument(final String siteId, final String documentId,
      final boolean softDelete) {

    boolean deleted = false;

    this.versionsService.deleteAllVersionIds(this.dbClient, siteId, documentId);

    DocumentItem item = findDocument(siteId, documentId);

    deleteFolderIndex(siteId, item);

    Map<String, AttributeValue> keys = keysGeneric(siteId, PREFIX_DOCS + documentId, null);
    AttributeValue pk = keys.get(PK);
    AttributeValue sk;

    List<Map<String, AttributeValue>> list = queryDocumentAttributes(pk, null, softDelete);

    if (softDelete) {

      deleted = this.dbService.moveItems(list,
          new DocumentDeleteMoveAttributeFunction(siteId, documentId));

    } else {

      pk = fromS(SOFT_DELETE + pk.s());
      list.addAll(queryDocumentAttributes(pk, null, false));

      keys = keysGeneric(siteId, SOFT_DELETE + PREFIX_DOCS, null);
      pk = keys.get(PK);
      sk = fromS(SOFT_DELETE + "document#" + documentId);

      list.addAll(queryDocumentAttributes(pk, sk, false));

      if (this.dbService.deleteItems(list)) {
        deleted = true;
      }
    }

    return deleted;
  }

  @Override
  public List<DocumentAttributeRecord> deleteDocumentAttribute(final String siteId,
      final String documentId, final String attributeKey, final AttributeValidation validation,
      final AttributeValidationAccess validationAccess) throws ValidationException {

    if (!AttributeValidation.NONE.equals(validation)) {
      Schema schema = getSchame(siteId);
      Collection<ValidationError> errors = this.attributeValidator.validateDeleteAttribute(schema,
          siteId, attributeKey, validationAccess);
      if (!errors.isEmpty()) {
        throw new ValidationException(errors);
      }
    }

    List<DocumentAttributeRecord> documentAttributes =
        findDocumentAttribute(siteId, documentId, attributeKey);

    List<Map<String, AttributeValue>> keys = documentAttributes.stream()
        .map(a -> Map.of(PK, a.fromS(a.pk(siteId)), SK, a.fromS(a.sk()))).toList();
    this.dbService.deleteItems(keys);

    return documentAttributes;
  }

  private List<DocumentAttributeRecord> deleteDocumentAttributes(final String siteId,
      final String documentId, final AttributeValidationAccess validationAccess)
      throws ValidationException {

    final int limit = 100;
    DocumentAttributeRecord r = new DocumentAttributeRecord().setDocumentId(documentId);

    QueryConfig config = new QueryConfig();
    QueryResponse response =
        this.dbService.queryBeginsWith(config, r.fromS(r.pk(siteId)), r.fromS(ATTR), null, limit);

    List<DocumentAttributeRecord> documentAttributes = response.items().stream()
        .map(a -> new DocumentAttributeRecord().getFromAttributes(siteId, a)).toList();

    if (AttributeValidationAccess.SET.equals(validationAccess)) {

      List<String> attributeKeys = response.items().stream().map(a -> a.get("key").s()).toList();

      Map<String, AttributeRecord> attributes =
          this.attributeService.getAttributes(siteId, attributeKeys);

      Optional<AttributeRecord> o =
          attributes.values().stream().filter(a -> AttributeType.OPA.equals(a.getType())).findAny();

      if (o.isPresent()) {
        String key = o.get().getKey();
        String error = "Cannot remove attribute '" + key + "' type OPA";
        throw new ValidationException(
            Collections.singletonList(new ValidationErrorImpl().key(key).error(error)));
      }
    }

    List<Map<String, AttributeValue>> keys =
        response.items().stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();

    this.dbService.deleteItems(keys);
    return documentAttributes;
  }

  @Override
  public boolean deleteDocumentAttributeValue(final String siteId, final String documentId,
      final String attributeKey, final String attributeValue,
      final AttributeValidationAccess validationAccess) throws ValidationException {

    Schema schema = getSchame(siteId);
    Collection<ValidationError> errors = this.attributeValidator.validateDeleteAttributeValue(
        schema, siteId, attributeKey, attributeValue, validationAccess);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    DocumentAttributeRecord r =
        new DocumentAttributeRecord().setDocumentId(documentId).setKey(attributeKey)
            .setStringValue(attributeValue).setValueType(DocumentAttributeValueType.STRING);
    return this.dbService.deleteItem(Map.of(PK, r.fromS(r.pk(siteId)), SK, r.fromS(r.sk())));
  }

  @Override
  public void deleteDocumentFormat(final String siteId, final String documentId,
      final String contentType) {
    delete(keysDocumentFormats(siteId, documentId, contentType));
  }

  @Override
  public void deleteDocumentFormats(final String siteId, final String documentId) {

    PaginationMapToken startkey = null;

    do {
      PaginationResults<DocumentFormat> pr =
          findDocumentFormats(siteId, documentId, startkey, MAX_RESULTS);

      for (DocumentFormat format : pr.getResults()) {
        deleteDocumentFormat(siteId, documentId, format.getContentType());
      }

      startkey = pr.getToken();

    } while (startkey != null);
  }

  @Override
  public void deleteDocumentTag(final String siteId, final String documentId, final String tagKey) {
    deleteItem(keysDocumentTag(siteId, documentId, tagKey));
  }

  @Override
  public void deleteDocumentTags(final String siteId, final String documentId) {

    PaginationMapToken startkey = null;

    do {
      PaginationResults<DocumentTag> pr =
          findDocumentTags(siteId, documentId, startkey, MAX_RESULTS);

      for (DocumentTag tag : pr.getResults()) {
        deleteDocumentTag(siteId, documentId, tag.getKey());
      }

      startkey = pr.getToken();

    } while (startkey != null);
  }

  /**
   * Delete Folder Index.
   *
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   */
  private void deleteFolderIndex(final String siteId, final DocumentItem item) {
    if (item != null) {

      try {
        Map<String, String> attr = this.folderIndexProcessor.getIndex(siteId, item.getPath());

        if (attr.containsKey("documentId")) {
          deleteItem(Map.of(PK, AttributeValue.builder().s(attr.get(PK)).build(), SK,
              AttributeValue.builder().s(attr.get(SK)).build()));
        }
      } catch (IOException e) {
        // ignore folder doesn't exist
      }
    }
  }

  /**
   * Delete Document Row by Parition / Sort Key. param dbClient {@link DynamoDbClient}
   *
   * @param key DocumentDb Key {@link Map}
   * @return boolean
   */
  private boolean deleteItem(final Map<String, AttributeValue> key) {

    DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
        .tableName(this.documentTableName).key(key).returnValues(ReturnValue.ALL_OLD).build();

    DeleteItemResponse response = this.dbClient.deleteItem(deleteItemRequest);
    return !response.attributes().isEmpty();
  }

  @Override
  public void deletePreset(final String siteId, final String id) {
    deletePresetTags(siteId, id);
    delete(keysPreset(siteId, id));
  }

  @Override
  public void deletePresets(final String siteId, final String type) {
    PaginationMapToken startkey = null;

    do {
      PaginationResults<Preset> pr = findPresets(siteId, null, type, null, startkey, MAX_RESULTS);

      for (Preset p : pr.getResults()) {
        deletePreset(siteId, p.getId());
      }

      startkey = pr.getToken();

    } while (startkey != null);

  }

  @Override
  public void deletePresetTag(final String siteId, final String id, final String tag) {
    delete(keysPresetTag(siteId, id, tag));
  }

  @Override
  public void deletePresetTags(final String siteId, final String id) {
    PaginationMapToken startkey = null;

    do {
      PaginationResults<PresetTag> pr = findPresetTags(siteId, id, startkey, MAX_RESULTS);

      for (PresetTag tag : pr.getResults()) {
        deletePresetTag(siteId, id, tag.getKey());
      }

      startkey = pr.getToken();

    } while (startkey != null);

  }

  @Override
  public boolean deletePublishDocument(final String siteId, final String documentId) {

    DocumentPublicationRecord r0 = new DocumentPublicationRecord().setDocumentId(documentId);

    DocumentAttributeRecord r1 = new DocumentAttributeRecord().setDocumentId(documentId)
        .setKey(AttributeKeyReserved.PUBLICATION.getKey()).updateValueType();

    Map<String, AttributeValue> attributes0 = r0.getAttributes(siteId);
    Map<String, AttributeValue> attributes1 = r1.getAttributes(siteId);

    Map<String, AttributeValue> keys0 = Map.of(PK, attributes0.get(PK), SK, attributes0.get(SK));
    Map<String, AttributeValue> keys1 = Map.of(PK, attributes1.get(PK), SK, attributes1.get(SK));

    return this.dbService.deleteItems(List.of(keys0, keys1));
  }

  @Override
  public boolean exists(final String siteId, final String documentId) {
    Map<String, AttributeValue> keys = keysDocument(siteId, documentId);
    return this.dbService.exists(keys.get(PK), keys.get(SK));
  }

  /**
   * Get Record.
   *
   * @param pk {@link String}
   * @param sk {@link String}
   * @return {@link Optional} {@link Map} {@link AttributeValue}
   */
  private Optional<Map<String, AttributeValue>> find(final String pk, final String sk) {

    Map<String, AttributeValue> keyMap = keysGeneric(pk, sk);
    GetItemRequest r =
        GetItemRequest.builder().key(keyMap).tableName(this.documentTableName).build();

    Map<String, AttributeValue> result = this.dbClient.getItem(r).item();
    return result != null && !result.isEmpty() ? Optional.of(result) : Optional.empty();
  }

  /**
   * Get Records.
   *
   * @param pk {@link String}
   * @param sk {@link String}
   * @param indexName {@link String}
   * @param token {@link PaginationMapToken}
   * @param scanIndexForward {@link Boolean}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentFormat}
   */
  private PaginationResults<Map<String, AttributeValue>> find(final String pk, final String sk,
      final String indexName, final PaginationMapToken token, final Boolean scanIndexForward,
      final int maxresults) {

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);
    Map<String, AttributeValue> values = queryKeys(keysGeneric(pk, sk));

    String indexPrefix = indexName != null ? indexName : "";
    String expression = values.containsKey(":sk")
        ? indexPrefix + PK + " = :pk and begins_with(" + indexPrefix + SK + ", :sk)"
        : indexPrefix + PK + " = :pk";

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(indexName)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .scanIndexForward(scanIndexForward).limit(maxresults).exclusiveStartKey(startkey).build();

    QueryResponse result = this.dbClient.query(q);
    return new PaginationResults<>(result.items(), new QueryResponseToPagination().apply(result));
  }

  /**
   * Get Record and transform to object.
   *
   * @param <T> Type of object
   * @param keys {@link Map} {@link AttributeValue}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param func {@link Function}
   * @return {@link Optional} {@link Map} {@link AttributeValue}
   */
  private <T> PaginationResults<T> findAndTransform(final Map<String, AttributeValue> keys,
      final PaginationMapToken token, final int maxresults,
      final Function<Map<String, AttributeValue>, T> func) {
    return findAndTransform(PK, SK, keys, token, maxresults, func);
  }

  /**
   * Get Record and transform to object.
   *
   * @param <T> Type of object
   * @param pkKey {@link String}
   * @param skKey {@link String}
   * @param keys {@link Map} {@link AttributeValue}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param func {@link Function}
   * @return {@link Optional} {@link Map} {@link AttributeValue}
   */
  private <T> PaginationResults<T> findAndTransform(final String pkKey, final String skKey,
      final Map<String, AttributeValue> keys, final PaginationMapToken token, final int maxresults,
      final Function<Map<String, AttributeValue>, T> func) {
    String pk = keys.get(pkKey).s();
    String sk = keys.containsKey(skKey) ? keys.get(skKey).s() : null;
    String indexName = getIndexName(pkKey);

    PaginationResults<Map<String, AttributeValue>> results =
        find(pk, sk, indexName, token, null, maxresults);

    List<T> list = results.getResults().stream().map(func).collect(Collectors.toList());

    return new PaginationResults<>(list, results.getToken());
  }

  @Override
  public DocumentItem findDocument(final String siteId, final String documentId) {
    return findDocument(siteId, documentId, false, null, 0).getResult();
  }

  @Override
  public PaginationResult<DocumentItem> findDocument(final String siteId, final String documentId,
      final boolean includeChildDocuments, final PaginationMapToken token, final int limit) {

    DocumentItem item = null;
    PaginationMapToken pagination = null;

    GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, documentId))
        .tableName(this.documentTableName).consistentRead(Boolean.TRUE).build();

    Map<String, AttributeValue> result = this.dbClient.getItem(r).item();

    if (result != null && !result.isEmpty()) {

      item = new AttributeValueToDocumentItem().apply(result);

      if (includeChildDocuments) {

        Map<String, AttributeValue> values =
            queryKeys(keysDocument(siteId, documentId, Optional.of("")));

        Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

        QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
            .keyConditionExpression(PK + " = :pk and begins_with(" + SK + ",:sk)")
            .expressionAttributeValues(values).exclusiveStartKey(startkey).limit(limit).build();

        QueryResponse response = this.dbClient.query(q);
        List<Map<String, AttributeValue>> results = response.items();
        List<String> ids =
            results.stream().map(s -> s.get("documentId").s()).collect(Collectors.toList());

        List<DocumentItem> childDocs = findDocuments(siteId, ids);
        item.setDocuments(childDocs);

        pagination = new QueryResponseToPagination().apply(response);
      }
    }

    return new PaginationResult<>(item, pagination);
  }

  @Override
  public List<DocumentAttributeRecord> findDocumentAttribute(final String siteId,
      final String documentId, final String attributeKey) {
    final int limit = 100;
    return findDocumentAttribute(siteId, documentId, attributeKey, limit);
  }

  private List<DocumentAttributeRecord> findDocumentAttribute(final String siteId,
      final String documentId, final String attributeKey, final int limit) {

    DocumentAttributeRecord r =
        new DocumentAttributeRecord().setDocumentId(documentId).setKey(attributeKey);

    String sk = ATTR + attributeKey + "#";
    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);

    QueryResponse response =
        this.dbService.queryBeginsWith(config, r.fromS(r.pk(siteId)), r.fromS(sk), null, limit);

    return response.items().stream()
        .map(a -> new DocumentAttributeRecord().getFromAttributes(siteId, a)).toList();
  }

  @Override
  public PaginationResults<DocumentAttributeRecord> findDocumentAttributes(final String siteId,
      final String documentId, final PaginationMapToken token, final int limit) {

    DocumentAttributeRecord r = new DocumentAttributeRecord().setDocumentId(documentId);
    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);
    QueryResponse response = this.dbService.queryBeginsWith(config, r.fromS(r.pk(siteId)),
        r.fromS(AttributeRecord.ATTR), startkey, limit);

    List<DocumentAttributeRecord> list = response.items().stream()
        .map(a -> new DocumentAttributeRecord().getFromAttributes(siteId, a)).toList();

    return new PaginationResults<>(list, new QueryResponseToPagination().apply(response));
  }

  @Override
  public Optional<DocumentFormat> findDocumentFormat(final String siteId, final String documentId,
      final String contentType) {

    Map<String, AttributeValue> keyMap = keysDocumentFormats(siteId, documentId, contentType);
    Optional<Map<String, AttributeValue>> result = find(keyMap.get("PK").s(), keyMap.get("SK").s());

    AttributeValueToDocumentFormat format = new AttributeValueToDocumentFormat();
    return result.map(format);
  }

  @Override
  public PaginationResults<DocumentFormat> findDocumentFormats(final String siteId,
      final String documentId, final PaginationMapToken token, final int maxresults) {
    Map<String, AttributeValue> keys = keysDocumentFormats(siteId, documentId, null);
    return findAndTransform(keys, token, maxresults, new AttributeValueToDocumentFormat());
  }

  @Override
  public List<DocumentItem> findDocuments(final String siteId, final List<String> ids) {

    List<DocumentItem> results = Collections.emptyList();

    BatchGetConfig config = new BatchGetConfig();

    if (!ids.isEmpty()) {

      List<Map<String, AttributeValue>> keys = ids.stream()
          .map(documentId -> keysDocument(siteId, documentId)).collect(Collectors.toList());

      Collection<List<Map<String, AttributeValue>>> values = getBatch(config, keys).values();
      List<Map<String, AttributeValue>> result =
          !values.isEmpty() ? values.iterator().next() : Collections.emptyList();

      AttributeValueToDocumentItem toDocumentItem = new AttributeValueToDocumentItem();
      List<DocumentItem> items =
          result.stream().map(a -> toDocumentItem.apply(Collections.singletonList(a)))
              .collect(Collectors.toList());
      items = sortByIds(ids, items);

      if (!items.isEmpty()) {
        results = items;
      }
    }

    return results;
  }

  @Override
  public PaginationResults<DocumentItem> findDocumentsByDate(final String siteId,
      final ZonedDateTime date, final PaginationMapToken token, final int maxresults) {

    List<Map<String, String>> searchMap = generateSearchCriteria(siteId, date, token);

    PaginationResults<DocumentItem> results =
        findDocumentsBySearchMap(siteId, searchMap, token, maxresults);

    // if number of results == maxresult, check to see if next page has at least 1 record.
    if (results.getResults().size() == maxresults) {
      PaginationMapToken nextToken = results.getToken();
      searchMap = generateSearchCriteria(siteId, date, nextToken);
      PaginationResults<DocumentItem> next =
          findDocumentsBySearchMap(siteId, searchMap, nextToken, 1);

      if (next.getResults().isEmpty()) {
        results = new PaginationResults<>(results.getResults(), null);
      }
    }

    return results;
  }

  /**
   * Find Documents using the Search Map.
   *
   * @param siteId DynamoDB PK siteId
   * @param searchMap {@link List}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DocumentItem> findDocumentsBySearchMap(final String siteId,
      final List<Map<String, String>> searchMap, final PaginationMapToken token,
      final int maxresults) {

    int max = maxresults;
    PaginationMapToken itemsToken = null;
    PaginationMapToken qtoken = token;
    List<DocumentItem> items = new ArrayList<>();

    for (Map<String, String> map : searchMap) {
      String pk = map.get("pk");
      String skMin = map.get("skMin");
      String skMax = map.get("skMax");
      PaginationResults<DocumentItem> results =
          queryDocuments(siteId, pk, skMin, skMax, qtoken, max);

      items.addAll(results.getResults());
      itemsToken = results.getToken();
      max = max - results.getResults().size();

      if (max < 1) {
        break;
      }

      qtoken = null;
    }

    return new PaginationResults<>(items, itemsToken);
  }

  @Override
  public Map<String, Collection<DocumentTag>> findDocumentsTags(final String siteId,
      final Collection<String> documentIds, final List<String> tags) {

    final Map<String, Collection<DocumentTag>> tagMap = new HashMap<>();
    List<Map<String, AttributeValue>> keys = new ArrayList<>();

    documentIds.forEach(id -> {
      tagMap.put(id, new ArrayList<>());
      tags.forEach(tag -> {
        Map<String, AttributeValue> key = keysDocumentTag(siteId, id, tag);
        keys.add(key);
      });
    });

    List<List<Map<String, AttributeValue>>> paritions = Objects.parition(keys, MAX_QUERY_RECORDS);

    for (List<Map<String, AttributeValue>> partition : paritions) {

      Map<String, KeysAndAttributes> requestedItems =
          Map.of(this.documentTableName, KeysAndAttributes.builder().keys(partition).build());

      BatchGetItemRequest batchReq =
          BatchGetItemRequest.builder().requestItems(requestedItems).build();
      BatchGetItemResponse batchResponse = this.dbClient.batchGetItem(batchReq);

      Collection<List<Map<String, AttributeValue>>> values = batchResponse.responses().values();
      List<Map<String, AttributeValue>> result =
          !values.isEmpty() ? values.iterator().next() : Collections.emptyList();

      AttributeValueToDocumentTag toDocumentTag = new AttributeValueToDocumentTag(siteId);
      List<DocumentTag> list = result.stream().map(toDocumentTag).toList();

      for (DocumentTag tag : list) {
        tagMap.get(tag.getDocumentId()).add(tag);
      }
    }

    return tagMap;
  }

  @Override
  public DocumentTag findDocumentTag(final String siteId, final String documentId,
      final String tagKey) {

    DocumentTag item = null;
    QueryResponse response = findDocumentTagAttributes(siteId, documentId, tagKey, 1);
    List<Map<String, AttributeValue>> items = response.items();

    if (!items.isEmpty()) {
      item = new AttributeValueToDocumentTag(siteId).apply(items.get(0));
    }

    return item;
  }

  /**
   * Find Document Tag {@link AttributeValue}.
   *
   * @param siteId DynamoDB PK siteId
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @param maxresults {@link Integer}
   *
   * @return {@link QueryResponse}
   */
  private QueryResponse findDocumentTagAttributes(final String siteId, final String documentId,
      final String tagKey, final Integer maxresults) {

    Map<String, AttributeValue> values = queryKeys(keysDocumentTag(siteId, documentId, tagKey));

    Builder req = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression(PK + " = :pk and begins_with(" + SK + ", :sk)")
        .expressionAttributeValues(values);

    if (maxresults != null) {
      req = req.limit(maxresults);
    }

    QueryRequest q = req.build();
    return this.dbClient.query(q);
  }

  @Override
  public PaginationResults<DocumentTag> findDocumentTags(final String siteId,
      final String documentId, final PaginationMapToken token, final int limit) {

    Map<String, AttributeValue> keys = keysDocumentTag(siteId, documentId, null);

    PaginationResults<DocumentTag> tags =
        findAndTransform(keys, token, limit, new AttributeValueToDocumentTag(siteId));

    // filter duplicates
    DocumentTag prev = null;
    for (Iterator<DocumentTag> itr = tags.getResults().iterator(); itr.hasNext();) {
      DocumentTag t = itr.next();
      if (prev != null && prev.getKey().equals(t.getKey())) {
        itr.remove();
      } else {
        prev = t;
      }
    }

    return tags;
  }

  @Override
  public ZonedDateTime findMostDocumentDate() {
    ZonedDateTime date = null;
    PaginationResults<Map<String, AttributeValue>> result =
        find(PREFIX_DOCUMENT_DATE, null, null, null, Boolean.FALSE, 1);

    if (!result.getResults().isEmpty()) {
      String dateString = result.getResults().get(0).get(SK).s();
      date = DateUtil.toDateTimeFromString(dateString, null);
    }

    return date;
  }

  @Override
  public Optional<Preset> findPreset(final String siteId, final String id) {
    Map<String, AttributeValue> keyMap = keysPreset(siteId, id);
    Optional<Map<String, AttributeValue>> result = find(keyMap.get("PK").s(), keyMap.get("SK").s());

    AttributeValueToPreset format = new AttributeValueToPreset();
    return result.isPresent() ? Optional.of(format.apply(result.get())) : Optional.empty();
  }

  @Override
  public PaginationResults<Preset> findPresets(final String siteId, final String id,
      final String type, final String name, final PaginationMapToken token, final int maxresults) {
    Map<String, AttributeValue> keys = keysPresetGsi2(siteId, id, type, name);
    return findAndTransform(GSI2_PK, GSI2_SK, keys, token, maxresults,
        new AttributeValueToPreset());
  }

  @Override
  public Optional<PresetTag> findPresetTag(final String siteId, final String id,
      final String tagKey) {
    Map<String, AttributeValue> keyMap = keysPresetTag(siteId, id, tagKey);
    Optional<Map<String, AttributeValue>> result = find(keyMap.get("PK").s(), keyMap.get("SK").s());

    AttributeValueToPresetTag format = new AttributeValueToPresetTag();
    return result.isPresent() ? Optional.of(format.apply(result.get())) : Optional.empty();
  }

  @Override
  public PaginationResults<PresetTag> findPresetTags(final String siteId, final String id,
      final PaginationMapToken token, final int maxresults) {
    Map<String, AttributeValue> keys = keysPresetTag(siteId, id, null);
    return findAndTransform(keys, token, maxresults, new AttributeValueToPresetTag());
  }

  @Override
  public DocumentPublicationRecord findPublishDocument(final String siteId,
      final String documentId) {
    DocumentPublicationRecord r = new DocumentPublicationRecord().setDocumentId(documentId);
    Map<String, AttributeValue> a = this.dbService.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));

    if (!a.isEmpty()) {
      r = r.getFromAttributes(siteId, a);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public PaginationResults<DocumentItem> findSoftDeletedDocuments(final String siteId,
      final Map<String, AttributeValue> startkey, final int limit) {

    Map<String, AttributeValue> sdKeys =
        keysGeneric(siteId, SOFT_DELETE + PREFIX_DOCS, SOFT_DELETE + "document");

    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);
    QueryResponse result =
        this.dbService.queryBeginsWith(config, sdKeys.get(PK), sdKeys.get(SK), startkey, limit);

    List<DocumentItem> items = result.items().stream()
        .map(a -> new AttributeValueToDocumentItem().apply(a)).collect(Collectors.toList());
    return new PaginationResults<>(items, new QueryResponseToPagination().apply(result));
  }

  private Collection<DocumentAttributeRecord> generateCompositeKeys(
      final SchemaAttributes schemaAttributes, final String siteId, final String documentId,
      final Collection<DocumentAttributeRecord> documentAttributes) {

    Set<String> documentAttributeKeys = documentAttributes.stream()
        .map(DocumentAttributeRecord::getKey).collect(Collectors.toSet());

    List<SchemaAttributesCompositeKey> schemaCompositeKeys =
        notNull(schemaAttributes.getCompositeKeys());

    List<SchemaAttributesCompositeKey> matchingCompositeKeys =
        schemaCompositeKeys.stream().filter(sc -> {

          List<String> attributeKeys = sc.getAttributeKeys();
          for (String attributeKey : attributeKeys) {
            if (documentAttributeKeys.contains(attributeKey)) {
              return true;
            }
          }

          return false;

        }).toList();

    List<String> attributesToLoad =
        matchingCompositeKeys.stream().flatMap(c -> c.getAttributeKeys().stream())
            .filter(k -> !documentAttributeKeys.contains(k)).toList();

    Collection<DocumentAttributeRecord> existingAttributes =
        getDocumentAttribute(siteId, documentId, attributesToLoad);

    List<DocumentAttributeRecord> merged =
        Stream.concat(documentAttributes.stream(), existingAttributes.stream()).toList();

    return new DocumentAttributeSchema(schemaAttributes, documentId).apply(merged);
  }

  /**
   * Generate DynamoDB PK(s)/SK(s) to search.
   *
   * @param siteId DynamoDB PK siteId
   * @param date {@link ZonedDateTime}
   * @param token {@link PaginationMapToken}
   * @return {@link List} {@link String}
   */
  private List<Map<String, String>> generateSearchCriteria(final String siteId,
      final ZonedDateTime date, final PaginationMapToken token) {

    List<Map<String, String>> list = new ArrayList<>();

    LocalDateTime startDate = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    LocalDateTime endDate = startDate.plusDays(1);

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    String pk1 = PREFIX_DOCUMENT_DATE_TS + startDate.format(this.yyyymmddFormatter);
    String pk2 = PREFIX_DOCUMENT_DATE_TS + endDate.format(this.yyyymmddFormatter);
    boolean nextDayPagination = isNextDayPagination(siteId, pk1, startkey);

    if (!nextDayPagination) {
      String skMin = startkey != null ? startkey.get(GSI1_SK).s()
          : this.df.format(Date.from(startDate.toInstant(ZoneOffset.UTC)));
      Map<String, String> map = createSearchMap(siteId, pk1, skMin, null);
      list.add(map);
    }

    if (!pk1.equals(pk2)) {
      String skMin =
          this.df.format(Date.from(endDate.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC)));
      String skMax = this.df.format(Date.from(endDate.toInstant(ZoneOffset.UTC)));

      if (startkey != null && nextDayPagination) {
        Map<String, String> map = createSearchMap(siteId, pk2, startkey.get(GSI1_SK).s(), skMax);
        list.add(map);
      } else if (!skMin.equals(skMax)) {
        Map<String, String> map = createSearchMap(siteId, pk2, skMin, skMax);
        list.add(map);
      }
    }

    list.forEach(m -> m.put("pk", resetDatabaseKey(siteId, m.get("pk"))));

    return list;
  }

  /**
   * Get Batch Keys.
   *
   * @param keys {@link List} {@link Map} {@link AttributeValue}
   * @param config {@link BatchGetConfig}
   * @return {@link Map}
   */
  private Map<String, List<Map<String, AttributeValue>>> getBatch(final BatchGetConfig config,
      final Collection<Map<String, AttributeValue>> keys) {
    ReadRequestBuilder builder = new ReadRequestBuilder();
    builder.append(this.documentTableName, keys);
    return builder.batchReadItems(this.dbClient, config);
  }

  /**
   * Get {@link Date} or Now.
   *
   * @param date {@link Date}
   * @param defaultDate {@link Date}
   * @return {@link Date}
   */
  private Date getDateOrNow(final Date date, final Date defaultDate) {
    return date != null ? date : defaultDate;
  }

  /**
   * Get List of Document Attributes.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributeKeys {@link Collection} {@link String}
   * @return {@link Collection} {@link DocumentAttributeRecord}
   */
  private Collection<DocumentAttributeRecord> getDocumentAttribute(final String siteId,
      final String documentId, final Collection<String> attributeKeys) {

    return attributeKeys.stream()
        .flatMap(attributeKey -> findDocumentAttribute(siteId, documentId, attributeKey).stream())
        .toList();
  }

  private Collection<DocumentAttributeRecord> getDocumentAttributes(final DynamicDocumentItem doc) {
    List<DynamicObject> list = doc.getList("attributes");
    return new DynamicObjectToDocumentAttributeRecord(doc.getDocumentId(), doc.getUserId(), null)
        .apply(list);
  }

  private Date getPreviousInsertedDate(final Map<String, AttributeValue> previous) {

    Date date = null;
    AttributeValue insertedDate = previous.get("inserteddate");
    if (insertedDate != null) {
      try {
        date = this.df.parse(insertedDate.s());
      } catch (ParseException e) {
        // ignore
      }
    }

    return date;
  }

  /**
   * Save {@link DocumentItemDynamoDb}.
   *
   * @param keys {@link Map}
   * @param siteId DynamoDB PK siteId
   * @param document {@link DocumentItem}
   * @param previous {@link Map}
   * @param options {@link SaveDocumentOptions}
   * @param documentExists boolean
   * @return {@link Map}
   */
  private Map<String, AttributeValue> getSaveDocumentAttributes(
      final Map<String, AttributeValue> keys, final String siteId, final DocumentItem document,
      final Map<String, AttributeValue> previous, final SaveDocumentOptions options,
      final boolean documentExists) {

    Date previousInsertedDate = getPreviousInsertedDate(previous);

    Date insertedDate = getDateOrNow(document.getInsertedDate(), new Date());
    if (previousInsertedDate != null) {
      insertedDate = previousInsertedDate;
    }

    document.setInsertedDate(insertedDate);

    Date lastModifiedDate = getDateOrNow(document.getLastModifiedDate(), insertedDate);
    if (documentExists) {
      lastModifiedDate = new Date();
    }
    document.setLastModifiedDate(lastModifiedDate);

    String shortdate = this.yyyymmddFormat.format(insertedDate);
    String fullInsertedDate = this.df.format(insertedDate);
    String fullLastModifiedDate = this.df.format(lastModifiedDate);

    Map<String, AttributeValue> pkvalues = new HashMap<>(keys);

    if (options.saveDocumentDate()) {
      addS(pkvalues, GSI1_PK, createDatabaseKey(siteId, PREFIX_DOCUMENT_DATE_TS + shortdate));
      addS(pkvalues, GSI1_SK, fullInsertedDate + TAG_DELIMINATOR + document.getDocumentId());
    }

    addS(pkvalues, "documentId", document.getDocumentId());

    if (fullInsertedDate != null) {
      addS(pkvalues, "inserteddate", fullInsertedDate);
      addS(pkvalues, "lastModifiedDate", fullLastModifiedDate);
    }

    addS(pkvalues, "userId", document.getUserId());

    String path = isEmpty(document.getPath()) ? document.getDocumentId() : document.getPath();
    document.setPath(path);
    addS(pkvalues, "path", path);

    updateDeepLinkPath(pkvalues, document);

    addS(pkvalues, "version", document.getVersion());
    addS(pkvalues, DocumentVersionService.S3VERSION_ATTRIBUTE, document.getS3version());
    addS(pkvalues, "contentType", document.getContentType());


    if (document.getContentLength() != null) {
      addN(pkvalues, "contentLength", "" + document.getContentLength());
    }

    updateCurrentChecksumFromPrevious(document, previous);

    if (document.getChecksum() != null) {
      String etag = removeQuotes(document.getChecksum());
      addS(pkvalues, "checksum", etag);
    }

    addS(pkvalues, "belongsToDocumentId", document.getBelongsToDocumentId());

    addN(pkvalues, "TimeToLive", options.timeToLive());

    addMetadata(document, pkvalues);

    return pkvalues;
  }

  /**
   * Generate Save Tags DynamoDb Keys.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param timeToLive {@link String}
   * @return {@link List} {@link Map}
   */
  private List<Map<String, AttributeValue>> getSaveTagsAttributes(final String siteId,
      final String documentId, final Collection<DocumentTag> tags, final String timeToLive) {

    Predicate<DocumentTag> predicate = tag -> DocumentTagType.SYSTEMDEFINED.equals(tag.getType())
        || !SYSTEM_DEFINED_TAGS.contains(tag.getKey());

    DocumentTagToAttributeValueMap mapper =
        new DocumentTagToAttributeValueMap(this.df, PREFIX_DOCS, siteId, documentId);

    List<Map<String, AttributeValue>> items = notNull(tags).stream().filter(predicate).map(mapper)
        .flatMap(List::stream).collect(Collectors.toList());

    if (timeToLive != null) {
      items.forEach(v -> addN(v, "TimeToLive", timeToLive));
    }

    return items;
  }

  private Schema getSchame(final String siteId) {
    return this.schemaService.getSitesSchema(siteId);
  }

  /**
   * Has current changed from previous.
   *
   * @param previous {@link Map}
   * @param current {@link Map}
   * @return boolean
   */
  private boolean isChangedMatching(final Map<String, AttributeValue> previous,
      final Map<String, AttributeValue> current) {

    boolean changed = false;

    for (Map.Entry<String, AttributeValue> e : current.entrySet()) {

      if (!e.getKey().equals("inserteddate") && !e.getKey().equals("lastModifiedDate")
          && !e.getKey().equals("checksum")) {

        AttributeValue av = previous.get(e.getKey());
        if (av == null || !e.getValue().equals(av)) {
          changed = true;
          break;
        }
      }
    }

    return changed;
  }

  /**
   * Is {@link List} {@link DynamicObject} contain a non generated tag.
   *
   * @param tags {@link List} {@link DynamicObject}
   * @return boolean
   */
  private boolean isDocumentUserTagged(final List<DynamicObject> tags) {
    return tags != null && tags.stream().anyMatch(t -> {
      String key = t.getString("key");
      return key != null && !SYSTEM_DEFINED_TAGS.contains(key);
    });
  }

  @Override
  public boolean isFolderExists(final String siteId, final String path) {

    boolean exists = false;
    try {
      Map<String, String> map = this.folderIndexProcessor.getIndex(siteId, path);

      if ("folder".equals(map.get("type"))) {
        GetItemResponse response =
            this.dbClient.getItem(GetItemRequest.builder().tableName(this.documentTableName)
                .key(Map.of(PK, AttributeValue.builder().s(map.get(PK)).build(), SK,
                    AttributeValue.builder().s(map.get(SK)).build()))
                .build());
        exists = !response.item().isEmpty();
      }
    } catch (IOException e) {
      exists = false;
    }

    return exists;
  }

  /**
   * Checks the {@link Map} of {@link AttributeValue} if the PK in the map matches DateKey. If they
   * do NOT match and map is NOT null. Then we are pagination on the NEXT Day.
   *
   * @param siteId DynamoDB PK siteId
   * @param dateKey (yyyy-MM-dd) format
   * @param map {@link Map}
   * @return boolean
   */
  private boolean isNextDayPagination(final String siteId, final String dateKey,
      final Map<String, AttributeValue> map) {
    return map != null && !dateKey.equals(resetDatabaseKey(siteId, map.get(GSI1_PK).s()));
  }

  /**
   * Is Document Path Changed.
   *
   * @param previous {@link Map}
   * @param current {@link Map}
   * @return boolean
   */
  private boolean isPathChanges(final Map<String, AttributeValue> previous,
      final Map<String, AttributeValue> current) {
    String path0 = previous.containsKey("path") ? previous.get("path").s() : "";
    String path1 = current.containsKey("path") ? current.get("path").s() : "";
    return !path1.equals(path0) && !"".equals(path0);
  }

  @Override
  public void publishDocument(final String siteId, final String documentId, final String s3version,
      final String path, final String contentType, final String userId) {

    DocumentPublicationRecord val =
        new DocumentPublicationRecord().setPath(path).setS3version(s3version)
            .setContentType(contentType).setDocumentId(documentId).setUserId(userId);

    DocumentAttributeRecord r = new DocumentAttributeRecord().setDocumentId(documentId)
        .setKey(AttributeKeyReserved.PUBLICATION.getKey()).setUserId(userId)
        .setStringValue(documentId).setInsertedDate(new Date())
        .setValueType(DocumentAttributeValueType.PUBLICATION);

    this.dbService.putItems(List.of(r.getAttributes(siteId), val.getAttributes(siteId)));

    AttributeRecord a = new AttributeRecord().documentId(AttributeKeyReserved.PUBLICATION.getKey())
        .key(AttributeKeyReserved.PUBLICATION.getKey()).type(AttributeType.STANDARD)
        .dataType(AttributeDataType.PUBLICATION);

    if (!this.dbService.exists(a.fromS(a.pk(siteId)), a.fromS(a.sk()))) {
      this.dbService.putItem(a.getAttributes(siteId));
    }
  }

  /**
   * Query For Document Attributes.
   *
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @param softDelete boolean
   * @return {@link List} {@link Map}
   */
  private List<Map<String, AttributeValue>> queryDocumentAttributes(final AttributeValue pk,
      final AttributeValue sk, final boolean softDelete) {

    final int limit = 100;
    Map<String, AttributeValue> startkey = null;
    List<Map<String, AttributeValue>> list = new ArrayList<>();
    QueryConfig config = new QueryConfig().projectionExpression(softDelete ? null : "PK,SK");

    do {

      QueryResponse response = this.dbService.queryBeginsWith(config, pk, sk, startkey, limit);

      List<Map<String, AttributeValue>> attrs = response.items().stream().toList();
      list.addAll(attrs);

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());

    return list;

  }

  /**
   * Query Documents by Primary Key.
   *
   * @param siteId DynamoDB PK siteId
   * @param pk {@link String}
   * @param skMin {@link String}
   * @param skMax {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DocumentItem> queryDocuments(final String siteId, final String pk,
      final String skMin, final String skMax, final PaginationMapToken token,
      final int maxresults) {

    String expr = GSI1_PK + " = :pk";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.builder().s(createDatabaseKey(siteId, pk)).build());

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    if (skMax != null) {
      values.put(":sk1", AttributeValue.builder().s(skMin).build());
      values.put(":sk2", AttributeValue.builder().s(skMax).build());
      expr += " and " + GSI1_SK + " between :sk1 and :sk2";
    } else if (skMin != null) {
      values.put(":sk", AttributeValue.builder().s(skMin).build());
      expr += " and " + GSI1_SK + " >= :sk";
    }

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(GSI1)
        .keyConditionExpression(expr).expressionAttributeValues(values).limit(maxresults)
        .exclusiveStartKey(startkey).build();

    QueryResponse result = this.dbClient.query(q);

    List<DocumentItem> list = result.items().stream().map(s -> {
      String documentId = s.get("documentId").s();
      return new DocumentItemDynamoDb(documentId, null, null);
    }).collect(Collectors.toList());

    if (!list.isEmpty()) {
      List<String> documentIds =
          list.stream().map(DocumentItem::getDocumentId).collect(Collectors.toList());

      list = findDocuments(siteId, documentIds);
    }

    return new PaginationResults<>(list, new QueryResponseToPagination().apply(result));
  }

  /**
   * Remove Null Metadata.
   *
   * @param document {@link DocumentItem}
   * @param documentValues {@link Map}
   */
  private void removeNullMetadata(final DocumentItem document,
      final Map<String, AttributeValue> documentValues) {
    notNull(document.getMetadata()).stream()
        .filter(m -> m.getValues() == null && isEmpty(m.getValue())).toList()
        .forEach(m -> documentValues.remove(PREFIX_DOCUMENT_METADATA + m.getKey()));
  }

  @Override
  public boolean removeTag(final String siteId, final String documentId, final String tagKey,
      final String tagValue) {

    QueryResponse response = findDocumentTagAttributes(siteId, documentId, tagKey, null);
    List<Map<String, AttributeValue>> items = response.items();

    List<DeleteItemRequest> deletes = new ArrayList<>();
    List<PutItemRequest> puts = new ArrayList<>();

    items.forEach(i -> {

      String pk = i.get("PK").s();
      String sk = i.get("SK").s();
      String value = i.get("tagValue").s();
      Map<String, AttributeValue> key = keysGeneric(pk, sk);

      if (value.equals(tagValue)) {
        DeleteItemRequest deleteItemRequest =
            DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();
        deletes.add(deleteItemRequest);

      } else if (i.containsKey("tagValues")) {

        List<AttributeValue> avalues = i.get("tagValues").l();
        avalues =
            avalues.stream().filter(v -> !v.s().equals(tagValue)).collect(Collectors.toList());

        Map<String, AttributeValue> m = new HashMap<>(i);
        if (avalues.size() == 1) {
          m.remove("tagValues");
          m.put("tagValue", avalues.get(0));
        } else {
          m.put("tagValues", AttributeValue.builder().l(avalues).build());
        }
        puts.add(PutItemRequest.builder().tableName(this.documentTableName).item(m).build());
      }
    });

    deletes.forEach(this.dbClient::deleteItem);
    puts.forEach(this.dbClient::putItem);

    return !deletes.isEmpty();
  }

  @Override
  public void removeTags(final String siteId, final String documentId,
      final Collection<String> tags) {

    for (String tag : tags) {

      QueryResponse response = findDocumentTagAttributes(siteId, documentId, tag, null);

      List<Map<String, AttributeValue>> items = response.items();
      items = items.stream().filter(i -> tag.equals(i.get("tagKey").s())).toList();

      items.forEach(i -> {
        Map<String, AttributeValue> key = keysGeneric(i.get("PK").s(), i.get("SK").s());
        DeleteItemRequest deleteItemRequest =
            DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

        this.dbClient.deleteItem(deleteItemRequest);
      });
    }
  }

  @Override
  public boolean restoreSoftDeletedDocument(final String siteId, final String documentId) {

    boolean restored = false;

    Map<String, AttributeValue> sdKeys = keysGeneric(siteId, SOFT_DELETE + PREFIX_DOCS, null);
    AttributeValue pk = sdKeys.get(PK);
    AttributeValue sk = AttributeValue.fromS(SOFT_DELETE + "document" + "#" + documentId);

    final int limit = 100;

    Map<String, AttributeValue> attr = this.dbService.get(pk, sk);

    if (!attr.isEmpty()) {

      List<Map<String, AttributeValue>> list = new ArrayList<>();
      list.add(attr);

      Map<String, AttributeValue> startkey = null;
      QueryConfig config = new QueryConfig();

      Map<String, AttributeValue> keys = keysDocument(siteId, documentId);

      do {

        QueryResponse response = this.dbService.queryBeginsWith(config,
            fromS(SOFT_DELETE + keys.get(PK).s()), null, startkey, limit);

        List<Map<String, AttributeValue>> attrs = response.items().stream().toList();
        list.addAll(attrs);

        startkey = response.lastEvaluatedKey();

      } while (startkey != null && !startkey.isEmpty());

      restored = this.dbService.moveItems(list,
          new DocumentRestoreMoveAttributeFunction(siteId, documentId));

      String path = attr.get("path").s();
      String userId = attr.get("userId").s();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      item.setPath(path);

      List<Map<String, AttributeValue>> folderIndex =
          this.folderIndexProcessor.generateIndex(siteId, item);

      WriteRequestBuilder writeBuilder =
          new WriteRequestBuilder().appends(this.documentTableName, folderIndex);

      writeBuilder.batchWriteItem(this.dbClient);
    }

    return restored;
  }

  /**
   * Save Record.
   *
   * @param values {@link Map} {@link AttributeValue}
   * @return {@link Map} {@link AttributeValue}
   */
  private Map<String, AttributeValue> save(final Map<String, AttributeValue> values) {
    PutItemRequest put =
        PutItemRequest.builder().tableName(this.documentTableName).item(values).build();

    return this.dbClient.putItem(put).attributes();
  }

  /**
   * Save Child Documents.
   *
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @param item {@link DocumentItem}
   * @param date {@link Date}
   * @throws ValidationException ValidationException
   */
  private void saveChildDocuments(final String siteId, final DynamicDocumentItem doc,
      final DocumentItem item, final Date date) throws ValidationException {

    List<DocumentTag> tags;
    Map<String, AttributeValue> keys;
    List<DynamicObject> documents = doc.getList("documents");

    for (DynamicObject subdoc : documents) {

      if (subdoc.getDate("insertedDate") == null) {
        subdoc.put("insertedDate", date);
      }

      DocumentItem document = new DynamicDocumentItem(subdoc);
      document.setBelongsToDocumentId(item.getDocumentId());

      DocumentItem dockey = new DynamicDocumentItem(new HashMap<>());
      dockey.setDocumentId(subdoc.getString("documentId"));
      dockey.setBelongsToDocumentId(item.getDocumentId());

      // save child document
      keys =
          keysDocument(siteId, item.getDocumentId(), Optional.of(subdoc.getString("documentId")));

      SaveDocumentOptions childOptions =
          new SaveDocumentOptions().saveDocumentDate(false).timeToLive(doc.getString("TimeToLive"));
      saveDocument(keys, siteId, dockey, null, null, childOptions);

      List<DynamicObject> doctags = subdoc.getList("tags");
      tags = doctags.stream().map(t -> {
        DynamicObjectToDocumentTag transformer = new DynamicObjectToDocumentTag(this.df);
        return transformer.apply(t);
      }).collect(Collectors.toList());

      keys = keysDocument(siteId, subdoc.getString("documentId"));

      childOptions =
          new SaveDocumentOptions().saveDocumentDate(false).timeToLive(doc.getString("TimeToLive"));
      saveDocument(keys, siteId, document, tags, null, childOptions);
    }
  }

  /**
   * Save Document.
   *
   * @param keys {@link Map}
   * @param siteId {@link String}
   * @param document {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param attributes {@link Collection} {@link DocumentAttributeRecord}
   * @param options {@link SaveDocumentOptions}
   * @throws ValidationException ValidationException
   */
  private void saveDocument(final Map<String, AttributeValue> keys, final String siteId,
      final DocumentItem document, final Collection<DocumentTag> tags,
      final Collection<DocumentAttributeRecord> attributes, final SaveDocumentOptions options)
      throws ValidationException {

    updatePathFromDeepLink(document);
    validate(document);

    boolean documentExists = exists(siteId, document.getDocumentId());

    Map<String, AttributeValue> previous =
        documentExists ? new HashMap<>(this.dbService.get(keys.get(PK), keys.get(SK)))
            : Collections.emptyMap();

    Map<String, AttributeValue> documentValues = new HashMap<>(previous);
    Map<String, AttributeValue> current =
        getSaveDocumentAttributes(keys, siteId, document, previous, options, documentExists);
    documentValues.putAll(current);

    removeNullMetadata(document, documentValues);

    String documentVersionsTableName = this.versionsService.getDocumentVersionsTableName();
    boolean hasDocumentChanged = documentVersionsTableName != null && !previous.isEmpty()
        && isChangedMatching(previous, current);

    if (hasDocumentChanged) {
      this.versionsService.addDocumentVersionAttributes(previous, documentValues);
    }

    List<Map<String, AttributeValue>> folderIndex =
        this.folderIndexProcessor.generateIndex(siteId, document);
    if (!isEmpty(document.getPath())) {
      documentValues.put("path", AttributeValue.fromS(document.getPath()));
    }

    if (isPathChanges(previous, documentValues)) {
      this.folderIndexProcessor.deletePath(siteId, document.getDocumentId(),
          previous.get("path").s());
    }

    List<Map<String, AttributeValue>> tagValues =
        getSaveTagsAttributes(siteId, document.getDocumentId(), tags, options.timeToLive());

    WriteRequestBuilder writeBuilder = new WriteRequestBuilder()
        .append(this.documentTableName, documentValues).appends(this.documentTableName, tagValues)
        .appends(this.documentTableName, folderIndex);

    appendDocumentAttributes(writeBuilder, siteId, document.getDocumentId(), attributes,
        documentExists, AttributeValidation.FULL, options.getValidationAccess());

    if (hasDocumentChanged) {
      writeBuilder = writeBuilder.appends(documentVersionsTableName, List.of(previous));
    }

    if (writeBuilder.batchWriteItem(this.dbClient)) {

      List<String> tagKeys =
          notNull(tags).stream().map(t -> t.getKey()).collect(Collectors.toList());
      this.indexWriter.writeTagIndex(siteId, tagKeys);

      if (options.saveDocumentDate()) {
        saveDocumentDate(document);
      }
    }
  }

  @Override
  public void saveDocument(final String siteId, final DocumentItem document,
      final Collection<DocumentTag> tags) throws ValidationException {
    SaveDocumentOptions options = new SaveDocumentOptions().saveDocumentDate(true).timeToLive(null);
    Map<String, AttributeValue> keys = keysDocument(siteId, document.getDocumentId());
    saveDocument(keys, siteId, document, tags, null, options);
  }

  @Override
  public void saveDocument(final String siteId, final DocumentItem document,
      final Collection<DocumentTag> tags,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final SaveDocumentOptions options) throws ValidationException {

    Map<String, AttributeValue> keys = keysDocument(siteId, document.getDocumentId());
    saveDocument(keys, siteId, document, tags, documentAttributes, options);

    for (DocumentItem childDoc : notNull(document.getDocuments())) {

      DocumentItem dockey = new DynamicDocumentItem(new HashMap<>());
      dockey.setDocumentId(childDoc.getDocumentId());
      dockey.setBelongsToDocumentId(document.getDocumentId());
      childDoc.setBelongsToDocumentId(document.getDocumentId());

      // save child document
      keys = keysDocument(siteId, document.getDocumentId(), Optional.of(childDoc.getDocumentId()));

      SaveDocumentOptions childOptions =
          new SaveDocumentOptions().saveDocumentDate(false).timeToLive(childDoc.getTimeToLive());
      saveDocument(keys, siteId, dockey, null, null, childOptions);

      keys = keysDocument(siteId, childDoc.getDocumentId(), Optional.empty());
      saveDocument(keys, siteId, childDoc, null, null, childOptions);
    }
  }

  @Override
  public void saveDocumentAttributes(final String siteId, final String documentId,
      final Collection<DocumentAttributeRecord> attributes, final boolean isUpdate,
      final AttributeValidation validation, final AttributeValidationAccess validationAccess)
      throws ValidationException {

    WriteRequestBuilder writeBuilder = new WriteRequestBuilder();
    appendDocumentAttributes(writeBuilder, siteId, documentId, attributes, isUpdate, validation,
        validationAccess);

    writeBuilder.batchWriteItem(this.dbClient);
  }

  /**
   * Save Document Date record, if it already doesn't exist.
   *
   * @param document {@link DocumentItem}
   */
  private void saveDocumentDate(final DocumentItem document) {

    Date insertedDate =
        document.getInsertedDate() != null ? document.getInsertedDate() : new Date();
    String shortdate = this.yyyymmddFormat.format(insertedDate);

    if (this.lastShortDate == null || !this.lastShortDate.equals(shortdate)) {

      this.lastShortDate = shortdate;

      Map<String, AttributeValue> values =
          Map.of(PK, AttributeValue.builder().s(PREFIX_DOCUMENT_DATE).build(), SK,
              AttributeValue.builder().s(shortdate).build());
      String conditionExpression = "attribute_not_exists(" + PK + ")";
      PutItemRequest put = PutItemRequest.builder().tableName(this.documentTableName)
          .conditionExpression(conditionExpression).item(values).build();

      try {
        this.dbClient.putItem(put).attributes();
      } catch (ConditionalCheckFailedException e) {
        // Conditional Check Fails on second insert attempt
      }
    }
  }

  @Override
  public DocumentFormat saveDocumentFormat(final String siteId, final DocumentFormat format) {

    Date insertedDate = format.getInsertedDate();
    String fulldate = this.df.format(insertedDate);

    Map<String, AttributeValue> pkvalues =
        keysDocumentFormats(siteId, format.getDocumentId(), format.getContentType());

    addS(pkvalues, "documentId", format.getDocumentId());
    addS(pkvalues, "inserteddate", fulldate);
    addS(pkvalues, "contentType", format.getContentType());
    addS(pkvalues, "userId", format.getUserId());

    save(pkvalues);

    return format;
  }

  /**
   * Generate Tags for {@link DocumentTag}.
   *
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @param date {@link Date}
   * @param username {@link String}
   * @return {@link List} {@link DocumentTag}
   */
  private List<DocumentTag> saveDocumentItemGenerateTags(final String siteId,
      final DynamicDocumentItem doc, final Date date, final String username) {

    List<DynamicObject> doctags = doc.getList("tags");

    return doctags.stream().filter(t -> t.containsKey("key")).map(t -> {
      DynamicObjectToDocumentTag transform = new DynamicObjectToDocumentTag(this.df);
      DocumentTag tag = transform.apply(t);
      tag.setInsertedDate(date);
      tag.setUserId(username);
      return tag;
    }).collect(Collectors.toList());
  }

  @Override
  public DocumentItem saveDocumentItemWithTag(final String siteId, final DynamicDocumentItem doc)
      throws ValidationException {

    final Date date = new Date();
    String username = doc.getUserId();
    String documentId = resetDatabaseKey(siteId, doc.getDocumentId());

    DocumentItem item = new DocumentItemDynamoDb(documentId, null, username);

    String path = doc.getPath();

    item.setDocumentId(doc.getDocumentId());
    item.setPath(path);
    item.setDeepLinkPath(doc.getDeepLinkPath());
    item.setContentType(doc.getContentType());
    item.setChecksum(doc.getChecksum());
    item.setContentLength(doc.getContentLength());
    item.setUserId(doc.getUserId());
    item.setBelongsToDocumentId(doc.getBelongsToDocumentId());
    item.setMetadata(doc.getMetadata());

    List<DocumentTag> tags = saveDocumentItemGenerateTags(siteId, doc, date, username);
    Collection<DocumentAttributeRecord> attributes = getDocumentAttributes(doc);

    boolean saveGsi1 = doc.getBelongsToDocumentId() == null;
    SaveDocumentOptions options = new SaveDocumentOptions().saveDocumentDate(saveGsi1)
        .timeToLive(doc.getString("TimeToLive"));

    Map<String, AttributeValue> keys = keysDocument(siteId, item.getDocumentId());

    saveDocument(keys, siteId, item, tags, attributes, options);

    saveChildDocuments(siteId, doc, item, date);

    return item;
  }

  @Override
  public Preset savePreset(final String siteId, final String id, final String type,
      final Preset preset, final List<PresetTag> tags) {

    if (preset != null) {
      Date insertedDate = preset.getInsertedDate();
      String fulldate = this.df.format(insertedDate);

      Map<String, AttributeValue> pkvalues = keysPreset(siteId, preset.getId());
      addS(pkvalues, "inserteddate", fulldate);
      addS(pkvalues, "tagKey", preset.getName());
      addS(pkvalues, "type", preset.getType());
      addS(pkvalues, "userId", preset.getUserId());
      addS(pkvalues, "documentId", preset.getId());
      pkvalues.putAll(keysPresetGsi2(siteId, id, type, preset.getName()));

      save(pkvalues);
    }

    if (tags != null) {

      for (PresetTag tag : tags) {

        Date insertedDate = tag.getInsertedDate();
        String fulldate = this.df.format(insertedDate);

        Map<String, AttributeValue> pkvalues = keysPresetTag(siteId, id, tag.getKey());
        addS(pkvalues, "inserteddate", fulldate);
        addS(pkvalues, "userId", tag.getUserId());
        addS(pkvalues, "tagKey", tag.getKey());

        save(pkvalues);
      }
    }

    return preset;
  }

  /**
   * Set Last Short Date.
   *
   * @param date {@link String}
   */
  public void setLastShortDate(final String date) {
    this.lastShortDate = date;
  }

  /**
   * Sort {@link DocumentItem} to match DocumentIds {@link List}.
   *
   * @param documentIds {@link List} {@link String}
   * @param documents {@link List} {@link DocumentItem}
   * @return {@link List} {@link DocumentItem}
   */
  private List<DocumentItem> sortByIds(final List<String> documentIds,
      final List<DocumentItem> documents) {
    Map<String, DocumentItem> map = documents.stream()
        .collect(Collectors.toMap(DocumentItem::getDocumentId, Function.identity()));
    return documentIds.stream().map(map::get).filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Because Document checksum are set in the DocumentsS3Update.class, the correct checksum maybe in
   * the previous loaded document.
   *
   * @param document {@link DocumentItem}
   * @param previous {@link Map}
   */
  private void updateCurrentChecksumFromPrevious(final DocumentItem document,
      final Map<String, AttributeValue> previous) {
    AttributeValue pchecksum = previous.get("checksum");
    if (pchecksum != null && !isEmpty(pchecksum.s())) {
      String checksum = document.getChecksum();
      if (isEmpty(checksum) || isUuid(checksum)) {
        document.setChecksum(pchecksum.s());
      }
    }
  }

  private void updateDeepLinkPath(final Map<String, AttributeValue> pkvalues,
      final DocumentItem document) {

    String deepLinkPath = document.getDeepLinkPath();

    if (!isEmpty(deepLinkPath)) {
      addS(pkvalues, "deepLinkPath", deepLinkPath);

      if (isEmpty(document.getContentType())) {

        Map<String, String> googleContentTypes = Map.of("document",
            "application/vnd.google-apps.document", "spreadsheets",
            "application/vnd.google-apps.spreadsheet", "forms", "application/vnd.google-apps.form",
            "presentation", "application/vnd.google-apps.presentation");

        for (Map.Entry<String, String> e : googleContentTypes.entrySet()) {
          if (deepLinkPath.startsWith("https://docs.google.com/" + e.getKey())) {
            document.setContentType(e.getValue());
          }
        }
      }

    } else {
      addS(pkvalues, "deepLinkPath", "");
    }
  }

  @Override
  public void updateDocument(final String siteId, final String documentId,
      final Map<String, AttributeValue> attributes, final boolean updateVersioning) {

    Map<String, AttributeValue> keys = keysDocument(siteId, documentId);

    if (updateVersioning) {

      String documentVersionsTableName = this.versionsService.getDocumentVersionsTableName();
      if (documentVersionsTableName != null) {

        Map<String, AttributeValue> current =
            new HashMap<>(this.dbService.get(keys.get(PK), keys.get(SK)));
        Map<String, AttributeValue> updated = new HashMap<>(current);
        updated.putAll(attributes);

        String fullLastModifiedDate = this.df.format(new Date());
        addS(updated, "lastModifiedDate", fullLastModifiedDate);

        this.versionsService.addDocumentVersionAttributes(current, updated);

        WriteRequestBuilder writeBuilder = new WriteRequestBuilder()
            .append(this.documentTableName, updated).append(documentVersionsTableName, current);

        writeBuilder.batchWriteItem(this.dbClient);
      }
    }

    this.dbService.updateValues(keys.get(PK), keys.get(SK), attributes);
  }

  private void updatePathFromDeepLink(final DocumentItem item) {
    if (!isEmpty(item.getDeepLinkPath())) {

      if (isEmpty(item.getPath()) || item.getPath().equals(item.getDocumentId())) {

        String filename = Strings.getFilename(item.getDeepLinkPath());

        if (!isEmpty(filename)) {
          item.setPath(filename);
        }
      }
    }
  }

  /**
   * Validate {@link DocumentItem}.
   *
   * @param document {@link DocumentItem}
   */
  private void validate(final DocumentItem document) throws ValidationException {

    Collection<ValidationError> errors = Collections.emptyList();

    if (!isEmpty(document.getDeepLinkPath())) {

      if (!Strings.isUrl(document.getDeepLinkPath())) {
        errors = List.of(new ValidationErrorImpl().key("deepLinkPath")
            .error("DeepLinkPath '" + document.getDeepLinkPath() + "' is not a valid URL"));
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private Map<String, AttributeRecord> validateDocumentAttributes(
      final List<SchemaAttributes> schemaAttributes, final String siteId, final String documentId,
      final Collection<DocumentAttributeRecord> documentAttributes, final boolean isUpdate,
      final AttributeValidation validation, final AttributeValidationAccess validationAccess)
      throws ValidationException {

    Map<String, AttributeRecord> attributeRecordMap = Collections.emptyMap();
    Collection<ValidationError> errors = new ArrayList<>();

    validateDocumentAttributes(siteId, documentId, documentAttributes, validationAccess, errors);

    if (errors.isEmpty()) {

      attributeRecordMap =
          this.attributeValidator.getAttributeRecordMap(siteId, documentAttributes);

      switch (validation) {
        case FULL -> errors = this.attributeValidator.validateFullAttribute(schemaAttributes,
            siteId, documentId, documentAttributes, attributeRecordMap, isUpdate, validationAccess);
        case PARTIAL -> errors = this.attributeValidator.validatePartialAttribute(schemaAttributes,
            siteId, documentAttributes, attributeRecordMap, validationAccess);
        case NONE -> {
        }
        default -> throw new IllegalArgumentException("Unexpected value: " + validation);
      }
    }

    postValidateDocumentAttributeErrors(siteId, documentId, validationAccess, errors);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return attributeRecordMap;
  }

  /**
   * Post Processing Document Attribute Validation Errors. If updating and attribute key is missing
   * error, check to see if the attribute was added previously.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param validationAccess {@link AttributeValidationAccess}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void postValidateDocumentAttributeErrors(final String siteId, final String documentId,
      final AttributeValidationAccess validationAccess, final Collection<ValidationError> errors) {

    if (AttributeValidationAccess.ADMIN_UPDATE.equals(validationAccess)
        || AttributeValidationAccess.UPDATE.equals(validationAccess)) {

      List<ValidationError> missingRequiredErrors =
          errors.stream().filter(e -> e.error().startsWith("missing required attribute")).toList();
      for (ValidationError error : missingRequiredErrors) {

        String attributeKey = error.key();

        List<DocumentAttributeRecord> documentAttribute =
            findDocumentAttribute(siteId, documentId, attributeKey, 1);

        if (!documentAttribute.isEmpty()) {
          errors.remove(error);
        }
      }
    }
  }

  private void validateDocumentAttributes(final String siteId, final String documentId,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final AttributeValidationAccess validationAccess, final Collection<ValidationError> errors) {

    validateDocumentAttributesExist(siteId, documentId, documentAttributes, validationAccess,
        errors);
  }

  /**
   * Validate Document Attributes Exist when creating new ones.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param validationAccess {@link AttributeValidationAccess}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateDocumentAttributesExist(final String siteId, final String documentId,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final AttributeValidationAccess validationAccess, final Collection<ValidationError> errors) {

    if (AttributeValidationAccess.CREATE.equals(validationAccess)
        || AttributeValidationAccess.ADMIN_CREATE.equals(validationAccess)) {

      Set<String> keys = documentAttributes.stream().map(DocumentAttributeRecord::getKey)
          .filter(k -> !AttributeKeyReserved.RELATIONSHIPS.getKey().equals(k))
          .collect(Collectors.toSet());

      keys.forEach(key -> {

        List<DocumentAttributeRecord> documentAttribute =
            findDocumentAttribute(siteId, documentId, key, 1);

        if (!documentAttribute.isEmpty()) {
          errors.add(new ValidationErrorImpl().key(key)
              .error("document attribute '" + key + "' already exists"));
        }
      });
    }
  }
}
