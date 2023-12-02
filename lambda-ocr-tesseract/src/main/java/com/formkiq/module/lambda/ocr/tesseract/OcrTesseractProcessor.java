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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsMessageRecord;
import com.formkiq.aws.sqs.SqsMessageRecords;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionStatusPredicate;
import com.formkiq.module.actions.services.ActionTypePredicate;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.EventServiceSnsExtension;
import com.formkiq.module.lambda.ocr.docx.DocFormatConverter;
import com.formkiq.module.lambda.ocr.docx.DocxFormatConverter;
import com.formkiq.module.lambda.ocr.pdf.PdfFormatConverter;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.ocr.DocumentOcrServiceExtension;
import com.formkiq.module.ocr.FormatConverter;
import com.formkiq.module.ocr.FormatConverterResult;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.module.ocr.OcrSqsMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

/** {@link RequestHandler} for handling DynamoDb to Tesseract OCR Processor. */
public class OcrTesseractProcessor implements RequestStreamHandler {

  /** {@link AwsServiceCache}. */
  private AwsServiceCache awsServices;
  /** {@link List} {@link FormatConverter}. */
  private List<FormatConverter> converters;
  /** Documents S3 Bucket. */
  private String documentsBucket;
  /** {@link Gson}. */
  protected Gson gson = new GsonBuilder().create();
  /** {@link String}. */
  private String ocrDocumentsBucket;
  /** {@link S3Service}. */
  private S3Service s3Service;

  /**
   * constructor.
   * 
   */
  public OcrTesseractProcessor() {
    this(new AwsServiceCacheBuilder(System.getenv(), Map.of(),
        EnvironmentVariableCredentialsProvider.create())
        .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
            new SnsAwsServiceRegistry())
        .build());
  }

  /**
   * constructor.
   *
   * @param services {@link DynamoDbConnectionBuilder}
   */
  public OcrTesseractProcessor(final AwsServiceCache services) {

    this.documentsBucket = services.environment("DOCUMENTS_S3_BUCKET");
    this.ocrDocumentsBucket = services.environment("OCR_S3_BUCKET");

    register(services);
    this.awsServices = services;
  }

  /**
   * constructor.
   * 
   * @param services {@link AwsServiceCache}
   * @param converterList {@link List} {@link FormatConverter}
   */
  public OcrTesseractProcessor(final AwsServiceCache services,
      final List<FormatConverter> converterList) {
    this(services);
    this.converters = converterList;
  }

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache getAwsServices() {
    return this.awsServices;
  }

  /**
   * Get Converters.
   * 
   * @return {@link List} {@link FormatConverter}
   */
  protected List<FormatConverter> getConverters() {

    if (this.converters == null) {
      this.converters = getDefaultConverters();
    }

    return this.converters;
  }

  /**
   * Get Default Converters.
   * 
   * @return {@link List} {@link FormatConverter}
   */
  protected List<FormatConverter> getDefaultConverters() {
    return Arrays.asList(new DocxFormatConverter(), new DocFormatConverter(),
        new PdfFormatConverter(), new TesseractFormatConverter(new TesseractWrapperImpl()));
  }

  protected OcrSqsMessage getSqsMessage(final SqsMessageRecord record) {
    OcrSqsMessage sqsMessage = this.gson.fromJson(record.body(), OcrSqsMessage.class);
    return sqsMessage;
  }

  @Override
  public void handleRequest(final InputStream input, final OutputStream output,
      final Context context) throws IOException {

    LambdaLogger logger = context.getLogger();

    String json = IoUtils.toUtf8String(input);

    if (this.awsServices.debug()) {
      logger.log(json);
    }

    SqsMessageRecords records = this.gson.fromJson(json, SqsMessageRecords.class);

    DocumentOcrService ocrService = this.awsServices.getExtension(DocumentOcrService.class);

    records.records().forEach(record -> {

      OcrSqsMessage sqsMessage = getSqsMessage(record);
      processRecord(logger, ocrService, sqsMessage);

    });
  }

  protected File loadFile(final OcrSqsMessage sqsMessage, final MimeType mt) throws IOException {

    String siteId = sqsMessage.siteId();
    String documentId = sqsMessage.documentId();

    String tmpDirectory =
        new File("/tmp").exists() ? "/tmp/" : System.getProperty("java.io.tmpdir") + "\\";
    String documentS3Key = createS3Key(siteId, documentId);
    File file =
        new File(tmpDirectory + documentS3Key.replaceAll("/", "_") + "." + mt.getExtension());

    try (InputStream is =
        this.s3Service.getContentAsInputStream(this.documentsBucket, documentS3Key)) {

      try (OutputStream fileOs = new FileOutputStream(file)) {
        IoUtils.copy(is, fileOs);
      }
    }

    return file;
  }

  private void processRecord(final LambdaLogger logger, final DocumentOcrService ocrService,
      final OcrSqsMessage sqsMessage) {

    String siteId = sqsMessage.siteId();
    String documentId = sqsMessage.documentId();
    String jobId = sqsMessage.jobId();
    String contentType = sqsMessage.contentType();

    String s = String.format(
        "{\"siteId\": \"%s\",\"documentId\": \"%s\",\"jobId\": \"%s\",\"contentType\":\"%s\"}",
        siteId, documentId, jobId, contentType);
    logger.log(s);

    try {

      MimeType mt = MimeType.fromContentType(contentType);

      Optional<FormatConverter> fc =
          getConverters().stream().filter(c -> c.isSupported(sqsMessage, mt)).findFirst();

      if (fc.isEmpty()) {
        throw new IOException("unsupported Content-Type: " + contentType);
      }

      File file = loadFile(sqsMessage, mt);

      try {

        FormatConverterResult result = fc.get().convert(this.awsServices, sqsMessage, file);

        if (result.text() != null) {
          String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);
          this.s3Service.putObject(this.ocrDocumentsBucket, ocrS3Key,
              result.text().getBytes(StandardCharsets.UTF_8), "text/plain");
        }

        if (OcrScanStatus.SUCCESSFUL.equals(result.status())) {
          ocrService.updateOcrScanStatus(this.awsServices, siteId, documentId,
              OcrScanStatus.SUCCESSFUL);
        }

      } finally {

        if (file != null && !file.delete()) {
          file.deleteOnExit();
        }
      }

    } catch (IOException | RuntimeException e) {
      e.printStackTrace();
      ocrService.updateOcrScanStatus(siteId, documentId, OcrScanStatus.FAILED);
      logger.log(String.format("setting OCR Scan Status: %s", OcrScanStatus.FAILED));

      ActionsService actionsService = this.awsServices.getExtension(ActionsService.class);
      List<Action> actions = actionsService.getActions(siteId, documentId);
      Optional<Action> o = actions.stream().filter(new ActionStatusPredicate(ActionStatus.RUNNING))
          .filter(new ActionTypePredicate(ActionType.OCR)).findFirst();

      if (o.isPresent()) {
        o.get().status(ActionStatus.FAILED);
        actionsService.updateActionStatus(siteId, documentId, o.get());
      }
    }
  }

  /**
   * Register Compoments.
   * 
   * @param services {@link AwsServiceCache}
   */
  protected void register(final AwsServiceCache services) {

    services.register(S3Service.class, new S3ServiceExtension());
    services.register(DocumentOcrService.class, new DocumentOcrServiceExtension());
    services.register(ActionsService.class, new ActionsServiceExtension());

    services.register(EventService.class, new EventServiceSnsExtension());
    services.register(ActionsNotificationService.class, new ActionsNotificationServiceExtension());

    this.s3Service = services.getExtension(S3Service.class);
  }
}
