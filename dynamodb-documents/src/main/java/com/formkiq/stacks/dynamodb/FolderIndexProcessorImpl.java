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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
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

      String ss = path.startsWith(DELIMINATOR) ? path.substring(DELIMINATOR.length()) : path;
      ss = ss.replaceAll(":://", DELIMINATOR);
      strs = ss.split(DELIMINATOR);

    } else {
      strs = new String[] {};
    }

    return strs;
  }

  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();

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

  /**
   * Create Folder.
   * 
   * @param siteId {@link String}
   * @param pk {@link String}
   * @param sk {@link String}
   * @param folder {@link String}
   * @param insertedDate {@link Date}
   * @param userId {@link String}
   * @return {@link String}
   */
  private String createFolder(final String siteId, final String pk, final String sk,
      final String folder, final Date insertedDate, final String userId) {

    String uuid;

    uuid = UUID.randomUUID().toString();

    Map<String, AttributeValue> values = new HashMap<>(Map.of(PK,
        AttributeValue.builder().s(pk).build(), SK, AttributeValue.builder().s(sk).build(),
        "documentId", AttributeValue.builder().s(uuid).build()));

    String fullInsertedDate = this.df.format(insertedDate);
    addS(values, "inserteddate", fullInsertedDate);
    addS(values, "lastModifiedDate", fullInsertedDate);
    addS(values, "userId", userId);
    addS(values, "path", folder);
    addS(values, "type", "folder");

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
          uuid = cr.item().get("documentId").s();
        } else {
          throw e;
        }
      } else {
        throw e;
      }
    }

    return uuid;
  }

  private void createFolderPaths(final String siteId, final String[] folders,
      final Date insertedDate, final String userId, final boolean allDirectories) {

    int i = 0;
    String lastUuid = "";
    int len = folders.length;
    for (String folder : folders) {

      String pk = getPk(siteId, lastUuid);
      String sk = getSk(folder, false);

      if (allDirectories || !isFileToken(folder, i, len)) {
        lastUuid = createFolder(siteId, pk, sk, folder, insertedDate, userId);
      }

      i++;
    }
  }

  @Override
  public boolean deleteEmptyDirectory(final String siteId, final String parentId,
      final String path) {

    boolean deleted = false;
    String pk = getPk(siteId, parentId);
    String sk = getSk(path, false);

    Map<String, AttributeValue> attr =
        this.dynamoDb.get(AttributeValue.fromS(pk), AttributeValue.fromS(sk));

    String documentId = attr.get("documentId").s();

    if (!hasFiles(siteId, documentId)) {
      this.dynamoDb.deleteItem(AttributeValue.fromS(pk), AttributeValue.fromS(sk));
      deleted = true;
    }

    return deleted;
  }

  @Override
  public void deletePath(final String siteId, final String documentId, final String path) {

    try {
      String[] folders = tokens(path);

      Map<String, Map<String, String>> map = generateFileKeys(siteId, path, folders, documentId);

      List<Map<String, String>> files =
          map.entrySet().stream().filter(f -> "file".equals(f.getValue().get("type")))
              .map(e -> e.getValue()).collect(Collectors.toList());

      for (Map<String, String> file : files) {
        this.dynamoDb.deleteItem(AttributeValue.fromS(file.get(PK)),
            AttributeValue.fromS(file.get(SK)));
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Map<String, Map<String, String>> generateFileKeys(final String siteId, final String path,
      final String[] tokens, final String documentId) throws IOException {

    int i = 0;
    String lastUuid = "";
    int len = tokens.length;
    boolean allDirectories = path != null && path.endsWith("/");

    Map<String, Map<String, String>> uuids = new HashMap<>();

    for (String folder : tokens) {

      String pk = getPk(siteId, lastUuid);

      if (!allDirectories && isFileToken(folder, i, len)) {

        String sk = getSk(folder, true);
        String docId = documentId;
        if (docId == null) {
          docId = getFolderId(siteId, pk, sk, folder);
        }

        uuids.put(folder, Map.of(PK, pk, SK, sk, "documentId", docId, "type", "file"));

      } else {

        String sk = getSk(folder, false);

        lastUuid = getFolderId(siteId, pk, sk, folder);
        uuids.put(folder, Map.of(PK, pk, SK, sk, "documentId", lastUuid, "type", "folder"));
      }

      i++;
    }

    return uuids;
  }

  /**
   * Generate File Keys and Create Path if they dont exist.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @param folders {@link String}
   * @param insertedDate {@link Date}
   * @param userId {@link String}
   * @return {@link Map}
   * @throws IOException IOException
   */
  private Map<String, Map<String, String>> generateFileKeysAndCreatePaths(final String siteId,
      final String path, final String[] folders, final Date insertedDate, final String userId)
      throws IOException {

    Map<String, Map<String, String>> destination;

    try {
      destination = generateFileKeys(siteId, path, folders, null);
    } catch (IOException e) {
      boolean allDirectories = path != null && path.endsWith("/");
      createFolderPaths(siteId, folders, insertedDate, userId, allDirectories);
      destination = generateFileKeys(siteId, path, folders, null);
    }

    return destination;
  }

  @Override
  public List<Map<String, AttributeValue>> generateIndex(final String siteId,
      final DocumentItem item) {

    List<Map<String, AttributeValue>> list = new ArrayList<>();

    String path = item.getPath();
    String[] folders = tokens(path);
    Date insertedDate = item.getInsertedDate() != null ? item.getInsertedDate() : new Date();

    Map<String, Map<String, String>> uuidMap = Collections.emptyMap();

    try {
      uuidMap = generateFileKeys(siteId, item.getPath(), folders, item.getDocumentId());
    } catch (IOException e) {

      boolean allDirectories = path != null && path.endsWith("/");
      createFolderPaths(siteId, folders, insertedDate, item.getUserId(), allDirectories);

      try {
        uuidMap = generateFileKeys(siteId, item.getPath(), folders, item.getDocumentId());
      } catch (IOException ee) {
        throw new RuntimeException(ee);
      }
    }

    for (String folder : folders) {

      Map<String, String> map = uuidMap.get(folder);

      AttributeValue pk = AttributeValue.fromS(map.get(PK));
      AttributeValue sk = AttributeValue.fromS(map.get(SK));
      String type = map.get("type");

      Map<String, AttributeValue> values = new HashMap<>(Map.of(PK, pk, SK, sk));
      values.put("path", AttributeValue.fromS(folder));
      values.put("type", AttributeValue.fromS(type));

      if ("file".equals(type)) {
        addS(values, "documentId", item.getDocumentId());
      } else {
        String fullInsertedDate = this.df.format(insertedDate);
        addS(values, "inserteddate", fullInsertedDate);
        addS(values, "lastModifiedDate", fullInsertedDate);
        addS(values, "userId", item.getUserId());
        addS(values, "documentId", map.get("documentId"));

      }

      list.add(values);
    }

    return list;
  }

  /**
   * Get Folder Id.
   * 
   * @param siteId {@link String}
   * @param pk {@link String}
   * @param sk {@link String}
   * @param folder {@link String}
   * @return {@link String}
   * @throws IOException IOException
   */
  private String getFolderId(final String siteId, final String pk, final String sk,
      final String folder) throws IOException {

    Map<String, AttributeValue> map =
        this.dynamoDb.get(AttributeValue.fromS(pk), AttributeValue.fromS(sk));

    if (!map.containsKey("documentId")) {
      throw new IOException(String.format("index for '%s' does not exist", folder));
    }

    String uuid = map.get("documentId").s();

    return uuid;
  }

  @Override
  public Map<String, String> getIndex(final String siteId, final String path) throws IOException {
    Map<String, String> map = Collections.emptyMap();

    if (!StringUtils.isEmpty(path)) {
      String[] folders = tokens(path);
      Map<String, Map<String, String>> keys = generateFileKeys(siteId, path, folders, null);

      String key = folders[folders.length - 1];
      map = keys.get(key);
    }

    return map;
  }

  private String getPk(final String siteId, final String id) {
    return SiteIdKeyGenerator.createS3Key(siteId, GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR + id);
  }

  private String getSk(final String folder, final boolean isFile) {
    return isFile ? INDEX_FILE_SK + folder.toLowerCase() : INDEX_FOLDER_SK + folder.toLowerCase();
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

  /**
   * Move Directory from one to another.
   * 
   * @param siteId {@link String}
   * @param source {@link Map}
   * @param sourceFolders {@link String}
   * @param target {@link Map}
   * @param targetPath {@link String}
   */
  private void moveDirectoryToDirectory(final String siteId, final Map<String, String> source,
      final String[] sourceFolders, final Map<String, String> target, final String targetPath) {

    Map<String, AttributeValue> sourceAttr = this.dynamoDb.get(AttributeValue.fromS(source.get(PK)),
        AttributeValue.fromS(source.get(SK)));

    Map<String, AttributeValue> targetAttr = new HashMap<>(sourceAttr);

    String pk =
        targetAttr.get(PK).s().substring(0, targetAttr.get(PK).s().lastIndexOf(TAG_DELIMINATOR) + 1)
            + target.get("documentId");
    targetAttr.put(PK, AttributeValue.fromS(pk));
    this.dynamoDb.putItem(targetAttr);

    this.dynamoDb.deleteItem(AttributeValue.fromS(source.get(PK)),
        AttributeValue.fromS(source.get(SK)));
  }

  /**
   * Move Directory from one to another.
   * 
   * @param siteId {@link String}
   * @param source {@link Map}
   * @param sourceFolders {@link String}
   * @param target {@link Map}
   * @param targetPath {@link String}
   */
  private void moveFileToDirectory(final String siteId, final Map<String, String> source,
      final String[] sourceFolders, final Map<String, String> target, final String targetPath) {

    String sourcePk = source.get(PK);
    String sourceSk = source.get(SK);

    String targetPk = target.get(PK);
    String targetSk = target.get(SK);

    int pos = sourcePk.lastIndexOf(TAG_DELIMINATOR) + 1;

    String destPk = null;

    if (target.containsKey("documentId")) {
      destPk = sourcePk.substring(0, pos) + target.get("documentId");
    } else {
      destPk = getPk(siteId, "");
    }

    int sourceFolderLength = sourceFolders.length;
    String filename = sourceFolders[sourceFolderLength - 1];

    String targetPathName = "/".equals(targetPath) ? "" : targetPath;
    renameFile(siteId, sourcePk, sourceSk, destPk, sourceSk, targetPathName + filename);

    // update LastModified Date on Directory
    if (targetPk != null && targetSk != null) {
      String lastModifiedDate = this.df.format(new Date());
      this.dynamoDb.updateFields(AttributeValue.fromS(targetPk), AttributeValue.fromS(targetSk),
          Map.of("lastModifiedDate", AttributeValue.fromS(lastModifiedDate)));
    }
  }

  @Override
  public void moveIndex(final String siteId, final String sourcePath, final String targetPath,
      final String userId) throws IOException {

    String[] sourceFolders = tokens(sourcePath);
    String[] targetFolders = tokens(targetPath);

    Map<String, Map<String, String>> source =
        generateFileKeys(siteId, sourcePath, sourceFolders, null);

    Map<String, String> sourceMap = source.get(sourceFolders[sourceFolders.length - 1]);
    String sourceType = sourceMap.get("type");

    Map<String, Map<String, String>> target =
        generateFileKeysAndCreatePaths(siteId, targetPath, targetFolders, new Date(), userId);

    Map<String, String> targetMap =
        !target.isEmpty() ? target.get(targetFolders[targetFolders.length - 1])
            : Map.of("type", "folder");

    String targetType = targetMap.get("type");

    if ("file".equals(sourceType) && "folder".equals(targetType)) {

      moveFileToDirectory(siteId, sourceMap, sourceFolders, targetMap, targetPath);

    } else if ("folder".equals(sourceType) && "folder".equals(targetType)) {

      moveDirectoryToDirectory(siteId, sourceMap, sourceFolders, targetMap, targetPath);

    } else {
      throw new RuntimeException(
          String.format("Unsupported move %s to %s", sourceType, targetType));
    }
  }

  /**
   * Move Record from Key to another.
   * 
   * @param siteId {@link String}
   * @param pkSource {@link String}
   * @param skSource {@link String}
   * @param pkDestination {@link String}
   * @param skDestination {@link String}
   * @param newPath {@link String}
   */
  private void renameFile(final String siteId, final String pkSource, final String skSource,
      final String pkDestination, final String skDestination, final String newPath) {

    Map<String, AttributeValue> sourceKey =
        Map.of(PK, AttributeValue.fromS(pkSource), SK, AttributeValue.fromS(skSource));

    GetItemResponse response = this.dbClient
        .getItem(GetItemRequest.builder().tableName(this.documentTableName).key(sourceKey).build());

    Map<String, AttributeValue> attributes = new HashMap<>(response.item());
    attributes.put(PK, AttributeValue.builder().s(pkDestination).build());
    attributes.put(SK, AttributeValue.builder().s(skDestination).build());

    if (newPath != null) {
      String documentId = attributes.get("documentId").s();
      attributes.put("path", AttributeValue.builder().s(newPath).build());

      // update path on document
      Map<String, AttributeValue> keys = keysDocument(siteId, documentId);
      this.dynamoDb.updateFields(keys.get(PK), keys.get(SK),
          Map.of("path", AttributeValue.fromS(newPath)));
    }

    this.dynamoDb.putItem(attributes);

    this.dynamoDb.deleteItem(sourceKey.get(PK), sourceKey.get(SK));
  }
}
