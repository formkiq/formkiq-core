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
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_BMP;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_GIF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_JPEG;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_JPG;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_PNG;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_TIF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_TIFF;
import static com.formkiq.aws.dynamodb.objects.MimeType.MIME_WEBP;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sqs.SqsMessageRecords;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.ocr.DocumentOcrServiceExtension;
import com.formkiq.module.ocr.OcrScanStatus;
import com.formkiq.module.ocr.OcrSqsMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sourceforge.tess4j.TesseractException;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;

/** {@link RequestHandler} for handling DynamoDb to Tesseract OCR Processor. */
public class OcrTesseractProcessor implements RequestStreamHandler {

  /** Supported Mime Type. */
  private static final List<MimeType> SUPPORTED = Arrays.asList(MIME_PNG, MIME_JPEG, MIME_JPG,
      MIME_TIF, MIME_TIFF, MIME_GIF, MIME_WEBP, MIME_BMP);
  /** {@link AwsServiceCache}. */
  private AwsServiceCache awsServices;
  /** Documents S3 Bucket. */
  private String documentsBucket;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link String}. */
  private String ocrDocumentsBucket;
  /** {@link S3Service}. */
  private S3Service s3Service;
  /** {@link TesseractWrapper}. */
  private TesseractWrapper tesseract;

  /**
   * constructor.
   * 
   */
  public OcrTesseractProcessor() {
    this(System.getenv(),
        new DynamoDbConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new S3ConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SnsConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setCredentials(EnvironmentVariableCredentialsProvider.create())
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new TesseractWrapperImpl());
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param dbConnection {@link DynamoDbConnectionBuilder}
   * @param s3Connection {@link S3ConnectionBuilder}
   * @param snsConnection {@link SnsConnectionBuilder}
   * @param tesseractWrapper {@link TesseractWrapper}
   */
  public OcrTesseractProcessor(final Map<String, String> map,
      final DynamoDbConnectionBuilder dbConnection, final S3ConnectionBuilder s3Connection,
      final SnsConnectionBuilder snsConnection, final TesseractWrapper tesseractWrapper) {

    this.s3Service = new S3Service(s3Connection);
    this.documentsBucket = map.get("DOCUMENTS_S3_BUCKET");
    this.ocrDocumentsBucket = map.get("OCR_S3_BUCKET");
    this.tesseract = tesseractWrapper;

    this.awsServices =
        new AwsServiceCache().environment(map).debug("true".equals(map.get("DEBUG")));

    AwsServiceCache.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(dbConnection));
    AwsServiceCache.register(S3Service.class, new S3ServiceExtension(s3Connection));
    AwsServiceCache.register(DocumentOcrService.class, new DocumentOcrServiceExtension());
    AwsServiceCache.register(ActionsService.class, new ActionsServiceExtension());

    AwsServiceCache.register(ActionsNotificationService.class,
        new ActionsNotificationServiceExtension(snsConnection));
  }

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache getAwsServices() {
    return this.awsServices;
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

      OcrSqsMessage sqsMessage = this.gson.fromJson(record.body(), OcrSqsMessage.class);
      processRecord(logger, ocrService, sqsMessage);

    });
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

      if (!SUPPORTED.contains(mt)) {
        throw new IOException("unsupported Content-Type: " + contentType);
      }

      String documentS3Key = createS3Key(siteId, documentId);
      File file = new File("/tmp/" + documentS3Key.replaceAll("/", "_") + "." + mt.getExtension());

      try (InputStream is =
          this.s3Service.getContentAsInputStream(this.documentsBucket, documentS3Key)) {

        try (OutputStream fileOs = new FileOutputStream(file)) {
          IoUtils.copy(is, fileOs);
        }

        String text = this.tesseract.doOcr(file);

        String ocrS3Key = ocrService.getS3Key(siteId, documentId, jobId);
        this.s3Service.putObject(this.ocrDocumentsBucket, ocrS3Key,
            text.getBytes(StandardCharsets.UTF_8), "text/plain");

        ocrService.updateOcrScanStatus(this.awsServices, siteId, documentId,
            OcrScanStatus.SUCCESSFUL);

        logger.log(String.format("setting OCR Scan Status: %s", OcrScanStatus.SUCCESSFUL));

      } finally {

        if (!file.delete()) {
          file.deleteOnExit();
        }
      }

    } catch (IOException | TesseractException | RuntimeException e) {
      e.printStackTrace();
      ocrService.updateOcrScanStatus(siteId, documentId, OcrScanStatus.FAILED);
      logger.log(String.format("setting OCR Scan Status: %s", OcrScanStatus.FAILED));

      ActionsService actionsService = this.awsServices.getExtension(ActionsService.class);
      actionsService.updateActionStatus(siteId, documentId, ActionType.OCR, ActionStatus.FAILED);
    }
  }
}
