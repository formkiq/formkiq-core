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

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.stacks.api.handler.documents.DocumentsRestrictionsMaxContentLength;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceExtension;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/**
 * Unit Tests for {@link DocumentsRestrictionsMaxContentLength}.
 *
 * 
 */
@ExtendWith(DynamoDbExtension.class)
public class DocumentsRestrictionsMaxContentLengthTest {

  /** {@link DocumentsRestrictionsMaxContentLength}. */
  private static final DocumentsRestrictionsMaxContentLength SERVICE =
      new DocumentsRestrictionsMaxContentLength();
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsservice;

  /**
   * Before All.
   * 
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {

    Map<String, String> map = Map.of("DOCUMENTS_TABLE", DOCUMENTS_TABLE, "CACHE_TABLE", "",
        "APP_ENVIRONMENT", "unittest");
    awsservice = new AwsServiceCache().environment(map);

    DynamoDbConnectionBuilder adb = DynamoDbTestServices.getDynamoDbConnection();
    awsservice.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(adb));
    awsservice.register(ConfigService.class, new ConfigServiceExtension());
  }

  /**
   * No Max Content Length or Content Length.
   */
  @Test
  public void testIsViolated01() {
    // given
    String siteId = ID.uuid();
    SiteConfiguration config = SiteConfiguration.builder().build(siteId);
    DocumentItem item = new DocumentItemDynamoDb();

    // when
    boolean result = SERVICE.isViolated(awsservice, config, siteId, item);

    // then
    assertFalse(result);
  }

  /**
   * Max Content Length, no Content Length.
   */
  @Test
  public void testIsViolated02() {
    // given
    ConfigService configService = awsservice.getExtension(ConfigService.class);
    String siteId = ID.uuid();

    SiteConfiguration config =
        SiteConfiguration.builder().maxContentLengthBytes("10").build(siteId);
    configService.save(siteId, config);
    DocumentItem item = new DocumentItemDynamoDb();

    // when
    boolean result = SERVICE.isViolated(awsservice, config, siteId, item);

    // then
    assertTrue(result);
  }

  /**
   * Max Content Length, Content Length less than or equal.
   */
  @Test
  public void testIsViolated03() {
    // given
    ConfigService configService = awsservice.getExtension(ConfigService.class);
    Long contentLength = Long.valueOf("10");
    String siteId = ID.uuid();
    DocumentItem item = new DocumentItemDynamoDb();
    item.setContentLength(contentLength);

    SiteConfiguration config =
        SiteConfiguration.builder().maxContentLengthBytes("10").build(siteId);
    configService.save(siteId, config);

    // when
    boolean result = SERVICE.isViolated(awsservice, config, siteId, item);

    // then
    assertFalse(result);
  }

  /**
   * Max Content Length, Content Length greater.
   */
  @Test
  public void testIsViolated04() {
    // given
    ConfigService configService = awsservice.getExtension(ConfigService.class);
    Long contentLength = Long.valueOf("15");
    String siteId = ID.uuid();

    SiteConfiguration config =
        SiteConfiguration.builder().maxContentLengthBytes("10").build(siteId);
    configService.save(siteId, config);
    DocumentItem item = new DocumentItemDynamoDb();
    item.setContentLength(contentLength);

    // when
    boolean result = SERVICE.isViolated(awsservice, config, siteId, item);

    // then
    assertTrue(result);
  }

  /**
   * Max Content Length, Content Length=0.
   */
  @Test
  public void testIsViolated05() {
    // given
    Long contentLength = 0L;
    String siteId = ID.uuid();
    ConfigService configService = awsservice.getExtension(ConfigService.class);

    SiteConfiguration config =
        SiteConfiguration.builder().maxContentLengthBytes("10").build(siteId);
    configService.save(siteId, config);

    DocumentItem item = new DocumentItemDynamoDb();
    item.setContentLength(contentLength);

    // when
    boolean result = SERVICE.isViolated(awsservice, config, siteId, item);

    // then
    assertTrue(result);
  }
}
