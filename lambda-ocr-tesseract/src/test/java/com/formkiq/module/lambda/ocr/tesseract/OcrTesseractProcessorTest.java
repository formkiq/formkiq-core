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
package com.formkiq.module.lambda.ocr.tesseract;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.OCR_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sqs.SqsMessageRecord;
import com.formkiq.aws.sqs.SqsMessageRecords;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.ocr.Ocr;
import com.formkiq.module.ocr.OcrEngine;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * Unit Tests {@link OcrTesseractProcessor}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
class OcrTesseractProcessorTest {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().create();
  /** Ocr Text. */
  private static final String OCR_TEXT = "this is test data";
  /** {@link DocumentOcrService}. */
  private static DocumentOcrService ocrService;
  /** {@link OcrTesseractProcessor}. */
  private static OcrTesseractProcessor processor;
  /** {@link S3Service}. */
  private static S3Service s3;

  @BeforeAll
  public static void beforeAll() throws Exception {

    DynamoDbConnectionBuilder dbConnection = DynamoDbTestServices.getDynamoDbConnection();
    S3ConnectionBuilder s3Connection = TestServices.getS3Connection(null);
    SnsConnectionBuilder sns = TestServices.getSnsConnection(null);

    TesseractWrapperData wrapper = new TesseractWrapperData(OCR_TEXT);
    processor = new OcrTesseractProcessor(Map.of("DOCUMENTS_TABLE", DOCUMENTS_TABLE,
        "DOCUMENTS_S3_BUCKET", BUCKET_NAME, "OCR_S3_BUCKET", OCR_BUCKET_NAME), dbConnection,
        s3Connection, sns, wrapper);

    AwsServiceCache awsServices = processor.getAwsServices();
    ocrService = awsServices.getExtension(DocumentOcrService.class);
    s3 = awsServices.getExtension(S3Service.class);
    actionsService = awsServices.getExtension(ActionsService.class);
  }

  /** {@link Context}. */
  private Context context = new LambdaContextRecorder();

  /**
   * Test S3 File doesn't exist.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest01() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      String jobId = UUID.randomUUID().toString();

      List<Action> actions = Arrays.asList(new Action().type(ActionType.OCR));
      actionsService.saveActions(siteId, documentId, actions);

      Ocr ocr = new Ocr().siteId(siteId).documentId(documentId).jobId(jobId)
          .engine(OcrEngine.TESSERACT).status(OcrScanStatus.REQUESTED);
      ocrService.save(ocr);

      SqsMessageRecord record = new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId,
          "documentId", documentId, "jobId", jobId, "contentType", MimeType.MIME_JPEG)));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      DynamicObject obj = ocrService.get(siteId, documentId);
      assertEquals("failed", obj.get("ocrStatus"));

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.FAILED, actions.get(0).status());
    }
  }

  /**
   * Test Successful OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest02() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      String jobId = UUID.randomUUID().toString();

      List<Action> actions = Arrays.asList(new Action().type(ActionType.OCR));
      actionsService.saveActions(siteId, documentId, actions);

      String documentS3Key = createS3Key(siteId, documentId);
      s3.putObject(BUCKET_NAME, documentS3Key, "testdata".getBytes(StandardCharsets.UTF_8),
          "text/plain");

      Ocr ocr = new Ocr().siteId(siteId).documentId(documentId).jobId(jobId)
          .engine(OcrEngine.TESSERACT).status(OcrScanStatus.REQUESTED);
      ocrService.save(ocr);

      SqsMessageRecord record =
          new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId, "documentId", documentId,
              "jobId", jobId, "contentType", MimeType.MIME_JPEG.getContentType())));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      DynamicObject obj = ocrService.get(siteId, documentId);
      assertEquals("successful", obj.get("ocrStatus"));

      String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);
      assertEquals(OCR_TEXT, s3.getContentAsString(OCR_BUCKET_NAME, ocrS3Key, null));

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
    }
  }

  /**
   * Test Content Type unknown.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest03() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      String jobId = UUID.randomUUID().toString();

      List<Action> actions = Arrays.asList(new Action().type(ActionType.OCR));
      actionsService.saveActions(siteId, documentId, actions);

      Ocr ocr = new Ocr().siteId(siteId).documentId(documentId).jobId(jobId)
          .engine(OcrEngine.TESSERACT).status(OcrScanStatus.REQUESTED);
      ocrService.save(ocr);

      SqsMessageRecord record = new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId,
          "documentId", documentId, "jobId", jobId, "contentType", MimeType.MIME_DOCX)));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      DynamicObject obj = ocrService.get(siteId, documentId);
      assertEquals("failed", obj.get("ocrStatus"));

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.FAILED, actions.get(0).status());
    }
  }

}
