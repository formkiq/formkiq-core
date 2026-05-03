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
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import com.formkiq.validation.ValidationException;

import java.io.IOException;
import java.util.List;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Move a document to another folder.
 */
public class MoveAction implements DocumentAction {

  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link FolderIndexProcessor}. */
  private final FolderIndexProcessor folderIndexProcessor;

  /**
   * constructor.
   *
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public MoveAction(final AwsServiceCache awsServiceCache) {
    this.documentService = awsServiceCache.getExtension(DocumentService.class);
    this.folderIndexProcessor = awsServiceCache.getExtension(FolderIndexProcessor.class);
  }

  private String normalizeTargetPath(final Action action) {
    String path = action.parameters().get("path").toString().trim();
    path = Strings.removeBackSlashes(path);
    return !isEmpty(path) ? path + "/" : "";
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId,
      final DocumentArtifact document, final List<Action> actions, final Action action)
      throws IOException, ValidationException {

    DocumentRecord item = this.documentService.findDocument(siteId, document);
    if (item == null) {
      throw new IOException("document '" + document.documentId() + "' does not exist");
    }

    String sourcePath = item.path();
    if (isEmpty(sourcePath)) {
      throw new IOException("document '" + document.documentId() + "' does not have a path");
    }

    this.folderIndexProcessor.moveIndex(siteId, sourcePath, normalizeTargetPath(action),
        action.userId());

    return new ProcessActionStatus(ActionStatus.COMPLETE);
  }
}
