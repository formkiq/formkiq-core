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
package com.formkiq.module.lambda.typesense;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteId;
import static com.formkiq.module.http.HttpResponseStatus.is2XX;
import static com.formkiq.module.http.HttpResponseStatus.is404;
import static com.formkiq.module.http.HttpResponseStatus.is409;
import static com.formkiq.module.http.HttpResponseStatus.is429;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** {@link RequestHandler} for handling DynamoDb to Typesense Processor. */
@Reflectable
public class TypesenseProcessor implements RequestHandler<Map<String, Object>, Void> {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** {@link TypeSenseService}. */
  private TypeSenseService typeSenseService;
  /** {@link DocumentToFulltextDocument}. */
  private DocumentToFulltextDocument fulltext = new DocumentToFulltextDocument();
  /** Debug. */
  private boolean debug;

  /**
   * constructor.
   * 
   */
  public TypesenseProcessor() {
    this(System.getenv(), EnvironmentVariableCredentialsProvider.create().resolveCredentials());
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   * @param credentials {@link AwsCredentials}
   */
  public TypesenseProcessor(final Map<String, String> map, final AwsCredentials credentials) {

    Region region = Region.of(map.get("AWS_REGION"));

    this.typeSenseService = new TypeSenseServiceImpl(map.get("TYPESENSE_HOST"),
        map.get("TYPESENSE_API_KEY"), region, credentials);
    this.debug = "true".equals(map.get("DEBUG"));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json = null;

    try {

      LambdaLogger logger = context.getLogger();

      if (this.debug) {
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
   * Process Record.
   * 
   * @param logger {@link LambdaLogger}
   * 
   * @param record {@link Map}
   */
  private void processRecord(final LambdaLogger logger, final Map<String, Object> record) {

    String eventName = record.get("eventName").toString();
    Map<String, Object> dynamodb = toMap(record.get("dynamodb"));

    Map<String, Object> newImage =
        dynamodb.containsKey("NewImage") ? toMap(dynamodb.get("NewImage")) : Collections.emptyMap();

    String siteId = newImage.containsKey(PK) ? getSiteId(newImage.get(PK).toString()) : null;
    String documentId =
        newImage.containsKey("documentId") ? newImage.get("documentId").toString() : null;

    if (documentId != null) {

      try {

        if ("INSERT".equalsIgnoreCase(eventName) || "MODIFY".equalsIgnoreCase(eventName)) {
          logger
              .log("processing event " + eventName + " for document " + siteId + " " + documentId);

          writeToIndex(logger, siteId, documentId, newImage);
        } else {
          logger.log("skipping event " + eventName + " for document " + siteId + " " + documentId);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }

    } else {
      logger.log("skipping event " + eventName);
    }
  }

  /**
   * Process Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param records {@link List} {@link Map}
   */
  private void processRecords(final LambdaLogger logger, final List<Map<String, Object>> records) {
    for (Map<String, Object> record : records) {

      if (record.containsKey("eventName")) {
        processRecord(logger, record);
      }
    }
  }

  /**
   * Remove DynamoDb Keys.
   * 
   * @param map {@link Map}
   */
  private void removeDynamodbKeys(final Map<String, Object> map) {
    map.remove(PK);
    map.remove(SK);
    map.remove(GSI1_PK);
    map.remove(GSI1_SK);
    map.remove(GSI2_PK);
    map.remove(GSI2_SK);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> toMap(final Object object) {
    return (Map<String, Object>) object;
  }

  /**
   * Write Data to Typesense Index.
   * 
   * @param logger {@link LambdaLogger}
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param data {@link Map}
   * @throws IOException IOException
   */
  private void writeToIndex(final LambdaLogger logger, final String siteId, final String documentId,
      final Map<String, Object> data) throws IOException {

    if (this.debug) {
      logger.log("writing to index: " + data);
    }

    boolean isDocument = data.get(SK).toString().contains("document");
    // boolean isTag = data.get(SK).toString().contains("tags" + TAG_DELIMINATOR);

    removeDynamodbKeys(data);

    if (isDocument) {
      Map<String, Object> document = new DocumentMapToDocument().apply(data);
      document = this.fulltext.apply(document);
      addOrUpdate(siteId, document.get("documentId").toString(), document);
    } else if (this.debug) {
      logger.log("skipping dynamodb record");
    }

    // if (isTag) {
    // Document document = new DocumentTagMapToDocument().apply(data);
    // addOrUpdate(siteId, document);
    // }
  }

  /**
   * Add or Update Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param data {@link Map}
   * @throws IOException IOException
   */
  public void addOrUpdate(final String siteId, final String documentId,
      final Map<String, Object> data) throws IOException {

    HttpResponse<String> response = this.typeSenseService.addDocument(siteId, documentId, data);

    if (!is2XX(response)) {

      if (is404(response)) {

        response = this.typeSenseService.addCollection(siteId);
        if (!is2XX(response)) {
          throw new IOException(response.body());
        }

        response = this.typeSenseService.addDocument(siteId, documentId, data);
        if (!is2XX(response)) {
          throw new IOException(response.body());
        }

      } else if (is409(response) || is429(response)) {
        this.typeSenseService.updateDocument(siteId, documentId, data);
      } else {
        throw new IOException(response.body());
      }
    }
  }
}
