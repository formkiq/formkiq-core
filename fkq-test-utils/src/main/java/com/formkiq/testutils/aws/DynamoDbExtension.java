/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018-2022 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.testutils.aws;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;

/**
 * 
 * JUnit 5 Extension for DynamoDb.
 *
 */
public class DynamoDbExtension
    implements BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {

  /** Documents Table Name. */
  public static final String DOCUMENTS_TABLE = "Documents";
  /** Cache Table Name. */
  public static final String CACHE_TABLE = "Cache";
  /** {@link DynamoDbConnectionBuilder}. */
  private DynamoDbConnectionBuilder dbConnection;
  /** {@link GenericContainer}. */
  private GenericContainer<?> dynamoDbLocal;
  /** {@link DynamoDbHelper}. */
  private DynamoDbHelper dbhelper;


  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    this.dynamoDbLocal = DynamoDbTestServices.getDynamoDbLocal();
    this.dynamoDbLocal.start();

    this.dbConnection = DynamoDbTestServices.getDynamoDbConnection(this.dynamoDbLocal);
    this.dbhelper =
        new DynamoDbHelper(DynamoDbTestServices.getDynamoDbConnection(this.dynamoDbLocal));

    DynamoDbHelper dbHelper = new DynamoDbHelper(this.dbConnection);
    if (!dbHelper.isTableExists(DOCUMENTS_TABLE)) {
      dbHelper.createDocumentsTable(DOCUMENTS_TABLE);
    }

    if (!dbHelper.isTableExists(CACHE_TABLE)) {
      dbHelper.createCacheTable(CACHE_TABLE);
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

    if (this.dbConnection != null) {
      this.dbConnection.close();
    }
  }
}
