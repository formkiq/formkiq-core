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
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DocumentAction for DataClassification {@link Action}.
 */
public class SetDataClassificationAction implements DocumentAction {

  /** {@link SendHttpRequest}. */
  private final SendHttpRequest http;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();
  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link ActionsService}. */
  private final ActionsService actionsService;

  /**
   * constructor.
   *
   * @param serviceCache {@link AwsServiceCache}
   */
  public SetDataClassificationAction(final AwsServiceCache serviceCache) {
    this.http = new SendHttpRequest(serviceCache);
    this.actionsService = serviceCache.getExtension(ActionsService.class);
    this.documentService = serviceCache.getExtension(DocumentService.class);
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException {

    ActionStatus status = ActionStatus.COMPLETE;

    DocumentItem item = this.documentService.findDocument(siteId, documentId);
    if (!MimeType.isPlainText(item.getContentType())
        && actions.stream().noneMatch(a -> a.type().equals(ActionType.OCR))) {

      Action ocrAction = new Action().userId("System").type(ActionType.OCR)
          .parameters(Map.of("ocrEngine", "tesseract"));
      this.actionsService.insertBeforeAction(siteId, documentId, actions, action, ocrAction);
      status = ActionStatus.PENDING;

    } else {

      Map<String, Object> payload = buildPayload(action);
      String json = this.gson.toJson(payload);

      if (logger.isLogged(LogLevel.DEBUG)) {
        String s = String.format(
            "{\"type\",\"%s\",\"method\":\"%s\",\"url\":\"%s\",\"body\":\"%s\"}",
            action.type().name(), "PUT", "/documents/" + documentId + "/dataClassification", json);
        logger.debug(s);
      }

      this.http.sendRequest(siteId, "put", "/documents/" + documentId + "/dataClassification",
          json);
    }

    return new ProcessActionStatus(status);
  }

  private Map<String, Object> buildPayload(final Action action) {
    Map<String, Object> parameters =
        action.parameters() != null ? action.parameters() : new HashMap<>();
    return Map.of("llmPromptEntityName", parameters.get("llmPromptEntityName"));
  }
}
