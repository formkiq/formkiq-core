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
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3PresignerServiceExtension;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.services.lambda.AbstractRestApiRequestHandler;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sqs.events.SqsEventRecord;
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
import com.formkiq.module.lambda.ocr.handlers.ObjectExaminePdfHandler;
import com.formkiq.module.lambda.ocr.handlers.ObjectExaminePdfIdHandler;
import com.formkiq.module.lambda.ocr.pdf.PdfFormatConverter;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.ocr.DocumentOcrServiceExtension;
import com.formkiq.module.ocr.FormatConverter;
import com.formkiq.module.ocr.FormatConverterResult;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.module.ocr.OcrSqsMessage;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

/** {@link RequestHandler} for handling DynamoDb to Tesseract OCR Processor. */
public class OcrTesseractProcessor extends AbstractRestApiRequestHandler {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;
  /** Url Class Map. */
  private static final Map<String, ApiGatewayRequestHandler> URL_MAP = new HashMap<>();

  static {

    if (System.getenv().containsKey("AWS_REGION")
        && !System.getenv().containsKey("TEXTRACT_ROLE")) {
      serviceCache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
              new SnsAwsServiceRegistry())
          .build();

      initialize(serviceCache);
    }
  }

  /**
   * Add Url Request Handler Mapping.
   * 
   * @param handler {@link ApiGatewayRequestHandler}
   */
  private static void addRequestHandler(final ApiGatewayRequestHandler handler) {
    URL_MAP.put(handler.getRequestUrl(), handler);
  }

  /**
   * Initialize.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   */
  protected static void initialize(final AwsServiceCache awsServiceCache) {
    awsServiceCache.register(S3Service.class, new S3ServiceExtension());
    awsServiceCache.register(DocumentOcrService.class, new DocumentOcrServiceExtension());
    awsServiceCache.register(ActionsService.class, new ActionsServiceExtension());

    awsServiceCache.register(EventService.class, new EventServiceSnsExtension());
    awsServiceCache.register(ActionsNotificationService.class,
        new ActionsNotificationServiceExtension());
    awsServiceCache.register(S3PresignerService.class, new S3PresignerServiceExtension());

    serviceCache = awsServiceCache;

    addRequestHandler(new ObjectExaminePdfHandler());
    addRequestHandler(new ObjectExaminePdfIdHandler());
  }

  /** {@link List} {@link FormatConverter}. */
  private List<FormatConverter> converters;

  /**
   * constructor.
   * 
   */
  public OcrTesseractProcessor() {}

  /**
   * constructor.
   *
   * @param services {@link DynamoDbConnectionBuilder}
   */
  public OcrTesseractProcessor(final AwsServiceCache services) {
    initialize(services);
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

  @Override
  public AwsServiceCache getAwsServices() {
    return serviceCache;
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

  private List<String> getParserTypes(final OcrSqsMessage sqsMessage) {
    List<String> parseTypes = Collections.emptyList();
    if (sqsMessage.request() != null) {
      parseTypes = notNull(sqsMessage.request().getParseTypes());
    }
    return parseTypes;
  }

  protected OcrSqsMessage getSqsMessage(final String body) {
    return this.gson.fromJson(body, OcrSqsMessage.class);
  }

  @Override
  public Map<String, ApiGatewayRequestHandler> getUrlMap() {
    return URL_MAP;
  }

  @Override
  public void handleSqsRequest(final Logger logger, final AwsServiceCache awsServices,
      final SqsEventRecord record) {

    DocumentOcrService ocrService = serviceCache.getExtension(DocumentOcrService.class);
    OcrSqsMessage sqsMessage = getSqsMessage(record.body());
    processRecord(logger, awsServices, ocrService, sqsMessage);
  }

  protected File loadFile(final AwsServiceCache awsServices, final OcrSqsMessage sqsMessage,
      final MimeType mt) throws IOException {

    S3Service s3Service = serviceCache.getExtension(S3Service.class);

    String siteId = sqsMessage.siteId();
    String documentId = sqsMessage.documentId();

    String tmpDirectory =
        new File("/tmp").exists() ? "/tmp/" : System.getProperty("java.io.tmpdir") + "\\";
    String documentS3Key = createS3Key(siteId, documentId);
    File file =
        new File(tmpDirectory + documentS3Key.replaceAll("/", "_") + "." + mt.getExtension());

    String documentsBucket = awsServices.environment("DOCUMENTS_S3_BUCKET");

    try (InputStream is = s3Service.getContentAsInputStream(documentsBucket, documentS3Key)) {

      try (OutputStream fileOs = new FileOutputStream(file)) {
        IoUtils.copy(is, fileOs);
      }
    }

    return file;
  }

  private void logProcessRecord(final Logger logger, final OcrSqsMessage sqsMessage,
      final String siteId, final String documentId, final String jobId, final String contentType) {

    List<String> parseTypes = getParserTypes(sqsMessage);

    String s = String.format(
        "{\"siteId\": \"%s\",\"documentId\": \"%s\",\"jobId\": \"%s\",\"contentType\":\"%s\","
            + "\"parseTypes\":\"%s\"}",
        siteId, documentId, jobId, contentType, String.join(", ", parseTypes));
    logger.info(s);
  }

  private void processRecord(final Logger logger, final AwsServiceCache awsServices,
      final DocumentOcrService ocrService, final OcrSqsMessage sqsMessage) {

    String siteId = sqsMessage.siteId();
    String documentId = sqsMessage.documentId();
    String jobId = sqsMessage.jobId();
    String contentType = sqsMessage.contentType();

    logProcessRecord(logger, sqsMessage, siteId, documentId, jobId, contentType);

    try {

      MimeType mt = MimeType.fromContentType(contentType);

      Optional<FormatConverter> fc =
          getConverters().stream().filter(c -> c.isSupported(sqsMessage, mt)).findFirst();

      if (fc.isEmpty()) {
        throw new IOException("unsupported Content-Type: " + contentType);
      }

      File file = loadFile(awsServices, sqsMessage, mt);

      try {

        FormatConverterResult result = fc.get().convert(serviceCache, sqsMessage, mt, file);

        if (result.text() != null) {

          S3Service s3Service = serviceCache.getExtension(S3Service.class);
          String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);

          String ocrDocumentsBucket = awsServices.environment("OCR_S3_BUCKET");

          s3Service.putObject(ocrDocumentsBucket, ocrS3Key,
              result.text().getBytes(StandardCharsets.UTF_8), "text/plain");
        }

        if (OcrScanStatus.SUCCESSFUL.equals(result.status())) {
          ocrService.updateOcrScanStatus(serviceCache, siteId, documentId,
              OcrScanStatus.SUCCESSFUL);
        }

      } finally {

        if (file != null && !file.delete()) {
          file.deleteOnExit();
        }
      }

    } catch (IOException | RuntimeException e) {

      ocrService.updateOcrScanStatus(siteId, documentId, OcrScanStatus.FAILED);

      logger.error(String.format("setting OCR Scan Status: %s", OcrScanStatus.FAILED));
      logger.error(e);

      ActionsService actionsService = serviceCache.getExtension(ActionsService.class);
      List<Action> actions = actionsService.getActions(siteId, documentId);
      Optional<Action> o = actions.stream().filter(new ActionStatusPredicate(ActionStatus.RUNNING))
          .filter(new ActionTypePredicate(ActionType.OCR)).findFirst();

      o.ifPresent(action -> {
        action.status(ActionStatus.FAILED);
        action.message(e.getMessage());
        actionsService.updateActionStatus(siteId, documentId, action);
      });
    }
  }
}
