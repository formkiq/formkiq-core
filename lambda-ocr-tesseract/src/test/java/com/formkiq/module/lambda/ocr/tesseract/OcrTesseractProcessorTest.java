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
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.OCR_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsMessageRecord;
import com.formkiq.aws.sqs.SqsMessageRecords;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambda.ocr.docx.DocFormatConverter;
import com.formkiq.module.lambda.ocr.docx.DocxFormatConverter;
import com.formkiq.module.lambda.ocr.pdf.PdfFormatConverter;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.ocr.Ocr;
import com.formkiq.module.ocr.OcrEngine;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

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

    Map<String, String> map = Map.of("AWS_REGION", AWS_REGION.toString(), "DOCUMENTS_TABLE",
        DOCUMENTS_TABLE, "DOCUMENTS_S3_BUCKET", BUCKET_NAME, "OCR_S3_BUCKET", OCR_BUCKET_NAME,
        "SNS_DOCUMENT_EVENT", "");

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    AwsServiceCache services = new AwsServiceCacheBuilder(map, TestServices.getEndpointMap(), cred)
        .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
            new SnsAwsServiceRegistry())
        .build();

    TesseractWrapperData wrapper = new TesseractWrapperData(OCR_TEXT);
    processor = new OcrTesseractProcessor(services, Arrays.asList(new DocxFormatConverter(),
        new DocFormatConverter(), new PdfFormatConverter(), new TesseractFormatConverter(wrapper)));

    ocrService = services.getExtension(DocumentOcrService.class);
    s3 = services.getExtension(S3Service.class);
    actionsService = services.getExtension(ActionsService.class);
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

      List<Action> actions = Arrays
          .asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.TESSERACT)
          .status(OcrScanStatus.REQUESTED);
      ocrService.save(siteId, ocr);

      SqsMessageRecord record = new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId,
          "documentId", documentId, "jobId", jobId, "contentType", MimeType.MIME_JPEG)));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      Ocr obj = ocrService.get(siteId, documentId);
      assertEquals("FAILED", obj.status().name());

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

      List<Action> actions = Arrays
          .asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      String documentS3Key = createS3Key(siteId, documentId);
      s3.putObject(BUCKET_NAME, documentS3Key, "testdata".getBytes(StandardCharsets.UTF_8),
          "text/plain");

      Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.TESSERACT)
          .status(OcrScanStatus.REQUESTED);
      ocrService.save(siteId, ocr);

      SqsMessageRecord record =
          new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId, "documentId", documentId,
              "jobId", jobId, "contentType", MimeType.MIME_JPEG.getContentType())));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      Ocr obj = ocrService.get(siteId, documentId);
      assertEquals("SUCCESSFUL", obj.status().name());

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

      List<Action> actions = Arrays
          .asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.TESSERACT)
          .status(OcrScanStatus.REQUESTED);
      ocrService.save(siteId, ocr);

      SqsMessageRecord record = new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId,
          "documentId", documentId, "jobId", jobId, "contentType", MimeType.MIME_DOCX)));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      Ocr obj = ocrService.get(siteId, documentId);
      assertEquals("FAILED", obj.status().name());

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.FAILED, actions.get(0).status());
    }
  }

  /**
   * Test Successful DOCX OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest04() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      String jobId = UUID.randomUUID().toString();

      List<Action> actions = Arrays
          .asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      String documentS3Key = createS3Key(siteId, documentId);
      try (InputStream is =
          LambdaContextRecorder.class.getResourceAsStream("/file-sample_100kB.docx")) {
        s3.putObject(BUCKET_NAME, documentS3Key, is, MimeType.MIME_DOCX.getContentType());
      }

      Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.TESSERACT)
          .status(OcrScanStatus.REQUESTED);
      ocrService.save(siteId, ocr);

      SqsMessageRecord record =
          new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId, "documentId", documentId,
              "jobId", jobId, "contentType", MimeType.MIME_DOCX.getContentType())));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      Ocr obj = ocrService.get(siteId, documentId);
      assertEquals("SUCCESSFUL", obj.status().name());

      String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);
      assertTrue(s3.getContentAsString(OCR_BUCKET_NAME, ocrS3Key, null)
          .contains("Vestibulum neque massa"));

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
    }
  }

  /**
   * Test Successful DOC OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest05() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      String jobId = UUID.randomUUID().toString();

      List<Action> actions = Arrays
          .asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      String documentS3Key = createS3Key(siteId, documentId);
      try (InputStream is =
          LambdaContextRecorder.class.getResourceAsStream("/file-sample_100kB.doc")) {
        s3.putObject(BUCKET_NAME, documentS3Key, is, MimeType.MIME_DOC.getContentType());
      }

      Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.TESSERACT)
          .status(OcrScanStatus.REQUESTED);
      ocrService.save(siteId, ocr);

      SqsMessageRecord record =
          new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId, "documentId", documentId,
              "jobId", jobId, "contentType", MimeType.MIME_DOC.getContentType())));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      Ocr obj = ocrService.get(siteId, documentId);
      assertEquals("SUCCESSFUL", obj.status().name());

      String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);
      assertTrue(s3.getContentAsString(OCR_BUCKET_NAME, ocrS3Key, null)
          .contains("Vestibulum neque massa"));

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
    }
  }

  /**
   * Test Successful application/pdf OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest06() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      String jobId = UUID.randomUUID().toString();

      List<Action> actions = Arrays
          .asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      String documentS3Key = createS3Key(siteId, documentId);
      try (InputStream is = LambdaContextRecorder.class.getResourceAsStream("/sample.pdf")) {
        s3.putObject(BUCKET_NAME, documentS3Key, is, MimeType.MIME_PDF.getContentType());
      }

      Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.TESSERACT)
          .status(OcrScanStatus.REQUESTED);
      ocrService.save(siteId, ocr);

      SqsMessageRecord record =
          new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId, "documentId", documentId,
              "jobId", jobId, "contentType", MimeType.MIME_PDF.getContentType())));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      Ocr obj = ocrService.get(siteId, documentId);
      assertEquals("SUCCESSFUL", obj.status().name());

      String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);
      assertTrue(s3.getContentAsString(OCR_BUCKET_NAME, ocrS3Key, null).contains("And more text"));

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
    }
  }

  /**
   * Test Successful PDF Portfolio application/pdf OCR.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest07() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      String jobId = UUID.randomUUID().toString();

      List<Action> actions = Arrays
          .asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      String documentS3Key = createS3Key(siteId, documentId);
      try (InputStream is = LambdaContextRecorder.class.getResourceAsStream("/collection.pdf")) {
        s3.putObject(BUCKET_NAME, documentS3Key, is, MimeType.MIME_PDF.getContentType());
      }

      Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.TESSERACT)
          .status(OcrScanStatus.REQUESTED);
      ocrService.save(siteId, ocr);

      SqsMessageRecord record =
          new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId, "documentId", documentId,
              "jobId", jobId, "contentType", MimeType.MIME_PDF.getContentType())));
      SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(records);
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
      Ocr obj = ocrService.get(siteId, documentId);
      assertEquals("SUCCESSFUL", obj.status().name());

      String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);
      assertTrue(s3.getContentAsString(OCR_BUCKET_NAME, ocrS3Key, null).contains("And more text"));

      actions = actionsService.getActions(siteId, documentId);
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
    }
  }
}
