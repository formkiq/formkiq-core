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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.module.documentevents.DocumentEventType.ACTIONS;
import static com.formkiq.module.http.HttpResponseStatus.is2XX;
import static com.formkiq.module.http.HttpResponseStatus.is404;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMapToDocument;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentToFulltextDocument;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
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
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceImpl;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.UpdateFulltext;
import com.formkiq.stacks.client.models.UpdateFulltextTag;
import com.formkiq.stacks.client.requests.AddDocumentOcrRequest;
import com.formkiq.stacks.client.requests.OcrParseType;
import com.formkiq.stacks.client.requests.SetDocumentAntivirusRequest;
import com.formkiq.stacks.client.requests.UpdateDocumentFulltextRequest;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** {@link RequestHandler} for handling Document Actions. */
@Reflectable
@ReflectableImport(classes = {DocumentEvent.class, DocumentVersionServiceDynamoDb.class,
    DocumentVersionServiceNoVersioning.class})
@ReflectableClass(className = UpdateFulltextTag.class, allPublicConstructors = true,
    fields = {@ReflectableField(name = "key"), @ReflectableField(name = "value"),
        @ReflectableField(name = "values")})
@ReflectableClass(className = UpdateFulltext.class, allPublicConstructors = true,
    fields = {@ReflectableField(name = "content"), @ReflectableField(name = "contentUrls")})
public class DocumentActionsProcessor implements RequestHandler<Map<String, Object>, Void>, DbKeys {

  /** Default Maximum for Typesense Content. */
  private static final int DEFAULT_TYPESENSE_CHARACTER_MAX = 32768;
  /** {@link ActionsService}. */
  private ActionsService actionsService;
  /** {@link DynamoDbService}. */
  private DynamoDbService dbService;
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
  /** {@link S3Service}. */
  private S3Service s3Service;
  /** {@link AwsServiceCache}. */
  private AwsServiceCache serviceCache;
  /** {@link TypeSenseService}. */
  private TypeSenseService typesense = null;

  /**
   * constructor.
   * 
   */
  public DocumentActionsProcessor() {
    this(System.getenv(), Region.of(System.getenv("AWS_REGION")),
        EnvironmentVariableCredentialsProvider.create().resolveCredentials(),
        new DynamoDbConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new S3ConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SsmConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SnsConnectionBuilder("true".equals(System.getenv("ENABLE_AWS_X_RAY")))
            .setRegion(Region.of(System.getenv("AWS_REGION"))));
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

    AwsServiceCache.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(dbBuilder));
    AwsServiceCache.register(SsmConnectionBuilder.class,
        new ClassServiceExtension<SsmConnectionBuilder>(ssm));
    AwsServiceCache.register(SsmService.class, new SsmServiceExtension());
    AwsServiceCache.register(S3Service.class, new S3ServiceExtension(s3));
    AwsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    AwsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    AwsServiceCache.register(ConfigService.class, new ConfigServiceExtension());
    AwsServiceCache.register(FormKiqClientV1.class,
        new FormKiQClientV1Extension(awsRegion, awsCredentials));

    this.serviceCache =
        new AwsServiceCache().environment(map).debug("true".equals(map.get("DEBUG")));

    this.s3Service = this.serviceCache.getExtension(S3Service.class);
    this.documentService = this.serviceCache.getExtension(DocumentService.class);
    this.actionsService = new ActionsServiceDynamoDb(dbBuilder, map.get("DOCUMENTS_TABLE"));
    this.dbService = new DynamoDbServiceImpl(dbBuilder, map.get("DOCUMENTS_TABLE"));
    String snsDocumentEvent = map.get("SNS_DOCUMENT_EVENT");
    this.notificationService = new ActionsNotificationServiceImpl(snsDocumentEvent, sns);

    String appEnvironment = map.get("APP_ENVIRONMENT");
    final int cacheTime = 5;
    SsmService ssmService = new SsmServiceCache(ssm, cacheTime, TimeUnit.MINUTES);

    String typeSenseHost =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/api/TypesenseEndpoint");
    String typeSenseApiKey =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/typesense/ApiKey");

    if (typeSenseHost != null && typeSenseApiKey != null) {
      this.typesense =
          new TypeSenseServiceImpl(typeSenseHost, typeSenseApiKey, awsRegion, awsCredentials);
    }

    this.documentsIamUrl =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

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

    DocumentItem result = this.documentService.findDocument(siteId, documentId);
    DynamicDocumentItem item = new DocumentItemToDynamicDocumentItem().apply(result);

    String site = siteId != null ? siteId : SiteIdKeyGenerator.DEFAULT_SITE_ID;
    item.put("siteId", site);

    URL s3Url = getS3Url(siteId, documentId, item);
    item.put("url", s3Url);

    List<DynamicDocumentItem> documents = new ArrayList<>();
    documents.add(item);

    Optional<Action> antiVirus = actions.stream()
        .filter(
            a -> ActionType.ANTIVIRUS.equals(a.type()) && ActionStatus.COMPLETE.equals(a.status()))
        .findFirst();

