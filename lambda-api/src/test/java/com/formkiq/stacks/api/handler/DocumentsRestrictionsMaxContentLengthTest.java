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
package com.formkiq.stacks.api.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.stacks.api.DynamoDbExtension;
import com.formkiq.stacks.api.DynamoDbTestServices;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.DynamoDbHelper;

/**
 * Unit Tests for {@link DocumentsRestrictionsMaxContentLength}.
 *
 * 
 */
@ExtendWith(DynamoDbExtension.class)
public class DocumentsRestrictionsMaxContentLengthTest {

  /** {@link DocumentsRestrictionsMaxContentLength}. */
  private static DocumentsRestrictionsMaxContentLength service =
      new DocumentsRestrictionsMaxContentLength();
  /** {@link AwsServiceCache}. */
  private AwsServiceCache awsservice;
  
  /**
   * Before Tests.
   * @throws URISyntaxException URISyntaxException
   * @throws IOException IOException
   */
  @BeforeEach
  public void before() throws URISyntaxException, IOException {
    DynamoDbConnectionBuilder adb = DynamoDbTestServices.getDynamoDbConnection(null);
    DynamoDbHelper dbhelper = new DynamoDbHelper(adb);
    this.awsservice = new AwsServiceCache().dbConnection(adb, dbhelper.getDocumentTable(), "")
        .appEnvironment("unittest");
  }

  /**
   * No Max Content Length or Content Length.
   */
  @Test
  public void testEnforced01() {
    // given
    String siteId = UUID.randomUUID().toString();
    Long contentLength = null;

    // when
    String value = service.getValue(this.awsservice, siteId);
    boolean result = service.enforced(this.awsservice, siteId, value, contentLength);

    // then
    assertFalse(result);
  }

  /**
   * Max Content Length, no Content Length.
   */
  @Test
  public void testEnforced02() {
    // given
    Long contentLength = null;
    String siteId = UUID.randomUUID().toString();
    
    DynamicObject ob = this.awsservice.configService().get(siteId);
    ob.put(ConfigService.MAX_DOCUMENT_SIZE_BYTES, "10");
    this.awsservice.configService().save(siteId, ob);

    // when
    String value = service.getValue(this.awsservice, siteId);
    boolean result = service.enforced(this.awsservice, siteId, value, contentLength);

    // then
    assertTrue(result);
  }

  /**
   * Max Content Length, Content Length less than or equal.
   */
  @Test
  public void testEnforced03() {
    // given
    Long contentLength = Long.valueOf("10");
    String siteId = UUID.randomUUID().toString();
    
    DynamicObject ob = this.awsservice.configService().get(siteId);
    ob.put(ConfigService.MAX_DOCUMENT_SIZE_BYTES, "10");
    this.awsservice.configService().save(siteId, ob);

    // when
    String value = service.getValue(this.awsservice, siteId);
    boolean result = service.enforced(this.awsservice, siteId, value, contentLength);

    // then
    assertFalse(result);
  }


  /**
   * Max Content Length, Content Length greater.
   */
  @Test
  public void testEnforced04() {
    // given
    Long contentLength = Long.valueOf("15");
    String siteId = UUID.randomUUID().toString();
    
    DynamicObject ob = this.awsservice.configService().get(siteId);
    ob.put(ConfigService.MAX_DOCUMENT_SIZE_BYTES, "10");
    this.awsservice.configService().save(siteId, ob);

    // when
    String value = service.getValue(this.awsservice, siteId);
    boolean result = service.enforced(this.awsservice, siteId, value, contentLength);

    // then
    assertTrue(result);
  }

  /**
   * Max Content Length, Content Length=0.
   */
  @Test
  public void testEnforced05() {
    // given
    Long contentLength = Long.valueOf(0);
    String siteId = UUID.randomUUID().toString();
    
    DynamicObject ob = this.awsservice.configService().get(siteId);
    ob.put(ConfigService.MAX_DOCUMENT_SIZE_BYTES, "10");
    this.awsservice.configService().save(siteId, ob);

    // when
    String value = service.getValue(this.awsservice, siteId);
    boolean result = service.enforced(this.awsservice, siteId, value, contentLength);

    // then
    assertTrue(result);
  }
}
