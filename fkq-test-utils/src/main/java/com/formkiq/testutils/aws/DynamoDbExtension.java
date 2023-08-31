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
package com.formkiq.testutils.aws;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import com.formkiq.aws.dynamodb.schema.DocumentSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * 
 * JUnit 5 Extension for DynamoDb.
 *
 */
public class DynamoDbExtension
    implements BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {

  /** Document Syncs Table Name. */
  public static final String DOCUMENT_SYNCS_TABLE = "DocumentSyncs";
  /** Documents Table Name. */
  public static final String DOCUMENTS_TABLE = "Documents";
  /** Documents Table Name. */
  public static final String DOCUMENTS_VERSION_TABLE = "DocumentVersions";
  /** Cache Table Name. */
  public static final String CACHE_TABLE = "Cache";
  /** {@link GenericContainer}. */
  private GenericContainer<?> dynamoDbLocal;
  /** {@link DynamoDbHelper}. */
  private DynamoDbHelper dbhelper;
  /** {@link DocumentSchema}. */
  private DocumentSchema schema;

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    this.dynamoDbLocal = DynamoDbTestServices.getDynamoDbLocal();

    if (this.dynamoDbLocal != null) {
      this.dynamoDbLocal.start();
    }

    this.dbhelper = new DynamoDbHelper(DynamoDbTestServices.getDynamoDbConnection());

    try (DynamoDbClient db = DynamoDbTestServices.getDynamoDbConnection().build()) {
      this.schema = new DocumentSchema(db);

      this.schema.createDocumentsTable(DOCUMENTS_TABLE);
      this.schema.createDocumentsTable(DOCUMENTS_VERSION_TABLE);
      this.schema.createCacheTable(CACHE_TABLE);
      this.schema.createDocumentSyncsTable(DOCUMENT_SYNCS_TABLE);
    }
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    this.dbhelper.truncateTable(DOCUMENTS_TABLE);
    this.dbhelper.truncateTable(CACHE_TABLE);
    if (0 != this.dbhelper.getDocumentItemCount(DOCUMENTS_TABLE)) {
      throw new RuntimeException("Database is not empty");
    }
  }

  @Override
  public void close() throws Throwable {

    if (this.dynamoDbLocal != null) {
      this.dynamoDbLocal.stop();
    }
  }
}
