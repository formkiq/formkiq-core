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
package com.formkiq.stacks.dynamodb.folders;

import static com.formkiq.aws.dynamodb.objects.Objects.last;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.dynamodb.objects.Strings.removeBackSlashes;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.AttributeValueToDynamicObject;
import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link FolderIndexProcessor} for generating Folder structure.
 *
 */
public class FolderIndexProcessorImpl implements FolderIndexProcessor, DbKeys {

  /** Lock Timeout in MS. */
  private static final long LOCK_ACQUIRE_TIMEOUT_IN_MS = 10000;
  /** Lock Expiration in MS. */
  private static final long LOCK_EXPIRATION_IN_MS = 20000;

  /**
   * Is File Token.
   * 
   * @param path {@link String}
   * @param i int
   * @param len int
   * @return boolean
   */
  private static boolean isFileToken(final String path, final int i, final int len) {
    return i >= len - 1 && !path.endsWith("/");
  }

  /**
   * Generate Path Tokens.
   * 
   * @param path {@link String}
   * @return {@link String}
   */
  private static String[] tokens(final String path) {

    String[] strs;

    if (!StringUtils.isEmpty(path) && !"/".equals(path)) {

      String p =
          path.replaceAll(":://", DELIMINATOR).replaceFirst("^\\.+", "").replaceAll("/+", "/");
      String ss = p.startsWith(DELIMINATOR) ? p.substring(DELIMINATOR.length()) : p;
      strs = ss.split(DELIMINATOR);

    } else {
      strs = new String[] {};
    }

    return strs;
  }

  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** Documents Table Name. */
  private final String documentTableName;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbClient}
   * @param documentsTable {@link String}
   */
  public FolderIndexProcessorImpl(final DynamoDbConnectionBuilder connection,
      final String documentsTable) {
    this.dbClient = connection.build();
    this.documentTableName = documentsTable;
    this.db = new DynamoDbServiceImpl(connection, documentsTable);
  }

  private void checkParentId(final FolderIndexRecord record, final String parentId) {
    if (record.parentDocumentId() == null) {
      record.parentDocumentId(parentId);
    }
  }

  /**
   * Create Folder.
   * 
   * @param siteId {@link String}
   * @param parentId {@link String}
   * @param folder {@link String}
   * @param insertedDate {@link Date}
   * @param userId {@link String}
   * @return {@link FolderIndexRecord}
   */
  private FolderIndexRecord createFolder(final String siteId, final String parentId,
      final String folder, final Date insertedDate, final String userId) {

    String uuid = ID.uuid();

    FolderIndexRecord record = new FolderIndexRecord().parentDocumentId(parentId).documentId(uuid)
        .insertedDate(insertedDate).lastModifiedDate(insertedDate).userId(userId).path(folder)
        .type("folder");

    AttributeValue pk = fromS(record.pk(siteId));
    AttributeValue sk = fromS(record.sk());
    DynamoDbKey key = new DynamoDbKey(pk.s(), sk.s(), null, null, null, null);

    Map<String, AttributeValue> attrs = this.db.get(key);
    if (!attrs.isEmpty()) {

      record = record.getFromAttributes(siteId, attrs);

    } else {

      boolean acquireLock = false;

      try {
        acquireLock = this.db.acquireLock(key, LOCK_ACQUIRE_TIMEOUT_IN_MS, LOCK_EXPIRATION_IN_MS);

        attrs = this.db.get(key);

        if (!attrs.isEmpty()) {

          record = record.getFromAttributes(siteId, attrs);

        } else {

          String conditionExpression = "attribute_not_exists(" + PK + ")";
          Put put = Put.builder().tableName(this.documentTableName)
              .conditionExpression(conditionExpression).item(record.getAttributes(siteId))
              .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
              .build();

          this.dbClient.transactWriteItems(TransactWriteItemsRequest.builder()
              .transactItems(TransactWriteItem.builder().put(put).build()).build());
        }

      } finally {

        if (acquireLock) {
          this.db.releaseLock(key);
        }
      }
    }

    return record;
  }

  private List<FolderIndexRecord> createFolderPaths(final String siteId, final String[] folders,
      final Date insertedDate, final String userId, final boolean allDirectories) {

    int i = 0;
    String parentId = "";
    int len = folders.length;

    List<FolderIndexRecord> list = new ArrayList<>();

    for (String folder : folders) {

      if (allDirectories || !isFileToken(folder, i, len)) {

        FolderIndexRecord record = createFolder(siteId, parentId, folder, insertedDate, userId);
        parentId = record.documentId();

        list.add(record);

        parentId = record.documentId();
      }

      i++;
    }

    return list;
  }

  @Override
  public List<FolderIndexRecord> createFolders(final String siteId, final String path,
      final String userId) {
    boolean allDirectories = path != null && path.endsWith("/");
    Date insertedDate = new Date();
    String[] folders = tokens(path);

    return createFolderPaths(siteId, folders, insertedDate, userId, allDirectories);
  }

  @Override
  public FolderIndexRecord addFileToFolder(final String siteId, final String documentId,
      final FolderIndexRecord parent, final String path) {

    String filename = Strings.getFilename(path);

    String parentId = parent != null ? parent.documentId() : "";

    // update last modified timestamp on folder.
    if (parent != null) {
      parent.lastModifiedDate(new Date());
      Map<String, AttributeValue> attributes = parent.getAttributes(siteId);
      Map<String, AttributeValueUpdate> values = Map.of("lastModifiedDate",
          AttributeValueUpdate.builder().value(attributes.get("lastModifiedDate")).build());
      this.db.updateItem(parent.fromS(parent.pk(siteId)), parent.fromS(parent.sk()), values);
    }

    FolderIndexRecord r = new FolderIndexRecord().parentDocumentId(parentId).documentId(documentId)
        .path(filename).type("file");

    Map<String, AttributeValue> a = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    if (!a.isEmpty() && !documentId.equals(a.get("documentId").s())) {
      String extension = Strings.getExtension(r.path());
      String newFilename =
          r.path().replaceAll("\\." + extension, " (" + documentId + ")" + "." + extension);
      r.path(newFilename);
    }

    return r;
  }

  @Override
  public boolean deleteEmptyDirectory(final String siteId, final String indexKey)
      throws IOException {
    boolean deleted = false;

    int pos = indexKey.indexOf(TAG_DELIMINATOR);
    if (pos != -1) {
      String parentId = indexKey.substring(0, pos);
      String path = indexKey.substring(pos + 1);

      deleted = deleteEmptyDirectory(siteId, parentId, path);
    }

    return deleted;
  }

  @Override
  public boolean deleteEmptyDirectory(final String siteId, final String parentId, final String path)
      throws IOException {

    boolean deleted = false;
    String pk = getPk(siteId, parentId);
    String sk = getSk(path, false);

    Map<String, AttributeValue> attr = this.db.get(fromS(pk), fromS(sk));

    if (attr.containsKey("documentId")) {
      String documentId = attr.get("documentId").s();

      if (!hasFiles(siteId, documentId)) {
        deleted = this.db.deleteItem(fromS(pk), fromS(sk));
      } else {
        throw new IOException("folder is not empty");
      }
    }

    return deleted;
  }

  @Override
  public void deletePath(final String siteId, final String documentId, final String path) {

    try {
      String[] folders = tokens(path);

      Map<String, Map<String, AttributeValue>> map =
          generateFileKeys(siteId, path, folders, documentId);

      List<Map<String, AttributeValue>> files = map.values().stream()
          .filter(stringAttributeValueMap -> "file".equals(stringAttributeValueMap.get("type").s()))
          .toList();

      for (Map<String, AttributeValue> file : files) {
        this.db.deleteItem(file.get(PK), file.get(SK));
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Map<String, Map<String, AttributeValue>> generateFileKeys(final String siteId,
      final String path, final String[] tokens, final String documentId) throws IOException {

    int i = 0;
    String lastUuid = "";
    int len = tokens.length;
    boolean allDirectories = path != null && path.endsWith("/");

    Map<String, Map<String, AttributeValue>> uuids = new HashMap<>();

    for (String folder : tokens) {

      String pk = getPk(siteId, lastUuid);

      if (!allDirectories && isFileToken(folder, i, len)) {

        String sk = getSk(folder, true);
        String docId = documentId;
        FolderIndexRecord record;
        if (docId == null) {
          record = getFolderId(siteId, pk, sk, folder);
        } else {
          record = new FolderIndexRecord().documentId(docId).type("file").parentDocumentId(lastUuid)
              .path(folder);
        }

        uuids.put(folder, record.getAttributes(siteId));

      } else {

        String sk = getSk(folder, false);

        FolderIndexRecord record = getFolderId(siteId, pk, sk, folder);
        lastUuid = record.documentId();
        uuids.put(folder, record.getAttributes(siteId));
      }

      i++;
    }

    return uuids;
  }

  @Override
  public List<FolderIndexRecordExtended> get(final String siteId, final String path,
      final String pathType, final String userId, final Date nowTimestamp) {

    String parentId = "";
    String[] tokens = tokens(path);

    int i = 0;
    int len = tokens.length;

    List<FolderIndexRecordExtended> list = new ArrayList<>();

    for (String token : tokens) {

      boolean isRecordChanged;
      String type = "file".equals(pathType) && (i == len - 1) ? "file" : "folder";
      FolderIndexRecord record =
          new FolderIndexRecord().parentDocumentId(parentId).documentId("").path(token).type(type);

      Map<String, AttributeValue> attrs = this.db.get(fromS(record.pk(siteId)), fromS(record.sk()));

      if (!attrs.isEmpty()) {

        record = record.getFromAttributes(siteId, attrs);
        isRecordChanged = false;

      } else {

        record.documentId(ID.uuid());

        if (!"file".equals(type)) {
          record.insertedDate(nowTimestamp);
          record.lastModifiedDate(nowTimestamp);
          record.userId(userId);
        }

        isRecordChanged = true;
      }

      list.add(new FolderIndexRecordExtended(record, isRecordChanged));

      checkParentId(record, parentId);

      if (record.documentId() != null) {
        parentId = record.documentId();
      }

      i++;
    }

    FolderIndexRecordExtended last = null;
    for (FolderIndexRecordExtended e : list) {

      if (e.isChanged() && last != null) {
        last.record().lastModifiedDate(new Date());
        last.changed(true);
      }

      last = e;
    }

    return list;
  }

  @Override
  public FolderIndexRecord getFolderByDocumentId(final String siteId, final String documentId) {

    Map<String, FolderIndexRecord> map = getFolderByDocumentIds(siteId, List.of(documentId));
    return map.getOrDefault(documentId, null);
  }

  @Override
  public Map<String, FolderIndexRecord> getFolderByDocumentIds(final String siteId,
      final List<String> documentIds) {

    List<Map<String, AttributeValue>> responses =
        documentIds.stream().map(documentId -> queryForFolderByDocumentId(siteId, documentId))
            .filter(r -> !r.items().isEmpty()).map(r -> r.items().get(0)).toList();

    return responses.stream().map(map -> this.db.get(map.get(PK), map.get(SK)))
        .map(attr -> new FolderIndexRecord().getFromAttributes(siteId, attr))
        .collect(Collectors.toMap(FolderIndexRecord::documentId, r -> r));
  }

  /**
   * Get Folder Id.
   * 
   * @param siteId {@link String}
   * @param pk {@link String}
   * @param sk {@link String}
   * @param folder {@link String}
   * @return {@link FolderIndexRecord}
   * @throws IOException IOException
   */
  private FolderIndexRecord getFolderId(final String siteId, final String pk, final String sk,
      final String folder) throws IOException {

    Map<String, AttributeValue> map = this.db.get(fromS(pk), fromS(sk));

    if (!map.containsKey("documentId")) {
      throw new IOException(String.format("index for '%s' does not exist", folder));
    }

    return new FolderIndexRecord().getFromAttributes(siteId, map);
  }

  @Override
  public List<FolderIndexRecord> getFoldersByDocumentId(final String siteId,
      final String documentId) {

    int i = 0;
    final int maxLoop = 100;
    boolean done = false;
    String id = documentId;

    Deque<FolderIndexRecord> queue = new LinkedList<>();

    while (!done) {
      FolderIndexRecord index = getFolderByDocumentId(siteId, id);

      if (index != null) {

        queue.add(index);
        id = index.parentDocumentId();

      } else {
        done = true;
      }

      i++;
      if (i > maxLoop) {
        throw new RuntimeException("maximum iterations reached");
      }
    }

    List<FolderIndexRecord> list = new ArrayList<>();
    while (!queue.isEmpty()) {
      list.add(queue.removeLast());
    }

    return list;
  }

  @Override
  public Map<String, Object> getIndex(final String siteId, final String path) throws IOException {

    Map<String, AttributeValue> attributes = getIndexByAttributeValues(siteId, path);
    return new AttributeValueToMap().apply(attributes);
  }

  @Override
  public DynamicObject getIndex(final String siteId, final String indexKey, final boolean isFile) {

    DynamicObject o = null;
    String index = URLDecoder.decode(indexKey, StandardCharsets.UTF_8);

    int pos = index.indexOf(TAG_DELIMINATOR);
    if (pos != -1) {
      String parentId = index.substring(0, pos);
      String path = index.substring(pos + 1);

      String pk = getPk(siteId, parentId);
      String sk = getSk(path, isFile);

      Map<String, AttributeValue> attr = this.db.get(fromS(pk), fromS(sk));

      o = new AttributeValueToDynamicObject().apply(attr);
    }

    return o;
  }

  @Override
  public List<FolderIndexRecord> getFolderIndexRecords(final String siteId, final String path)
      throws IOException {

    if (StringUtils.isEmpty(path)) {
      return Collections.emptyList();
    }

    String[] tokens = tokens(path);
    String fileToken = getFileToken(path, tokens);
    String lastUuid = "";
    List<FolderIndexRecord> records = new ArrayList<>();

    for (String folder : tokens) {

      String pk = getPk(siteId, lastUuid);
      String sk = getSk(folder, folder.equals(fileToken));
      DynamoDbKey key = new DynamoDbKey(pk, sk, null, null, null, null);

      Map<String, AttributeValue> attr = this.db.get(key);
      if (!attr.isEmpty()) {
        FolderIndexRecord record = new FolderIndexRecord().getFromAttributes(siteId, attr);
        records.add(record);
        lastUuid = record.documentId();
      } else {
        throw new IOException("Cannot find folder '" + path + "'");
      }

    }

    return records;
  }

  private String getFileToken(final String path, final String[] tokens) {
    if (path.endsWith("/")) {
      return null;
    }

    return last(tokens);
  }

  private Map<String, AttributeValue> getIndexByAttributeValues(final String siteId,
      final String path) throws IOException {

    Map<String, AttributeValue> attributes = Collections.emptyMap();

    if (!StringUtils.isEmpty(path)) {
      String[] folders = tokens(path);
      Map<String, Map<String, AttributeValue>> keys = generateFileKeys(siteId, path, folders, null);

      String key = folders[folders.length - 1];
      attributes = keys.get(key);
    }

    return attributes;
  }

  @Override
  public FolderIndexRecord getIndexAsRecord(final String siteId, final String indexKey,
      final boolean isFile) {

    FolderIndexRecord o = null;
    String index = URLDecoder.decode(indexKey, StandardCharsets.UTF_8);

    int pos = index.indexOf(TAG_DELIMINATOR);
    if (pos != -1) {
      String parentId = index.substring(0, pos);
      String path = index.substring(pos + 1);

      String pk = getPk(siteId, parentId);
      String sk = getSk(path, isFile);

      Map<String, AttributeValue> attr = this.db.get(fromS(pk), fromS(sk));

      o = new FolderIndexRecord().getFromAttributes(siteId, attr);
    }

    return o;
  }

  private String getPk(final String siteId, final String id) {
    return new FolderIndexRecord().parentDocumentId(id).pk(siteId);
  }

  private String getSk(final String folder, final boolean isFile) {
    return new FolderIndexRecord().path(folder).type(isFile ? "file" : "folder").sk();
  }

  private boolean hasFiles(final String siteId, final String documentId) {

    String pk = getPk(siteId, documentId);
    String expression = PK + " = :pk";

    Map<String, AttributeValue> values = Map.of(":pk", fromS(pk));

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression(expression).expressionAttributeValues(values).limit(1).build();

    QueryResponse response = this.dbClient.query(q);

    return !response.items().isEmpty();
  }

  @Override
  public boolean isFolderIdInPath(final String siteId, final String path, final String folderId)
      throws IOException {

    boolean found = false;
    String lastUuid = "";
    String[] folders = tokens(path);

    for (String folder : folders) {
      String pk = getPk(siteId, lastUuid);
      String sk = getSk(folder, false);
      FolderIndexRecord record = getFolderId(siteId, pk, sk, folder);

      lastUuid = record.documentId();

      if (folderId.equals(lastUuid)) {
        found = true;
        break;
      }
    }

    return found;
  }

  /**
   * Move File to Folder.
   * 
   * @param siteId {@link String}
   * @param sourcePath {@link String}
   * @param targetPath {@link String}
   * @param userId {@link String}
   * @throws IOException IOException
   */
  private void moveFileToFolder(final String siteId, final String sourcePath,
      final String targetPath, final String userId) throws IOException {

    Date now = new Date();

    List<FolderIndexRecordExtended> sourceRecords = get(siteId, sourcePath, "file", userId, now);
    List<FolderIndexRecordExtended> targetRecords = get(siteId, targetPath, "folder", userId, now);

    validateExists(sourcePath, sourceRecords);

    if (targetRecords.isEmpty()) {
      FolderIndexRecord record =
          new FolderIndexRecord().parentDocumentId("").type("folder").documentId("").path("");
      targetRecords = List.of(new FolderIndexRecordExtended(record, false));
    }

    FolderIndexRecordExtended sourceRecord = last(sourceRecords);
    FolderIndexRecordExtended targetRecord = last(targetRecords);

    FolderIndexRecord source = sourceRecord.record();
    FolderIndexRecord target = targetRecord.record();

    this.db.deleteItem(fromS(source.pk(siteId)), fromS(source.sk()));

    source.parentDocumentId(target.documentId());

    // save target folder if needed and source
    List<Map<String, AttributeValue>> toBeSaved =
        targetRecords.stream().filter(FolderIndexRecordExtended::isChanged)
            .map(r -> r.record().getAttributes(siteId)).collect(Collectors.toList());
    toBeSaved.add(source.getAttributes(siteId));
    this.db.putItems(toBeSaved);

    // update path on document
    String newPath =
        targetRecords.stream().map(r -> !isEmpty(r.record().path()) ? r.record().path() + "/" : "")
            .collect(Collectors.joining("")) + source.path();

    Map<String, AttributeValue> keys = keysDocument(siteId, source.documentId());
    this.db.updateValues(keys.get(PK), keys.get(SK), Map.of("path", fromS(newPath)));

    // update parent folder lastModifiedDate
    if (!isEmpty(target.documentId())) {
      SimpleDateFormat df = DateUtil.getIsoDateFormatter();

      String lastModifiedDate = df.format(new Date());
      this.db.updateValues(fromS(target.pk(siteId)), fromS(target.sk()),
          Map.of("lastModifiedDate", fromS(lastModifiedDate)));
    }
  }

  /**
   * Moves one Folder to another.
   * 
   * @param siteId {@link String}
   * @param sourcePath {@link String}
   * @param targetPath {@link String}
   * @param userId {@link String}
   * @throws IOException IOException
   */
  private void moveFolderToFolder(final String siteId, final String sourcePath,
      final String targetPath, final String userId) throws IOException {

    final int pos = targetPath.substring(0, targetPath.length() - 1).lastIndexOf("/");
    final String newTargetPath = pos > -1 ? targetPath.substring(0, pos) + "/" : "/";
    final String newPath = removeBackSlashes(pos > -1 ? targetPath.substring(pos) : targetPath);

    Date now = new Date();

    List<FolderIndexRecordExtended> sourceRecords = get(siteId, sourcePath, "folder", userId, now);
    List<FolderIndexRecordExtended> targetRecords =
        get(siteId, newTargetPath, "folder", userId, now);

    validateExists(sourcePath, sourceRecords);

    FolderIndexRecord source = last(sourceRecords).record();
    FolderIndexRecord target = last(targetRecords).record();

    this.db.deleteItem(fromS(source.pk(siteId)), fromS(source.sk()));

    source.parentDocumentId(target.documentId());
    source.path(newPath);

    // save target folder if needed and source
    List<Map<String, AttributeValue>> toBeSaved =
        targetRecords.stream().filter(FolderIndexRecordExtended::isChanged)
            .map(r -> r.record().getAttributes(siteId)).collect(Collectors.toList());
    toBeSaved.add(source.getAttributes(siteId));
    this.db.putItems(toBeSaved);

    // this.eventService.publish(event);
  }

  @Override
  public void moveIndex(final String siteId, final String sourcePath, final String targetPath,
      final String userId) throws IOException {

    String sourceType = sourcePath.endsWith("/") || sourcePath.isEmpty() ? "folder" : "file";
    String targetType = targetPath.endsWith("/") || targetPath.isEmpty() ? "folder" : "file";

    if ("file".equals(sourceType) && "folder".equals(targetType)) {

      moveFileToFolder(siteId, sourcePath, targetPath, userId);

    } else if ("folder".equals(sourceType) && "folder".equals(targetType)) {

      moveFolderToFolder(siteId, sourcePath, targetPath, userId);

    } else {
      throw new RuntimeException(
          String.format("Unsupported move %s to %s", sourceType, targetType));
    }
  }

  @Override
  public void setPermissions(final String siteId, final String path,
      final Collection<FolderRolePermission> roles) throws IOException {

    String folderPath = path.endsWith("/") ? path : path + "/";
    Map<String, AttributeValue> attributes = getIndexByAttributeValues(siteId, folderPath);
    FolderIndexRecord record = new FolderIndexRecord().getFromAttributes(siteId, attributes);
    record = record.rolePermissions(roles);
    this.db.putItem(record.getAttributes(siteId));
  }

  /**
   * Query GSI1 for Folder by DocumentId.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link QueryResponse}
   */
  private QueryResponse queryForFolderByDocumentId(final String siteId, final String documentId) {
    FolderIndexRecord record = new FolderIndexRecord().documentId(documentId);
    String pk = record.pkGsi1(siteId);
    return this.db.queryIndex(GSI1, fromS(pk), null, 1);
  }

  /**
   * Validate Path exists.
   * 
   * @param path {@link String}
   * @param sourceRecords {@link List} {@link FolderIndexRecordExtended}
   * @throws IOException IOException
   */
  private void validateExists(final String path,
      final List<FolderIndexRecordExtended> sourceRecords) throws IOException {

    Optional<FolderIndexRecordExtended> o =
        sourceRecords.stream().filter(FolderIndexRecordExtended::isChanged).findAny();
    if (o.isPresent()) {
      String msg = "folder '" + path + "' does not exist";
      throw new IOException(msg);
    }
  }
}
