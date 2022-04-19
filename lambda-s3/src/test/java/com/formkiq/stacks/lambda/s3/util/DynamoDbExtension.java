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
package com.formkiq.stacks.lambda.s3.util;

import static org.junit.Assert.assertEquals;
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
    this.dbhelper.truncateDocumentsTable();
    this.dbhelper.truncateWebhooks();
    this.dbhelper.truncateConfig();
    assertEquals(0, this.dbhelper.getDocumentItemCount());
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
