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

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import com.formkiq.validation.ValidationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delete a document.
 */
public class DeleteAction implements DocumentAction {

  /** {@link SendHttpRequest}. */
  private final SendHttpRequest http;

  /**
   * constructor.
   *
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public DeleteAction(final AwsServiceCache awsServiceCache) {
    this.http = new SendHttpRequest(awsServiceCache);
  }

  private Map<String, String> getParameters(final DocumentArtifact document) {
    Map<String, String> parameters = new HashMap<>();
    if (document.artifactId() != null) {
      parameters.put("artifactId", document.artifactId());
    }
    return parameters;
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId,
      final DocumentArtifact document, final List<Action> actions, final Action action)
      throws IOException, ValidationException {

    String deleteType = (String) action.parameters().get("deleteType");
    boolean softDelete = "SOFT_DELETE".equalsIgnoreCase(deleteType);
    boolean purge = "PURGE".equalsIgnoreCase(deleteType);

    if (purge) {
      this.http.sendRequest(siteId, "DELETE", "/documents/" + document.documentId() + "/purge", "",
          getParameters(document));
    } else {
      Map<String, String> parameters = getParameters(document);
      parameters.put("softDelete", Boolean.toString(softDelete));
      this.http.sendRequest(siteId, "DELETE", "/documents/" + document.documentId(), "",
          parameters);
    }

    return new ProcessActionStatus(ActionStatus.COMPLETE);
  }
}
