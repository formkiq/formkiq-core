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
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.module.events.document.DocumentEventType.ACTIONS;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3PresignerServiceExtension;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.ses.SesService;
import com.formkiq.aws.ses.SesServiceExtension;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionStatusPredicate;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.EventServiceSnsExtension;
import com.formkiq.module.events.document.DocumentEvent;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.httpsigv4.HttpServiceSigv4;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceExtension;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceExtension;
import com.formkiq.stacks.dynamodb.mappings.MappingService;
import com.formkiq.stacks.dynamodb.mappings.MappingServiceExtension;
import com.formkiq.stacks.lambda.s3.actions.DocumentTaggingAction;
import com.formkiq.stacks.lambda.s3.actions.FullTextAction;
import com.formkiq.stacks.lambda.s3.actions.IdpAction;
import com.formkiq.stacks.lambda.s3.actions.NotificationAction;
import com.formkiq.stacks.lambda.s3.actions.AddOcrAction;
import com.formkiq.stacks.lambda.s3.actions.PdfExportAction;
import com.formkiq.stacks.lambda.s3.actions.SendHttpRequest;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;

/** {@link RequestHandler} for handling Document Actions. */
@Reflectable
public class DocumentActionsProcessor implements RequestHandler<Map<String, Object>, Void>, DbKeys {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;