    if (antiVirus.isPresent()) {

      Map<String, Collection<DocumentTag>> tagMap = this.documentService.findDocumentsTags(siteId,
          Arrays.asList(documentId), Arrays.asList("CLAMAV_SCAN_STATUS", "CLAMAV_SCAN_TIMESTAMP"));

      Map<String, String> values = new HashMap<>();
      Collection<DocumentTag> tags = tagMap.get(documentId);
      for (DocumentTag tag : tags) {
        values.put(tag.getKey(), tag.getValue());
      }

      String status = values.getOrDefault("CLAMAV_SCAN_STATUS", "ERROR");
      item.put("status", status);

      String timestamp = values.getOrDefault("CLAMAV_SCAN_TIMESTAMP", "");
      item.put("timestamp", timestamp);
    }

    return this.gson.toJson(Map.of("documents", documents));
  }

  private void debug(final LambdaLogger logger, final String siteId, final DocumentItem item) {
    if (isDebug()) {
      String s = String.format(
          "{\"siteId\": \"%s\",\"documentId\": \"%s\",\"path\": \"%s\",\"userId\": \"%s\","
              + "\"s3Version\": \"%s\",\"contentType\": \"%s\"}",
          siteId, item.getDocumentId(), item.getPath(), item.getUserId(), item.getS3version(),
          item.getContentType());

      logger.log(s);
    }
  }

  private int getCharacterMax(final Action action) {
    Map<String, String> parameters = notNull(action.parameters());
    return parameters.containsKey("characterMax") ? -1 : DEFAULT_TYPESENSE_CHARACTER_MAX;
  }

  /**
   * Get Content from {@link Action}.
   * 
   * @param dcFunc {@link DocumentContentFunction}
   * @param action {@link Action}
   * @param contentUrls {@link List} {@link String}
   * @return {@link String}
   * @throws URISyntaxException URISyntaxException
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private String getContent(final DocumentContentFunction dcFunc, final Action action,
      final List<String> contentUrls) throws URISyntaxException, IOException, InterruptedException {

    StringBuilder sb = dcFunc.getContentUrls(contentUrls);

    int characterMax = getCharacterMax(action);

    String content =
        characterMax != -1 && sb.length() > characterMax ? sb.substring(0, characterMax)
            : sb.toString();

    return content;
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

  /**
   * Get Document S3 Url.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link URL}
   */
  private URL getS3Url(final String siteId, final String documentId, final DocumentItem item) {

    String documentsBucket = this.serviceCache.environment("DOCUMENTS_S3_BUCKET");
    Duration duration = Duration.ofDays(1);
    PresignGetUrlConfig config =
        new PresignGetUrlConfig().contentDispositionByPath(item.getPath(), false);
    String s3key = createS3Key(siteId, documentId);
    return this.s3Service.presignGetUrl(documentsBucket, s3key, duration, null, config);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    LambdaLogger logger = context.getLogger();

    if (isDebug()) {
      String json = this.gson.toJson(map);
      logger.log(json);
    }

    List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
    try {
      processRecords(logger, records);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return null;
  }

  private boolean isDebug() {
    return this.serviceCache.debug();
  }

  /**
   * Log Start of {@link Action}.
   * 
   * @param logger {@link LambdaLogger}
   * @param type {@link String}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   */
  private void logAction(final LambdaLogger logger, final String type, final String siteId,
      final String documentId, final Action action) {

    String s = String.format(
        "{\"type\",\"%s\",\"siteId\":\"%s\",\"documentId\":\"%s\",\"actionType\":\"%s\","
            + "\"actionStatus\":\"%s\",\"userId\":\"%s\",\"parameters\": \"%s\"}",
        type, siteId, documentId, action.type(), action.status(), action.userId(),
        action.parameters());

    logger.log(s);
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

    logAction(logger, "action start", siteId, documentId, action);

    if (ActionType.DOCUMENTTAGGING.equals(action.type())) {

      DocumentTaggingAction dtAction = new DocumentTaggingAction(this.serviceCache);
      dtAction.run(logger, siteId, documentId, action);

      List<Action> updatedActions = this.actionsService.updateActionStatus(siteId, documentId,
          action.type(), ActionStatus.COMPLETE);

      action.status(ActionStatus.COMPLETE);
      this.notificationService.publishNextActionEvent(updatedActions, siteId, documentId);

    } else if (ActionType.OCR.equals(action.type())) {

      List<OcrParseType> parseTypes = getOcrParseTypes(action);
      Map<String, String> parameters =
          action.parameters() != null ? action.parameters() : new HashMap<>();
      String addPdfDetectedCharactersAsText =
          parameters.getOrDefault("addPdfDetectedCharactersAsText", "false");

      this.formkiqClient.addDocumentOcr(
          new AddDocumentOcrRequest().siteId(siteId).documentId(documentId).parseTypes(parseTypes)
              .addPdfDetectedCharactersAsText("true".equals(addPdfDetectedCharactersAsText)));

    } else if (ActionType.FULLTEXT.equals(action.type())) {

      ActionStatus status = ActionStatus.COMPLETE;
      DocumentItem item = this.documentService.findDocument(siteId, documentId);
      debug(logger, siteId, item);

      logger.log("contenttype: " + item.getContentType());
      logger.log("MIMETYPE: " + MimeType.isPlainText(item.getContentType()));
      logger.log("DOCUMENTS_S3_BUCKET: " + this.serviceCache.environment("DOCUMENTS_S3_BUCKET"));
      DocumentContentFunction documentContentFunc = new DocumentContentFunction(this.serviceCache);
      List<String> contentUrls = documentContentFunc.getContentUrls(siteId, item);

      boolean moduleFulltext = this.serviceCache.hasModule("fulltext");

      if (moduleFulltext) {
        updateOpensearchFulltext(logger, siteId, documentId, action, contentUrls);
      } else if (this.typesense != null) {
        updateTypesense(documentContentFunc, siteId, documentId, action, contentUrls);
      } else {
        status = ActionStatus.FAILED;
      }

      List<Action> updatedActions =
          this.actionsService.updateActionStatus(siteId, documentId, ActionType.FULLTEXT, status);

      if (ActionStatus.COMPLETE.equals(status)) {
        this.notificationService.publishNextActionEvent(updatedActions, siteId, documentId);
      }

      action.status(status);

    } else if (ActionType.ANTIVIRUS.equals(action.type())) {

      SetDocumentAntivirusRequest req =
          new SetDocumentAntivirusRequest().siteId(siteId).documentId(documentId);
      this.formkiqClient.setDocumentAntivirus(req);

    } else if (ActionType.WEBHOOK.equals(action.type())) {

      sendWebhook(siteId, documentId, action);
    }

    logAction(logger, "action complete", siteId, documentId, action);
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

        this.actionsService.updateActionStatus(siteId, documentId, o.get().type(), status);

        try {

          processAction(logger, siteId, documentId, action);

        } catch (Exception e) {
          e.printStackTrace();
          status = ActionStatus.FAILED;
          action.status(status);

          logger.log(String.format("Updating Action Status to %s", action.status()));

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

          String s = String.format(
              "{\"siteId\": \"%s\",\"documentId\": \"%s\",\"s3key\": \"%s\",\"s3bucket\": \"%s\","
                  + "\"type\": \"%s\",\"userId\": %s,"
                  + "\"contentType\": \"%s\",\"path\":\"%s\",\"content\":%s}",
              event.siteId(), event.documentId(), event.s3key(), event.s3bucket(), event.type(),
              event.userId(), event.contentType(), event.path(), event.content());

          logger.log(s);
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

        action.status(ActionStatus.COMPLETE);

      } else {
        throw new IOException(url + " response status code " + statusCode);
      }

    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  /**
   * Update Document Content to Opensearch.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @param contentUrls {@link List} {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void updateOpensearchFulltext(final LambdaLogger logger, final String siteId,
      final String documentId, final Action action, final List<String> contentUrls)
      throws IOException, InterruptedException {

    UpdateFulltext fulltext = new UpdateFulltext().contentUrls(contentUrls);

    UpdateDocumentFulltextRequest req = new UpdateDocumentFulltextRequest().siteId(siteId)
        .documentId(documentId).document(fulltext);

    boolean updateDocumentFulltext = this.formkiqClient.updateDocumentFulltext(req);
    if (updateDocumentFulltext) {
      logger.log(String.format("successfully processed action %s", action.type()));
    } else {
      throw new IOException("unable to update Document Fulltext");
    }
  }

  /**
   * Update Typesense Content.
   * 
   * @param dcFunc {@link DocumentContentFunction}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param action {@link Action}
   * @param contentUrls {@link List} {@link String}
   * @throws IOException IOException
   */
  private void updateTypesense(final DocumentContentFunction dcFunc, final String siteId,
      final String documentId, final Action action, final List<String> contentUrls)
      throws IOException {

    try {

      Map<String, AttributeValue> keys = keysDocument(siteId, documentId);
      Map<String, AttributeValue> data = this.dbService.get(keys.get(PK), keys.get(SK));

      Map<String, Object> document = new DocumentMapToDocument().apply(data);
      document = new DocumentToFulltextDocument().apply(document);

      StringBuilder sb =
          document.containsKey("text") ? new StringBuilder(document.get("text").toString())
              : new StringBuilder();

      String content = getContent(dcFunc, action, contentUrls);
      sb.append(" ");
      sb.append(content);
      document.put("text", sb.toString());

      HttpResponse<String> response = this.typesense.updateDocument(siteId, documentId, document);

      if (!is2XX(response)) {
        response = this.typesense.addDocument(siteId, documentId, document);

        if (is404(response)) {
          response = this.typesense.addCollection(siteId);

          if (is2XX(response)) {
            response = this.typesense.addDocument(siteId, documentId, document);
          }

          if (!is2XX(response)) {
            throw new IOException(response.body());
          }
        }
      }

    } catch (URISyntaxException | InterruptedException e) {
      throw new IOException(e);
    }
  }
}
