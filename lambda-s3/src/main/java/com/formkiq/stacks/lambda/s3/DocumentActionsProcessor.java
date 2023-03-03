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
package com.formkiq.stacks.lambda.s3;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.module.documentevents.DocumentEventType.ACTIONS;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
import com.formkiq.graalvm.annotations.ReflectableClasses;
import com.formkiq.graalvm.annotations.ReflectableField;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceImpl;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.actions.services.NextActionPredicate;
import com.formkiq.module.documentevents.DocumentEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.UpdateFulltext;
import com.formkiq.stacks.client.models.UpdateFulltextTag;
import com.formkiq.stacks.client.requests.AddDocumentOcrRequest;
import com.formkiq.stacks.client.requests.GetDocumentOcrRequest;
import com.formkiq.stacks.client.requests.OcrParseType;
import com.formkiq.stacks.client.requests.SetDocumentAntivirusRequest;
import com.formkiq.stacks.client.requests.UpdateDocumentFulltextRequest;
import com.formkiq.stacks.common.formats.MimeType;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** {@link RequestHandler} for handling Document Actions. */
@Reflectable
@ReflectableImport(classes = {DocumentEvent.class, DocumentVersionServiceDynamoDb.class,
    DocumentVersionServiceNoVersioning.class})
@ReflectableClasses({@ReflectableClass(className = UpdateFulltextTag.class,
    allPublicConstructors = true, fields = {@ReflectableField(name = "key"),
        @ReflectableField(name = "value"), @ReflectableField(name = "values")})})
public class DocumentActionsProcessor implements RequestHandler<Map<String, Object>, Void> {

