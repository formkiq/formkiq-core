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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionStatusPredicate;
import com.formkiq.module.actions.services.ActionTypePredicate;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationOcr;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * 
 * DynamoDb implementation of {@link DocumentOcrService}.
 *
 */
public class DocumentOcrServiceTesseract implements DocumentOcrService, DbKeys {

  /** Content Type APPLICATION/JSON. */
  private static final String APPLICATION_JSON = "application/json";
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link String}. */
  private final String documentsBucket;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  /** OCR S3 Bucket. */
  private final String ocrBucket;
  /** {@link S3Service}. */
  private final S3Service s3;


  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param ocrTable {@link String}
   * @param s3Service {@link S3Service}
   * @param ocrS3Bucket {@link String}
   * @param documentsS3Bucket {@link String}
   */
  public DocumentOcrServiceTesseract(final DynamoDbConnectionBuilder connection,
      final String ocrTable, final S3Service s3Service, final String ocrS3Bucket,
      final String documentsS3Bucket) {

    if (ocrTable == null) {
      throw new IllegalArgumentException("'ocrTable' is null");
    }

    if (ocrS3Bucket == null) {
      throw new IllegalArgumentException("'ocrS3Bucket' is null");
    }

    if (documentsS3Bucket == null) {
      throw new IllegalArgumentException("'documentsS3Bucket' is null");
    }

    this.db = new DynamoDbServiceImpl(connection, ocrTable);
    this.s3 = s3Service;
    this.ocrBucket = ocrS3Bucket;
    this.documentsBucket = documentsS3Bucket;
  }

  @Override
  public boolean convert(final AwsServiceCache awsservice, final OcrRequest request,
      final String siteId, final String documentId, final String userId) {

    boolean valid =
        isValidOcrConfiguration(awsservice.getExtension(ConfigService.class), siteId, request);

    if (valid) {

      String s3key = createS3Key(siteId, documentId);

      String contentType = getS3FileContentType(this.documentsBucket, s3key);

      Logger log = awsservice.getLogger();

      if (MimeType.isPlainText(contentType)) {

        if (log.isLogged(LogLevel.TRACE)) {
          String msg = String.format("saving text document %s in bucket %s by user %s", s3key,
              this.documentsBucket, userId);
          log.trace(msg);
        }

        updatePlainText(awsservice, siteId, documentId, userId, s3key, contentType);

      } else {

        String documentS3toConvert = null;

        if (log.isLogged(LogLevel.TRACE)) {
          String msg = String.format("converting document %s in bucket %s by user %s", s3key,
              this.documentsBucket, userId);
          log.trace(msg);
        }

        String jobId = convertDocument(awsservice, request, siteId, documentId, s3key,
            documentS3toConvert, contentType);

        Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(getOcrEngine(request))
            .status(OcrScanStatus.REQUESTED).contentType(APPLICATION_JSON).userId(userId)
            .addPdfDetectedCharactersAsText(request.isAddPdfDetectedCharactersAsText())
            .ocrOutputType(request.getOcrOutputType());

        save(siteId, ocr);
      }
    }

    return valid;
  }

  private boolean isValidOcrConfiguration(final ConfigService configService, final String siteId,
      final OcrRequest request) {
    boolean invalid = false;

    SiteConfiguration config = configService.get(siteId);
    SiteConfigurationOcr ocr = config.getOcr();

    if (ocr != null) {

      if (ocr.getMaxTransactions() > 0) {

        long count = configService.getIncrement(siteId, CONFIG_OCR_COUNT);
        if (count >= ocr.getMaxTransactions()) {
          invalid = true;
        } else {
          configService.increment(siteId, CONFIG_OCR_COUNT);
        }
      }

      if (ocr.getMaxPagesPerTransaction() > 0
          && (getOcrNumberOfPages(request) > ocr.getMaxPagesPerTransaction())) {
        request.setOcrNumberOfPages("" + ocr.getMaxPagesPerTransaction());
      }
    }

    return !invalid;
  }

  private long getOcrNumberOfPages(final OcrRequest request) {
    try {
      return Long.parseLong(request.getOcrNumberOfPages());
    } catch (NumberFormatException e) {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public void delete(final String siteId, final String documentId) {

    String prefix = getS3Key(siteId, documentId, null);

    ListObjectsResponse response = this.s3.listObjects(this.ocrBucket, prefix);
    response.contents().forEach(resp -> this.s3.deleteObject(this.ocrBucket, resp.key(), null));

    Map<String, AttributeValue> map = keysDocumentOcr(siteId, documentId);
    this.db.deleteItem(map.get(PK), map.get(SK));
  }

  @Override
  public Ocr get(final String siteId, final String documentId) {

    Ocr ocr = new Ocr().documentId(documentId);

    AttributeValue pk = AttributeValue.fromS(ocr.pk(siteId));
    AttributeValue sk = AttributeValue.fromS(ocr.sk());
    Map<String, AttributeValue> result = this.db.get(pk, sk);

    if (!result.isEmpty()) {
      ocr = new Ocr().getFromAttributes(siteId, result);
    } else {
      ocr = null;
    }

    return ocr;
  }

  @Override
  public List<String> getOcrS3Keys(final String siteId, final String documentId,
      final String jobId) {

    String prefix = getS3Key(siteId, documentId, jobId);

    ListObjectsResponse list = this.s3.listObjects(this.ocrBucket, prefix);

    return list.contents().stream().map(S3Object::key)
        .filter(key -> !key.contains(".s3_access_check")).sorted(new S3KeysNaturalComparator())
        .collect(Collectors.toList());
  }

  @Override
  public String getS3Key(final String siteId, final String documentId, final String jobId) {
    return SiteIdKeyGenerator.createS3Key(siteId, documentId);
  }

  @Override
  public void save(final String siteId, final Ocr ocr) {
    this.db.putItem(ocr.getAttributes(siteId));
  }

  @Override
  public void set(final AwsServiceCache awsservice, final String siteId, final String documentId,
      final String userId, final String content, final String contentType) {

    this.delete(siteId, documentId);

    String jobId = ID.uuid();
    String s3Key = getS3Key(siteId, documentId, jobId);

    this.s3.putObject(this.ocrBucket, s3Key, content.getBytes(StandardCharsets.UTF_8), contentType,
        null);

    Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(OcrEngine.MANUAL)
        .status(OcrScanStatus.SUCCESSFUL).contentType(contentType).userId(userId);

    save(siteId, ocr);

    updateOcrScanStatus(awsservice, siteId, documentId, OcrScanStatus.SUCCESSFUL);
  }

  @Override
  public String toText(final List<String> contents) {
    StringBuilder sb = new StringBuilder();
    contents.forEach(sb::append);
    return sb.toString();
  }

  @Override
  public void updateOcrScanStatus(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final OcrScanStatus status) {

    updateOcrScanStatus(siteId, documentId, status);

    ActionStatus actionStatus = OcrScanStatus.FAILED.equals(status) ? ActionStatus.FAILED
        : OcrScanStatus.SKIPPED.equals(status) ? ActionStatus.SKIPPED : ActionStatus.COMPLETE;

    ActionsService service = awsservice.getExtension(ActionsService.class);

    List<Action> actions = service.getActions(siteId, documentId);
    Optional<Action> o = actions.stream().filter(new ActionStatusPredicate(ActionStatus.RUNNING))
        .filter(new ActionTypePredicate(ActionType.OCR)).findFirst();

    if (o.isPresent()) {
      o.get().status(actionStatus);
      service.updateActionStatus(siteId, documentId, o.get());
    }

    ActionsNotificationService notificationService =
        awsservice.getExtension(ActionsNotificationService.class);
    notificationService.publishNextActionEvent(actions, siteId, documentId);
  }

  @Override
  public void updateOcrScanStatus(final String siteId, final String documentId,
      final OcrScanStatus status) {

    Map<String, AttributeValue> pkvalues = keysDocumentOcr(siteId, documentId);
    Map<String, AttributeValue> attributeValues =
        Map.of("ocrStatus", AttributeValue.builder().s(status.name().toLowerCase()).build());

    this.db.updateValues(pkvalues.get(PK), pkvalues.get(SK), attributeValues);
  }

  @Override
  public List<Map<String, Object>> toKeyValue(final List<String> contents) {
    return Collections.emptyList();
  }

  @Override
  public Object toTables(final List<String> contents) {
    return null;
  }

  /**
   * OCR Convert Document.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param request {@link OcrRequest}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param s3key original s3 key
   * @param documentS3toConvert Document S3 Key to convert.
   * @param contentType {@link String}
   * @return {@link String}
   */
  protected String convertDocument(final AwsServiceCache awsservice, final OcrRequest request,
      final String siteId, final String documentId, final String s3key,
      final String documentS3toConvert, final String contentType) {

    String jobId = ID.uuid();

    OcrSqsMessage msg = new OcrSqsMessage().jobId(jobId).siteId(siteId).documentId(documentId)
        .contentType(contentType).request(request);

    String json = this.gson.toJson(msg);

    String sqsQueueUrl = awsservice.environment("OCR_SQS_QUEUE_URL");
    SqsService sqsService = awsservice.getExtension(SqsService.class);
    sqsService.sendMessage(sqsQueueUrl, json);

    return jobId;
  }

  protected OcrEngine getOcrEngine(final OcrRequest request) {
    return OcrEngine.TESSERACT;
  }

  /**
   * Get S3 File Content-Type.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @return {@link String}
   */
  private String getS3FileContentType(final String bucket, final String key) {

    S3ObjectMetadata meta = this.s3.getObjectMetadata(bucket, key, null);
    return meta.getContentType();
  }

  /**
   * Document Formats Key {@link AttributeValue}.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link Map}
   */
  private Map<String, AttributeValue> keysDocumentOcr(final String siteId,
      final String documentId) {
    Ocr ocr = new Ocr().documentId(documentId);
    return Map.of(PK, AttributeValue.fromS(ocr.pk(siteId)), SK, AttributeValue.fromS(ocr.sk()));
  }

  /**
   * Update Plain Text OCR.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param userId {@link String}
   * @param s3Key {@link S3Object}
   * @param contentType {@link String}
   */
  private void updatePlainText(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final String userId, final String s3Key, final String contentType) {

    String jobId = ID.uuid();

    String content = this.s3.getContentAsString(this.documentsBucket, s3Key, null);

    String ocrS3Key = getS3Key(siteId, documentId, jobId);
    this.s3.putObject(this.ocrBucket, ocrS3Key, content.getBytes(StandardCharsets.UTF_8),
        contentType, null);

    OcrScanStatus status = OcrScanStatus.SKIPPED;

    Ocr ocr = new Ocr().documentId(documentId).jobId(jobId).engine(getOcrEngine(null))
        .status(status).contentType(contentType).userId(userId);

    save(siteId, ocr);

    updateOcrScanStatus(awsservice, siteId, documentId, status);
  }
}
