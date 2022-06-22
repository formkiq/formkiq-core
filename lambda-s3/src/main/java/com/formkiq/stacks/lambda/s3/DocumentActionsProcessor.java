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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.actions.services.NextActionPredicate;
import com.formkiq.module.documentevents.DocumentEvent;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.AddDocumentOcrRequest;
import com.formkiq.stacks.client.requests.OcrParseType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** {@link RequestHandler} for handling Document Actions. */
@Reflectable
@ReflectableImport(classes = DocumentEvent.class)
public class DocumentActionsProcessor implements RequestHandler<Map<String, Object>, Void> {

  /** {@link ActionsService}. */
  private ActionsService actionsService;

  /** IAM Documents Url. */
  private String documentsIamUrl;
  /** {@link FormKiqClient}. */
  private FormKiqClientV1 formkiqClient = null;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** constructor. */
  public DocumentActionsProcessor() {
    this(System.getenv(), Region.of(System.getenv("AWS_REGION")),
        EnvironmentVariableCredentialsProvider.create().resolveCredentials(),
        new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))),
        new SsmConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION"))));
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param awsRegion {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param db {@link DynamoDbConnectionBuilder}
   * @param ssm {@link SsmConnectionBuilder}
   */
  protected DocumentActionsProcessor(final Map<String, String> map, final Region awsRegion,
      final AwsCredentials awsCredentials, final DynamoDbConnectionBuilder db,
      final SsmConnectionBuilder ssm) {

    this.actionsService = new ActionsServiceDynamoDb(db, map.get("DOCUMENTS_TABLE"));

    String appEnvironment = map.get("APP_ENVIRONMENT");
    final int cacheTime = 5;
    SsmService ssmService = new SsmServiceCache(ssm, cacheTime, TimeUnit.MINUTES);
    this.documentsIamUrl =
        ssmService.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

    FormKiqClientConnection fkqConnection =
        new FormKiqClientConnection(this.documentsIamUrl).region(awsRegion);

    if (awsCredentials != null) {
      fkqConnection = fkqConnection.credentials(awsCredentials);
    }

    this.formkiqClient = new FormKiqClientV1(fkqConnection);
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
        if (ActionType.OCR.equals(action.type())) {

          List<OcrParseType> parseTypes = getOcrParseTypes(action);

          this.formkiqClient.addDocumentOcr(new AddDocumentOcrRequest().siteId(siteId)
              .documentId(documentId).parseTypes(parseTypes));
        }
      }
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
}