  /** {@link ActionsService}. */
  private ActionsService actionsService;
  /** S3 Documents Bucket. */
  private String documentsBucket;
  /** {@link DocumentService}. */
  private DocumentService documentService;
  /** IAM Documents Url. */
  private String documentsIamUrl;
  /** {@link FormKiqClientConnection}. */
  private FormKiqClientConnection fkqConnection;
  /** {@link FormKiqClientV1}. */
  private FormKiqClientV1 formkiqClient = null;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link ActionsNotificationService}. */
  private ActionsNotificationService notificationService;
  /** Ocr Bucket. */
  private String ocrBucket;
  /** {@link S3Service}. */
  private S3Service s3Service;

  /**
   * constructor.
   * 
   */
  public DocumentActionsProcessor() {
    this(System.getenv(), Region.of(System.getenv("AWS_REGION")),
        EnvironmentVariableCredentialsProvider.create().resolveCredentials(),
        new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new S3ConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SsmConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SnsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param awsRegion {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param dbBuilder {@link DynamoDbConnectionBuilder}
   * @param s3 {@link S3ConnectionBuilder}
   * @param ssm {@link SsmConnectionBuilder}
   * @param sns {@link SnsConnectionBuilder}
   */
  protected DocumentActionsProcessor(final Map<String, String> map, final Region awsRegion,
      final AwsCredentials awsCredentials, final DynamoDbConnectionBuilder dbBuilder,
      final S3ConnectionBuilder s3, final SsmConnectionBuilder ssm,
      final SnsConnectionBuilder sns) {

    AwsServiceCache serviceCache = new AwsServiceCache().environment(map);
    DocumentVersionServiceExtension dsExtension = new DocumentVersionServiceExtension();
    DocumentVersionService versionService = dsExtension.loadService(serviceCache);

    this.s3Service = new S3Service(s3);
    this.documentService =
        new DocumentServiceImpl(dbBuilder, map.get("DOCUMENTS_TABLE"), versionService);
    this.actionsService = new ActionsServiceDynamoDb(dbBuilder, map.get("DOCUMENTS_TABLE"));
    String snsDocumentEvent = map.get("SNS_DOCUMENT_EVENT");
    this.notificationService = new ActionsNotificationServiceImpl(snsDocumentEvent, sns);

    String appEnvironment = map.get("APP_ENVIRONMENT");
    final int cacheTime = 5;
    SsmService ssmService = new SsmServiceCache(ssm, cacheTime, TimeUnit.MINUTES);

    this.documentsIamUrl =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");
    this.documentsBucket =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/s3/DocumentsS3Bucket");
    this.ocrBucket = ssmService.getParameterValue("/formkiq/" + appEnvironment + "/s3/OcrBucket");

    this.fkqConnection = new FormKiqClientConnection(this.documentsIamUrl).region(awsRegion);

    if (awsCredentials != null) {
      this.fkqConnection = this.fkqConnection.credentials(awsCredentials);
    }

    this.formkiqClient = new FormKiqClientV1(this.fkqConnection);
  }

  /**
   * Build Webhook Body.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actions {@link List} {@link Action}
   * @return {@link String}
   */
  private String buildWebhookBody(final String siteId, final String documentId,
      final List<Action> actions) {

    String site = siteId != null ? siteId : SiteIdKeyGenerator.DEFAULT_SITE_ID;

    List<Map<String, String>> documents = new ArrayList<>();
    Map<String, String> map = new HashMap<>();
    map.put("siteId", site);
    map.put("documentId", documentId);
    documents.add(map);

    Optional<Action> antiVirus = actions.stream()
        .filter(
            a -> ActionType.ANTIVIRUS.equals(a.type()) && ActionStatus.COMPLETE.equals(a.status()))
        .findFirst();

    if (antiVirus.isPresent()) {

      DocumentItem item = this.documentService.findDocument(siteId, documentId);
      map.put("filename", item.getPath());

      Map<String, Collection<DocumentTag>> tagMap = this.documentService.findDocumentsTags(siteId,
          Arrays.asList(documentId), Arrays.asList("CLAMAV_SCAN_STATUS", "CLAMAV_SCAN_TIMESTAMP"));

      Map<String, String> values = new HashMap<>();
      Collection<DocumentTag> tags = tagMap.get(documentId);
      for (DocumentTag tag : tags) {
        values.put(tag.getKey(), tag.getValue());
      }

      String status = values.getOrDefault("CLAMAV_SCAN_STATUS", "ERROR");
      map.put("status", status);

      String timestamp = values.getOrDefault("CLAMAV_SCAN_TIMESTAMP", "");
      map.put("timestamp", timestamp);
    }

    return this.gson.toJson(Map.of("documents", documents));
  }

  /**
   * Find Content Url.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link List} {@link String}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private List<String> findContentUrls(final String siteId, final DocumentItem item)
      throws IOException, InterruptedException {

    List<String> urls = null;
    String documentId = item.getDocumentId();
    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);

    if (MimeType.isPlainText(item.getContentType())) {

      PresignGetUrlConfig config = new PresignGetUrlConfig()
          .contentDispositionByPath(item.getPath(), false).contentType(item.getContentType());

      String bucket =
          MimeType.isPlainText(item.getContentType()) ? this.documentsBucket : this.ocrBucket;

      String url =
          this.s3Service.presignGetUrl(bucket, s3Key, Duration.ofHours(1), null, config).toString();
      urls = Arrays.asList(url);

    } else {

      GetDocumentOcrRequest req = new GetDocumentOcrRequest().siteId(siteId).documentId(documentId);

      req.addQueryParameter("contentUrl", "true");
      req.addQueryParameter("text", "true");

      HttpResponse<String> response = this.formkiqClient.getDocumentOcrAsHttpResponse(req);
      Map<String, Object> map = this.gson.fromJson(response.body(), Map.class);

      if (map != null && map.containsKey("contentUrls")) {
        urls = (List<String>) map.get("contentUrls");
      }
    }

    return urls;
  }

  /**
   * Get ParseTypes from {@link Action} parameters.
   * 
   * @param action {@link Action}
   * @return {@link List} {@link OcrParseType}
   */
  List<OcrParseType> getOcrParseTypes(final Action action) {

    Map<String, String> parameters = notNull(action.parameters());
    String s = parameters.containsKey("parseTypes") ? parameters.get("parseTypes") : "TEXT";

    List<OcrParseType> ocrParseTypes = Arrays.asList(s.split(",")).stream().map(t -> {
      try {
        return OcrParseType.valueOf(t.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
        return OcrParseType.TEXT;
      }
    }).distinct().collect(Collectors.toList());

    return ocrParseTypes;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json = null;

    try {

      LambdaLogger logger = context.getLogger();

      if ("true".equals(System.getenv("DEBUG"))) {
        json = this.gson.toJson(map);
        logger.log(json);
      }

      List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      processRecords(logger, records);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Process Action.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void processAction(final LambdaLogger logger, final String siteId,
      final String documentId, final Action action) throws IOException, InterruptedException {

    if (ActionType.OCR.equals(action.type())) {

      List<OcrParseType> parseTypes = getOcrParseTypes(action);
      Map<String, String> parameters =
          action.parameters() != null ? action.parameters() : new HashMap<>();
      String addPdfDetectedCharactersAsText =
          parameters.getOrDefault("addPdfDetectedCharactersAsText", "false");

      this.formkiqClient.addDocumentOcr(
          new AddDocumentOcrRequest().siteId(siteId).documentId(documentId).parseTypes(parseTypes)
              .addPdfDetectedCharactersAsText("true".equals(addPdfDetectedCharactersAsText)));

    } else if (ActionType.FULLTEXT.equals(action.type())) {

      DocumentItem item = this.documentService.findDocument(siteId, documentId);
      List<String> contentUrls = findContentUrls(siteId, item);

      // final int maxTags = 100;
      // PaginationResults<DocumentTag> docTags =
      // this.documentService.findDocumentTags(siteId, documentId, null, maxTags);

      // List<UpdateFulltextTag> tags =
      // docTags.getResults().stream().filter(t -> DocumentTagType.USERDEFINED.equals(t.getType()))
      // .map(t -> new UpdateFulltextTag().key(t.getKey()).value(t.getValue())
      // .values(t.getValues()))
      // .collect(Collectors.toList());

      UpdateFulltext fulltext = new UpdateFulltext().contentUrls(contentUrls);

      UpdateDocumentFulltextRequest req = new UpdateDocumentFulltextRequest().siteId(siteId)
          .documentId(documentId).document(fulltext);

      this.formkiqClient.updateDocumentFulltext(req);

    } else if (ActionType.ANTIVIRUS.equals(action.type())) {

      SetDocumentAntivirusRequest req =
          new SetDocumentAntivirusRequest().siteId(siteId).documentId(documentId);
      this.formkiqClient.setDocumentAntivirus(req);

    } else if (ActionType.WEBHOOK.equals(action.type())) {

      sendWebhook(siteId, documentId, action);
    }
  }

  /**
   * Process {@link DocumentEvent}.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link DocumentEvent}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  private void processEvent(final LambdaLogger logger, final DocumentEvent event)
      throws IOException, InterruptedException {

    if (ACTIONS.equals(event.type())) {

      String siteId = event.siteId();
      String documentId = event.documentId();

      List<Action> actions = this.actionsService.getActions(siteId, documentId);
      Optional<Action> o = actions.stream().filter(new NextActionPredicate()).findFirst();

      if (o.isPresent()) {

        Action action = o.get();
        ActionStatus status = ActionStatus.RUNNING;

        logger.log(String.format("Processing SiteId %s Document %s on action %s", siteId,
            documentId, action.type()));

        this.actionsService.updateActionStatus(siteId, documentId, o.get().type(), status);

        try {
          processAction(logger, siteId, documentId, action);

        } catch (Exception e) {
          e.printStackTrace();
          status = ActionStatus.FAILED;
          action.status(status);

          this.actionsService.updateActionStatus(siteId, documentId, o.get().type(), status);
        }


      } else {
        logger
            .log(String.format("NO ACTIONS found for  SiteId %s Document %s", siteId, documentId));
      }
    } else {
      logger.log(String.format("Skipping event %s", event.type()));
    }
  }

  /**
   * Process Event Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param records {@link List} {@link Map}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private void processRecords(final LambdaLogger logger, final List<Map<String, Object>> records)
      throws IOException, InterruptedException {

    for (Map<String, Object> e : records) {

      if (e.containsKey("body")) {

        String body = e.get("body").toString();

        Map<String, Object> map = this.gson.fromJson(body, Map.class);
        if (map.containsKey("Message")) {
          DocumentEvent event =
              this.gson.fromJson(map.get("Message").toString(), DocumentEvent.class);
          processEvent(logger, event);
        }
      }
    }
  }

  /**
   * Sends Webhook.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void sendWebhook(final String siteId, final String documentId, final Action action)
      throws IOException, InterruptedException {

    String url = action.parameters().get("url");

    List<Action> actions = this.actionsService.getActions(siteId, documentId);

    String body = buildWebhookBody(siteId, documentId, actions);

    try {

      HttpRequest request = HttpRequest.newBuilder().uri(new URI(url))
          .timeout(Duration.ofMinutes(1)).POST(HttpRequest.BodyPublishers.ofString(body)).build();

      HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS)
          .connectTimeout(Duration.ofMinutes(1)).build();
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

      int statusCode = response.statusCode();
      final int statusOk = 200;
      final int statusRedirect = 300;

      if (statusCode >= statusOk && statusCode < statusRedirect) {

        List<Action> updatedActions = this.actionsService.updateActionStatus(siteId, documentId,
            ActionType.WEBHOOK, ActionStatus.COMPLETE);

        this.notificationService.publishNextActionEvent(updatedActions, siteId, documentId);

      } else {
        throw new IOException(url + " response status code " + statusCode);
      }

    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }
}
