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
package com.formkiq.module.ocr;

import static com.formkiq.module.ocr.DocumentOcrService.CONFIG_OCR_COUNT;

import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationOcr;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.OCR_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link DocumentOcrServiceTesseract}.
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentOcrServiceTesseractTest {

  /** {@link DocumentOcrServiceTesseract}. */
  private static DocumentOcrServiceTesseract service;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;
  /** {@link ConfigService}. */
  private static ConfigService configService;

  /**
   * BeforeAll.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {

    Map<String, String> env = new HashMap<>(
        Map.of("AWS_REGION", Region.US_EAST_1.id(), "DOCUMENTS_TABLE", DOCUMENTS_TABLE));

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    serviceCache = new AwsServiceCacheBuilder(env, TestServices.getEndpointMap(), cred)
        .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
            new SqsAwsServiceRegistry())
        .build();

    serviceCache.register(ConfigService.class, new ConfigServiceExtension());
    serviceCache.register(S3Service.class, new S3ServiceExtension());
    serviceCache.register(SqsService.class, new SqsServiceExtension());

    SqsService sqs = serviceCache.getExtension(SqsService.class);
    String queueUrl = sqs.createQueue("test_" + ID.uuid()).queueUrl();
    env.put("OCR_SQS_QUEUE_URL", queueUrl);
    serviceCache.environment(env);

    DynamoDbConnectionBuilder db = DynamoDbTestServices.getDynamoDbConnection();
    S3Service s3 = serviceCache.getExtension(S3Service.class);

    service =
        new DocumentOcrServiceTesseract(db, DOCUMENTS_TABLE, s3, OCR_BUCKET_NAME, BUCKET_NAME);
    configService = serviceCache.getExtension(ConfigService.class);
  }

  /**
   * Test config max transactions.
   */
  @Test
  void testConvert01() {
    // given
    final String userId = "joe";
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      configService.save(siteId, new SiteConfiguration()
          .setOcr(new SiteConfigurationOcr().setMaxTransactions(1).setMaxPagesPerTransaction(2)));

      assertEquals(-1, configService.getIncrement(siteId, CONFIG_OCR_COUNT));

      String documentId = ID.uuid();
      OcrRequest request = new OcrRequest();

      // when
      boolean result = service.convert(serviceCache, request, siteId, documentId, userId);

      // then
      assertTrue(result);
      assertEquals(1, configService.getIncrement(siteId, CONFIG_OCR_COUNT));

      // when
      result = service.convert(serviceCache, request, siteId, documentId, userId);

      // then
      assertFalse(result);
      assertEquals(1, configService.getIncrement(siteId, CONFIG_OCR_COUNT));
    }
  }

  /**
   * Test config max pages per transaction.
   */
  @Test
  void testConvert02() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      OcrRequest request = convert(siteId, null, 2);

      // then
      assertEquals("2", request.getOcrNumberOfPages());

      // given
      final int maxPages = 3;

      // when
      request = convert(siteId, "5", maxPages);

      // then
      assertEquals("3", request.getOcrNumberOfPages());
    }
  }

  private OcrRequest convert(final String siteId, final String numberOfPages,
      final long maxPagesPerTransaction) {

    final String userId = "joe";
    configService.save(siteId, new SiteConfiguration().setOcr(new SiteConfigurationOcr()
        .setMaxTransactions(1).setMaxPagesPerTransaction(maxPagesPerTransaction)));

    String documentId = ID.uuid();
    OcrRequest request = new OcrRequest().setOcrNumberOfPages(numberOfPages);

    // when
    service.convert(serviceCache, request, siteId, documentId, userId);
    return request;
  }
}
