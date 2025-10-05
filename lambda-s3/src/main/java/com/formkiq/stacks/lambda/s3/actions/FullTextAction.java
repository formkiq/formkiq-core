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
package com.formkiq.stacks.lambda.s3.actions;

import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMapToDocument;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.DocumentContentFunction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.module.http.HttpResponseStatus.is2XX;

/**
 * Fulltext {@link DocumentAction}.
 */
public class FullTextAction implements DocumentAction {

  /** Default Maximum for Typesense Content. */
  private static final int DEFAULT_TYPESENSE_CHARACTER_MAX = 32768;
  /** {@link ActionsService}. */
  private final ActionsService actionsService;
  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link DocumentContentFunction}. */
  private final DocumentContentFunction documentContentFunc;
  /** {@link SendHttpRequest}. */
  private final SendHttpRequest http;
  /** {@link TypeSenseService}. */
  private final TypeSenseService typesense;
  /** Has Module Fulltext. */
  boolean moduleFulltext;
  /** Has Module Typesense. */
  boolean moduleTypesense;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   * 
   * @param serviceCache {@link AwsServiceCache}
   */
  public FullTextAction(final AwsServiceCache serviceCache) {
    this.actionsService = serviceCache.getExtension(ActionsService.class);
    this.documentService = serviceCache.getExtension(DocumentService.class);
    this.documentContentFunc = new DocumentContentFunction(serviceCache);
    this.typesense = serviceCache.getExtensionOrNull(TypeSenseService.class);
    this.moduleFulltext = serviceCache.hasModule("opensearch");
    this.moduleTypesense = serviceCache.hasModule("typesense");
    this.http = new SendHttpRequest(serviceCache);
  }

  private void debug(final Logger logger, final String siteId, final DocumentItem item) {
    if (logger.isLogged(LogLevel.DEBUG)) {
      String s = String.format(
          "{\"siteId\": \"%s\",\"documentId\": \"%s\",\"path\": \"%s\",\"userId\": \"%s\","
              + "\"s3Version\": \"%s\",\"contentType\": \"%s\"}",
          siteId, item.getDocumentId(), item.getPath(), item.getUserId(), item.getS3version(),
          item.getContentType());

      logger.debug(s);
    }
  }

  private int getCharacterMax(final Action action) {
    Map<String, Object> parameters = notNull(action.parameters());
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

    return characterMax != -1 && sb.length() > characterMax ? sb.substring(0, characterMax)
        : sb.toString();
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException {

    ActionStatus status = ActionStatus.PENDING;
    DocumentItem item = this.documentService.findDocument(siteId, documentId);
    debug(logger, siteId, item);

    List<String> contentUrls = this.documentContentFunc.getContentUrls(logger, siteId, item);

    if (!contentUrls.isEmpty()) {

      if (this.moduleFulltext) {

        try {
          updateOpensearchFulltext(siteId, documentId, contentUrls);
        } catch (InterruptedException e) {
          throw new IOException(e);
        }

        status = ActionStatus.COMPLETE;
      }

      if (this.moduleTypesense) {
        updateTypesense(this.documentContentFunc, siteId, documentId, action, contentUrls);
        status = ActionStatus.COMPLETE;
      }

      if (!ActionStatus.COMPLETE.equals(status)) {
        status = ActionStatus.FAILED;
      }

    } else if (actions.stream().filter(a -> a.type().equals(ActionType.OCR)).findAny().isEmpty()) {

      Action ocrAction = new Action().userId("System").type(ActionType.OCR)
          .parameters(Map.of("ocrEngine", "tesseract"));
      this.actionsService.insertBeforeAction(siteId, documentId, actions, action, ocrAction);

    } else {
      throw new IOException("no OCR document found");
    }

    return new ProcessActionStatus(status);
  }

  /**
   * Update Document Content to Opensearch.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param contentUrls {@link List} {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void updateOpensearchFulltext(final String siteId, final String documentId,
      final List<String> contentUrls) throws IOException, InterruptedException {

    final int sleep = 500;
    Map<String, Object> payload = Map.of("contentUrls", contentUrls);

    try {
      this.http.sendRequest(siteId, "patch", "/documents/" + documentId + "/fulltext",
          this.gson.toJson(payload));
    } catch (IOException ex1) {

      if (ex1.getMessage().contains(" 404")) {

        TimeUnit.MILLISECONDS.sleep(sleep);

        try {
          this.http.sendRequest(siteId, "post", "/documents/" + documentId + "/fulltext",
              this.gson.toJson(payload));
        } catch (IOException ex2) {

          if (ex2.getMessage().contains(" 409")) {
            TimeUnit.MILLISECONDS.sleep(sleep);
            this.http.sendRequest(siteId, "patch", "/documents/" + documentId + "/fulltext",
                this.gson.toJson(payload));
          } else {
            throw ex2;
          }
        }

      } else {
        throw ex1;
      }
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

      String content = getContent(dcFunc, action, contentUrls);
      Map<String, String> data = Map.of("content", content);

      Map<String, Object> document = new DocumentMapToDocument().apply(data);

      HttpResponse<String> response =
          this.typesense.addOrUpdateDocument(siteId, documentId, document);

      if (!is2XX(response)) {
        throw new IOException(response.body());
      }

    } catch (URISyntaxException | InterruptedException e) {
      throw new IOException(e);
    }
  }
}
