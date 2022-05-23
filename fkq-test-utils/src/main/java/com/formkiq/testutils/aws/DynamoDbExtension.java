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
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.DynamoDbHelper;

/**
 * 
 * JUnit 5 Extension for DynamoDb.
 *
 */
public class DynamoDbExtension
    implements BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {

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
    if (!dbHelper.isDocumentsTableExists()) {
      dbHelper.createDocumentsTable();
    }

    if (!dbHelper.isCacheTableExists()) {
      dbHelper.createCacheTable();
    }
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    this.dbhelper.truncateDocumentDates();
    this.dbhelper.truncateWebhooks();
    this.dbhelper.truncateDocumentsTable();
    this.dbhelper.truncateConfig();
    if (0 != this.dbhelper.getDocumentItemCount()) {
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