  static {

    if (System.getenv().containsKey("AWS_REGION")) {
      serviceCache = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
              new SnsAwsServiceRegistry(), new SsmAwsServiceRegistry(), new SesAwsServiceRegistry())
          .build();

      initialize(serviceCache);
    }
  }

  /**
   * Initialize.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   */
  static void initialize(final AwsServiceCache awsServiceCache) {

    awsServiceCache.register(DynamoDbService.class, new DynamoDbServiceExtension());
    awsServiceCache.register(MappingService.class, new MappingServiceExtension());
    awsServiceCache.register(SsmService.class, new SsmServiceExtension());
    awsServiceCache.register(S3Service.class, new S3ServiceExtension());
    awsServiceCache.register(S3PresignerService.class, new S3PresignerServiceExtension());
    awsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    awsServiceCache.register(ConfigService.class, new ConfigServiceExtension());
    awsServiceCache.register(EventService.class, new EventServiceSnsExtension());
    awsServiceCache.register(ActionsService.class, new ActionsServiceExtension());
    awsServiceCache.register(ActionsNotificationService.class,
        new ActionsNotificationServiceExtension());
    awsServiceCache.register(SesService.class, new SesServiceExtension());
    awsServiceCache.register(AttributeService.class, new AttributeServiceExtension());

    SsmService ssmService = awsServiceCache.getExtension(SsmService.class);

    String appEnvironment = awsServiceCache.environment("APP_ENVIRONMENT");

    String typeSenseHost =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/api/TypesenseEndpoint");
    String typeSenseApiKey =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/typesense/ApiKey");

    if (!isEmpty(typeSenseHost) && !isEmpty(typeSenseApiKey)) {
      awsServiceCache.environment().put("TYPESENSE_HOST", typeSenseHost);
      awsServiceCache.environment().put("TYPESENSE_API_KEY", typeSenseApiKey);
      awsServiceCache.register(TypeSenseService.class, new TypeSenseServiceExtension());
    }

    String documentsIamUrl =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

    awsServiceCache.environment().put("documentsIamUrl", documentsIamUrl);
    AwsCredentials awsCredentials = awsServiceCache.getExtension(AwsCredentials.class);
    awsServiceCache.register(HttpService.class, new ClassServiceExtension<>(
        new HttpServiceSigv4(awsServiceCache.region(), awsCredentials)));
  }

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   *
   */
  public DocumentActionsProcessor() {
    // empty
  }

  /**
   * constructor.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   * 
   */
  public DocumentActionsProcessor(final AwsServiceCache awsServiceCache) {
    this();
    initialize(awsServiceCache);
    serviceCache = awsServiceCache;
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

    DocumentService documentService = serviceCache.getExtension(DocumentService.class);

    DocumentItem result = documentService.findDocument(siteId, documentId);
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

      Map<String, Collection<DocumentTag>> tagMap =
          documentService.findDocumentsTags(siteId, Collections.singletonList(documentId),
              Arrays.asList("CLAMAV_SCAN_STATUS", "CLAMAV_SCAN_TIMESTAMP"));

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

  private ActionsService getActionsService() {
    return serviceCache.getExtension(ActionsService.class);
  }

  private ActionsNotificationService getNotificationService() {
    return serviceCache.getExtension(ActionsNotificationService.class);
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

    String documentsBucket = serviceCache.environment("DOCUMENTS_S3_BUCKET");
    Duration duration = Duration.ofDays(1);
    PresignGetUrlConfig config =
        new PresignGetUrlConfig().contentDispositionByPath(item.getPath(), false);
    String s3key = createS3Key(siteId, documentId);

    S3PresignerService s3Presigner = serviceCache.getExtension(S3PresignerService.class);
    return s3Presigner.presignGetUrl(documentsBucket, s3key, duration, null, config);
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
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      logger.log(Strings.toString(e));
      throw new RuntimeException(e);
    }

    return null;
  }

  private boolean isDebug() {
    return serviceCache.debug();
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

    if (isDebug()) {
      String s = String.format(
          "{\"type\",\"%s\",\"siteId\":\"%s\",\"documentId\":\"%s\",\"actionType\":\"%s\","
              + "\"actionStatus\":\"%s\",\"userId\":\"%s\",\"parameters\": \"%s\"}",
          type, siteId, documentId, action.type(), action.status(), action.userId(),
          action.parameters());

      logger.log(s);
    }
  }

  /**
   * Process Action.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actions {@link List} {@link Action}
   * @param action {@link Action}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void processAction(final LambdaLogger logger, final String siteId,
      final String documentId, final List<Action> actions, final Action action)
      throws IOException, InterruptedException, ValidationException {

    boolean updateComplete = false;
    ActionStatus completeStatus = ActionStatus.COMPLETE;

    logAction(logger, "action start", siteId, documentId, action);

    switch (action.type()) {
      case QUEUE -> {
        completeStatus = ActionStatus.IN_QUEUE;
        updateComplete = true;
      }

      case DOCUMENTTAGGING -> {
        DocumentTaggingAction dtAction = new DocumentTaggingAction(serviceCache);
        dtAction.run(logger, siteId, documentId, actions, action);

        updateComplete = true;
      }

      case OCR -> new AddOcrAction(serviceCache).run(logger, siteId, documentId, actions, action);

      case FULLTEXT ->
        new FullTextAction(serviceCache).run(logger, siteId, documentId, actions, action);

      case ANTIVIRUS -> new SendHttpRequest(serviceCache).sendRequest(siteId, "PUT",
          "/documents/" + documentId + "/antivirus", "");

      case WEBHOOK -> sendWebhook(logger, siteId, documentId, actions, action);

      case NOTIFICATION -> {
        DocumentAction da = new NotificationAction(siteId, serviceCache);
        da.run(logger, siteId, documentId, actions, action);

        updateComplete = true;
      }

      case IDP -> {
        DocumentAction da = new IdpAction(serviceCache);
        da.run(logger, siteId, documentId, actions, action);
        updateComplete = true;
      }

      case PUBLISH -> {
        DocumentAction da = new PublishAction(serviceCache);
        da.run(logger, siteId, documentId, actions, action);
        updateComplete = true;
      }

      case PDFEXPORT -> {
        DocumentAction da = new PdfExportAction(serviceCache);
        da.run(logger, siteId, documentId, actions, action);
        updateComplete = true;
      }

      default -> throw new IOException("Unhandled Action Type: " + action.type());
    }

    logAction(logger, "action complete", siteId, documentId, action);

    if (updateComplete) {
      updateComplete(logger, siteId, documentId, actions, action, completeStatus);
    }
  }

  /**
   * Process {@link DocumentEvent}.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link DocumentEvent}
   */
  public void processEvent(final LambdaLogger logger, final DocumentEvent event) {

    ActionsService actionsService = getActionsService();

    if (ACTIONS.equals(event.type())) {

      String siteId = event.siteId();
      String documentId = event.documentId();

      List<Action> actions = actionsService.getActions(siteId, documentId);

      Optional<Action> running =
          actions.stream().filter(new ActionStatusPredicate(ActionStatus.RUNNING)).findAny();
      Optional<Action> o =
          actions.stream().filter((new ActionStatusPredicate(ActionStatus.PENDING))).findFirst();

      if (running.isPresent()) {

        logger.log(
            String.format("ACTIONS already RUNNING for SiteId %s Document %s", siteId, documentId));

      } else if (o.isPresent()) {

        Action action = o.get();
        action.status(ActionStatus.RUNNING);

        actionsService.updateActionStatus(siteId, documentId, action);

        try {

          processAction(logger, siteId, documentId, actions, action);

        } catch (Exception e) {

          e.printStackTrace();
          String stacktrace = Strings.toString(e);
          logger.log(stacktrace);

          action.status(ActionStatus.FAILED);

          if (!isEmpty(e.getMessage())) {
            action.message(e.getMessage());
          } else {
            action.message(stacktrace);
          }

          updateDocumentWorkflow(siteId, documentId, action);

          logger.log(String.format("Updating Action Status to %s", action.status()));

          actionsService.updateActionStatus(siteId, documentId, action);
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

    for (Map<String, Object> e : Objects.notNull(records)) {

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
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actions {@link List} {@link Action}
   * @param action {@link Action}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void sendWebhook(final LambdaLogger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException, InterruptedException {

    String url = action.parameters().get("url");

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

        updateComplete(logger, siteId, documentId, actions, action, ActionStatus.COMPLETE);

      } else {
        throw new IOException(url + " response status code " + statusCode);
      }

    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  /**
   * Update Complete Action.
   * 
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param actions {@link List} {@link Action}
   * @param action {@link Action}
   * @param completeStatus {@link ActionStatus}
   */
  private void updateComplete(final LambdaLogger logger, final String siteId,
      final String documentId, final List<Action> actions, final Action action,
      final ActionStatus completeStatus) {

    if (isDebug()) {
      logger.log(String.format("updating status of %s to %s", documentId, completeStatus));
    }

    action.status(completeStatus);
    getActionsService().updateActionStatus(siteId, documentId, action);

    updateDocumentWorkflow(siteId, documentId, action);

    if (!ActionType.QUEUE.equals(action.type())) {
      boolean publishNextActionEvent =
          getNotificationService().publishNextActionEvent(actions, siteId, documentId);
      if (isDebug() && publishNextActionEvent) {
        logger.log(String.format("publishing next event for %s to %s", siteId, documentId));
      }
    }
  }

  private void updateDocumentWorkflow(final String siteId, final String documentId,
      final Action action) {

    if (!isEmpty(action.workflowId()) && !isEmpty(action.workflowStepId())) {
      getActionsService().updateDocumentWorkflowStatus(siteId, documentId, action);
    }
  }
}
