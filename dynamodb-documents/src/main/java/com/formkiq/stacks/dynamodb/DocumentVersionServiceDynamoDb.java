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

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * {@link DocumentVersionService} implementation.
 *
 */
@Reflectable
public class DocumentVersionServiceDynamoDb implements DocumentVersionService {

  /** The Default maximum results returned. */
  private static final int MAX_RESULTS = 100;
  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** DynamoDB Document Versions Table Name. */
  private String tableName = null;

  @Override
  public void addDocumentVersionAttributes(final Map<String, AttributeValue> previous,
      final Map<String, AttributeValue> current) {

    if (!previous.isEmpty()) {

      String version = current.getOrDefault(VERSION_ATTRIBUTE, AttributeValue.fromS("1")).s();
      String nextVersion = String.valueOf(Integer.parseInt(version) + 1);

      previous.put(VERSION_ATTRIBUTE, AttributeValue.fromS(version));

      String sk = getSk(previous, version);
      previous.put(SK, AttributeValue.fromS(sk));

      current.put(VERSION_ATTRIBUTE, AttributeValue.fromS(nextVersion));
    }
  }

  @Override
  public void deleteAllVersionIds(final DynamoDbClient client, final String siteId,
      final String documentId) {

    Map<String, AttributeValue> startkey = null;
    String pk = createDatabaseKey(siteId, PREFIX_DOCS + documentId);

    String documentVersionsTable = getDocumentVersionsTableName();
    DynamoDbService db = new DynamoDbServiceImpl(client, documentVersionsTable);

    do {

      QueryResponse response = db.query(AttributeValue.fromS(pk), startkey, MAX_RESULTS);

      List<Map<String, AttributeValue>> results = response.items();

      for (Map<String, AttributeValue> map : results) {
        db.deleteItem(map.get(PK), map.get(SK));
      }

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());
  }

  @Override
  public String getDocumentVersionsTableName() {
    return this.tableName;
  }

  private String getSk(final Map<String, AttributeValue> previous, final String version) {
    String sk = previous.get(SK).s() + TAG_DELIMINATOR + this.df.format(new Date())
        + TAG_DELIMINATOR + "v" + version;
    return sk;
  }

  @Override
  public String getVersionId(final DynamoDbConnectionBuilder connection, final String siteId,
      final String documentId, final String versionKey) {

    String versionId = null;
    String documentVersionsTable = getDocumentVersionsTableName();

    if (!isEmpty(documentVersionsTable) && !isEmpty(versionKey)) {

      String pk = createDatabaseKey(siteId, PREFIX_DOCS + documentId);
      String sk = versionKey;

      DynamoDbService db = new DynamoDbServiceImpl(connection, documentVersionsTable);

      Map<String, AttributeValue> attrs =
          db.get(AttributeValue.fromS(pk), AttributeValue.fromS(sk));

      if (attrs.containsKey(S3VERSION_ATTRIBUTE)) {
        versionId = attrs.get(S3VERSION_ATTRIBUTE).s();
      }
    }

    return versionId;
  }

  @Override
  public void initialize(final Map<String, String> map) {
    this.tableName = map.get("DOCUMENT_VERSIONS_TABLE");
  }

  @Override
  public void revertDocumentVersionAttributes(final Map<String, AttributeValue> previous,
      final Map<String, AttributeValue> current) {

    String nextVersion = String.valueOf(Integer.parseInt(current.get(VERSION_ATTRIBUTE).s()) + 1);

    previous.put(SK, current.get(SK));
    previous.put(VERSION_ATTRIBUTE, AttributeValue.fromS(nextVersion));

    String sk = getSk(current, current.get(VERSION_ATTRIBUTE).s());
    current.put(SK, AttributeValue.fromS(sk));
  }
}
