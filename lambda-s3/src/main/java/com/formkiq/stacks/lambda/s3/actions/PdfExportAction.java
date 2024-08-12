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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.module.actions.Action;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.validation.ValidationException;

import java.io.IOException;
import java.util.List;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * PdfExport implementation of {@link DocumentAction}.
 */
public class PdfExportAction implements DocumentAction {

  /** Google Docs Prefix. */
  private static final String GOOGLE_DOCS_PREFIX = "https://docs.google.com/document/d/";

  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link ConfigService}. */
  private final ConfigService configService;
  /** {@link SendHttpRequest}. */
  private final SendHttpRequest sendHttpRequest;

  /**
   * constructor.
   *
   * @param serviceCache {@link AwsServiceCache}
   */
  public PdfExportAction(final AwsServiceCache serviceCache) {
    this.configService = serviceCache.getExtension(ConfigService.class);
    this.documentService = serviceCache.getExtension(DocumentService.class);
    this.sendHttpRequest = new SendHttpRequest(serviceCache);
  }

  @Override
  public void run(final LambdaLogger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException, ValidationException {

    DocumentItem item = this.documentService.findDocument(siteId, documentId);
    String deepLink = item.getDeepLinkPath();

    if (isValid(siteId, deepLink)) {

      String url = String.format("/integrations/google/drive/documents/%s/export", documentId);
      this.sendHttpRequest.sendRequest(siteId, "POST", url, "{\"outputType\": \"PDF\"}");
    } else {
      throw new IllegalArgumentException("PdfExport only supports Google DeepLink");
    }
  }

  private boolean isValid(final String siteId, final String deepLink) {
    boolean valid = !Strings.isEmpty(deepLink) && deepLink.startsWith(GOOGLE_DOCS_PREFIX);

    DynamicObject obj = configService.get(siteId);
    String googleWorkloadIdentityAudience = obj.getString("googleWorkloadIdentityAudience");
    String googleWorkloadIdentityServiceAccount =
        obj.getString("googleWorkloadIdentityServiceAccount");

    if (isEmpty(googleWorkloadIdentityAudience) || isEmpty(googleWorkloadIdentityServiceAccount)) {
      throw new IllegalArgumentException("Google Workload Identity is not configured");
    }

    return valid;
  }
}
