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

import static com.formkiq.aws.dynamodb.objects.Objects.last;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.dynamodb.objects.Strings.removeBackSlashes;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.AttributeValueToDynamicObject;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link FolderIndexProcessor} for generating Folder structure.
 *
 */
public class FolderIndexProcessorImpl implements FolderIndexProcessor, DbKeys {

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

      String p = path.replaceAll(":://", DELIMINATOR).replaceAll("/+", "/");
      String ss = p.startsWith(DELIMINATOR) ? p.substring(DELIMINATOR.length()) : p;
      strs = ss.split(DELIMINATOR);

    } else {
      strs = new String[] {};
    }

    return strs;
  }

  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;
  /** Documents Table Name. */
  private String documentTableName;
  /** {@link DynamoDbService}. */
  private DynamoDbService dynamoDb;

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
    this.dynamoDb = new DynamoDbServiceImpl(connection, documentsTable);
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

    String uuid;

    uuid = UUID.randomUUID().toString();

    FolderIndexRecord record = new FolderIndexRecord().parentDocumentId(parentId).documentId(uuid)
        .insertedDate(insertedDate).lastModifiedDate(insertedDate).userId(userId).path(folder)
        .type("folder");
    Map<String, AttributeValue> values = new HashMap<>(record.getAttributes(siteId));

    String conditionExpression = "attribute_not_exists(" + PK + ")";

    Put put = Put.builder().tableName(this.documentTableName)
        .conditionExpression(conditionExpression).item(values)
        .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD).build();

    try {
      this.dbClient.transactWriteItems(TransactWriteItemsRequest.builder()
          .transactItems(TransactWriteItem.builder().put(put).build()).build());
    } catch (TransactionCanceledException e) {
      if (!e.cancellationReasons().isEmpty()) {
        CancellationReason cr = e.cancellationReasons().get(0);
        if (cr.item() != null && cr.item().containsKey("documentId")) {
          values = cr.item();
          record = record.getFromAttributes(siteId, values);
        } else {
          throw e;
        }
      } else {
        throw e;
      }
    }

    return record;
  }

  private List<Map<String, String>> createFolderPaths(final String siteId, final String[] folders,
      final Date insertedDate, final String userId, final boolean allDirectories) {

    int i = 0;
    String lastUuid = "";
    int len = folders.length;
    List<Map<String, String>> list = new ArrayList<>();

    for (String folder : folders) {

      if (allDirectories || !isFileToken(folder, i, len)) {

        FolderIndexRecord record = createFolder(siteId, lastUuid, folder, insertedDate, userId);
        record.parentDocumentId(lastUuid);

        String indexKey = record.createIndexKey(siteId);
        list.add(Map.of("folder", folder, "indexKey", indexKey));

        lastUuid = record.documentId();
      }

      i++;
    }

    return list;
  }

  @Override
  public List<Map<String, String>> createFolders(final String siteId, final String path,
      final String userId) {
    boolean allDirectories = true;
    Date insertedDate = new Date();
    String[] folders = tokens(path);
    return createFolderPaths(siteId, folders, insertedDate, userId, allDirectories);
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

    Map<String, AttributeValue> attr =
        this.dynamoDb.get(AttributeValue.fromS(pk), AttributeValue.fromS(sk));

    if (attr.containsKey("documentId")) {
      String documentId = attr.get("documentId").s();

      if (!hasFiles(siteId, documentId)) {
        this.dynamoDb.deleteItem(AttributeValue.fromS(pk), AttributeValue.fromS(sk));
        deleted = true;
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

      List<Map<String, AttributeValue>> files =
          map.entrySet().stream().filter(f -> "file".equals(f.getValue().get("type").s()))
              .map(e -> e.getValue()).collect(Collectors.toList());

      for (Map<String, AttributeValue> file : files) {
        this.dynamoDb.deleteItem(file.get(PK), file.get(SK));
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
        FolderIndexRecord record = null;
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
  public List<Map<String, AttributeValue>> generateIndex(final String siteId,
      final DocumentItem item) {

    Date now = new Date();
    List<FolderIndexRecordExtended> records =
        get(siteId, item.getPath(), "file", item.getUserId(), now);

    if (!records.isEmpty()) {
      FolderIndexRecordExtended extended = last(records);
      FolderIndexRecord record = extended.record();

      if (!extended.isChanged() && !item.getDocumentId().equals(record.documentId())) {
        String extension = Strings.getExtension(record.path());
        String newFilename = record.path().replaceAll("\\." + extension,
            " (" + item.getDocumentId() + ")" + "." + extension);
        record.path(newFilename);

        if (records.size() > 1) {
          String path = records.subList(0, records.size() - 1).stream().map(s -> s.record().path())
              .collect(Collectors.joining("/"));
          newFilename = path + "/" + newFilename;
        }

        item.setPath(newFilename);
        extended.changed(true);
      }

      record.documentId(item.getDocumentId());
    }

    return records.stream().filter(r -> r.isChanged()).map(r -> r.record().getAttributes(siteId))
        .collect(Collectors.toList());
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

      boolean isRecordChanged = false;
      String type = "file".equals(pathType) && (i == len - 1) ? "file" : "folder";
      FolderIndexRecord record =
          new FolderIndexRecord().parentDocumentId(parentId).documentId("").path(token).type(type);

      Map<String, AttributeValue> attrs = this.dynamoDb.get(AttributeValue.fromS(record.pk(siteId)),
          AttributeValue.fromS(record.sk()));

      if (!attrs.isEmpty()) {

        record = record.getFromAttributes(siteId, attrs);
        isRecordChanged = false;

      } else {

        record.documentId(UUID.randomUUID().toString());

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

    Map<String, FolderIndexRecord> map = getFolderByDocumentIds(siteId, Arrays.asList(documentId));
    return map.containsKey(documentId) ? map.get(documentId) : null;
  }

  @Override
  public Map<String, FolderIndexRecord> getFolderByDocumentIds(final String siteId,
      final List<String> documentIds) {

    List<Map<String, AttributeValue>> responses = documentIds.stream()
        .map(documentId -> queryForFolderByDocumentId(siteId, documentId))
        .filter(r -> !r.items().isEmpty()).map(r -> r.items().get(0)).collect(Collectors.toList());

    Map<String, FolderIndexRecord> recordMap =
        responses.stream().map(map -> this.dynamoDb.get(map.get(PK), map.get(SK)))
            .map(attr -> new FolderIndexRecord().getFromAttributes(siteId, attr))
            .collect(Collectors.toMap(r -> r.documentId(), r -> r));

    return recordMap;

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

    Map<String, AttributeValue> map =
        this.dynamoDb.get(AttributeValue.fromS(pk), AttributeValue.fromS(sk));

    if (!map.containsKey("documentId")) {
      throw new IOException(String.format("index for '%s' does not exist", folder));
    }

    FolderIndexRecord record = new FolderIndexRecord().getFromAttributes(siteId, map);

    return record;
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
  public Map<String, String> getIndex(final String siteId, final String path) throws IOException {

    Map<String, String> map = Collections.emptyMap();

    if (!StringUtils.isEmpty(path)) {
      String[] folders = tokens(path);
      Map<String, Map<String, AttributeValue>> keys = generateFileKeys(siteId, path, folders, null);

      String key = folders[folders.length - 1];
      map = keys.get(key).entrySet().stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().s()));
    }

    return map;
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

      Map<String, AttributeValue> attr =
          this.dynamoDb.get(AttributeValue.fromS(pk), AttributeValue.fromS(sk));

      o = new AttributeValueToDynamicObject().apply(attr);
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

    Map<String, AttributeValue> values = Map.of(":pk", AttributeValue.fromS(pk));

    QueryRequest q =
        QueryRequest.builder().tableName(this.documentTableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).limit(Integer.valueOf(1)).build();

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
      targetRecords = Arrays.asList(new FolderIndexRecordExtended(record, false));
    }

    FolderIndexRecordExtended sourceRecord = last(sourceRecords);
    FolderIndexRecordExtended targetRecord = last(targetRecords);

    FolderIndexRecord source = sourceRecord.record();
    FolderIndexRecord target = targetRecord.record();

    this.dynamoDb.deleteItem(AttributeValue.fromS(source.pk(siteId)),
        AttributeValue.fromS(source.sk()));

    source.parentDocumentId(target.documentId());

    // save target folder if needed and source
    List<Map<String, AttributeValue>> toBeSaved = targetRecords.stream().filter(r -> r.isChanged())
        .map(r -> r.record().getAttributes(siteId)).collect(Collectors.toList());
    toBeSaved.add(source.getAttributes(siteId));
    this.dynamoDb.putItems(toBeSaved);

    // update path on document
    String newPath =
        targetRecords.stream().map(r -> !isEmpty(r.record().path()) ? r.record().path() + "/" : "")
            .collect(Collectors.joining("")) + source.path();

    Map<String, AttributeValue> keys = keysDocument(siteId, source.documentId());
    this.dynamoDb.updateValues(keys.get(PK), keys.get(SK),
        Map.of("path", AttributeValue.fromS(newPath)));

    // update parent folder lastModifiedDate
    if (!isEmpty(target.documentId())) {
      SimpleDateFormat df = DateUtil.getIsoDateFormatter();

      String lastModifiedDate = df.format(new Date());
      this.dynamoDb.updateValues(AttributeValue.fromS(target.pk(siteId)),
          AttributeValue.fromS(target.sk()),
          Map.of("lastModifiedDate", AttributeValue.fromS(lastModifiedDate)));
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

    this.dynamoDb.deleteItem(AttributeValue.fromS(source.pk(siteId)),
        AttributeValue.fromS(source.sk()));

    // String site = siteId != null ? siteId : DEFAULT_SITE_ID;
    // final FolderEvent event = new FolderEvent().siteId(site).documentId(source.documentId())
    // .sourcePath(source.path()).destinationPath(destinationPath).type(FolderEventType.MOVE);

    source.parentDocumentId(target.documentId());
    source.path(newPath);

    // save target folder if needed and source
    List<Map<String, AttributeValue>> toBeSaved = targetRecords.stream().filter(r -> r.isChanged())
        .map(r -> r.record().getAttributes(siteId)).collect(Collectors.toList());
    toBeSaved.add(source.getAttributes(siteId));
    this.dynamoDb.putItems(toBeSaved);

    // this.eventService.publish(event);
  }

  @Override
  public void moveIndex(final String siteId, final String sourcePath, final String targetPath,
      final String userId) throws IOException {

    String sourceType = sourcePath.endsWith("/") || "".equals(sourcePath) ? "folder" : "file";
    String targetType = targetPath.endsWith("/") || "".equals(targetPath) ? "folder" : "file";

    if ("file".equals(sourceType) && "folder".equals(targetType)) {

      moveFileToFolder(siteId, sourcePath, targetPath, userId);

    } else if ("folder".equals(sourceType) && "folder".equals(targetType)) {

      moveFolderToFolder(siteId, sourcePath, targetPath, userId);

    } else {
      throw new RuntimeException(
          String.format("Unsupported move %s to %s", sourceType, targetType));
    }
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
    QueryResponse response = this.dynamoDb.queryIndex(GSI1, AttributeValue.fromS(pk), null, 1);
    return response;
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
        sourceRecords.stream().filter(r -> r.isChanged()).findAny();
    if (o.isPresent()) {
      String msg = "folder '" + path + "' does not exist";
      throw new IOException(msg);
    }
  }
}
