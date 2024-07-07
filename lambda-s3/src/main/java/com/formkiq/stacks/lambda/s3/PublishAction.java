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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.module.actions.Action;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.validation.ValidationException;

import java.io.IOException;
import java.util.List;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Publish {@link DocumentAction}.
 */
public class PublishAction implements DocumentAction {

  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link S3Service}. */
  private final S3Service s3;
  /** Documents S3 Bucket. */
  private final String documentsS3Bucket;

  /**
   * constructor.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public PublishAction(final AwsServiceCache awsServiceCache) {
    this.documentService = awsServiceCache.getExtension(DocumentService.class);
    this.s3 = awsServiceCache.getExtension(S3Service.class);
    this.documentsS3Bucket = awsServiceCache.environment("DOCUMENTS_S3_BUCKET");
  }

  @Override
  public void run(final LambdaLogger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException, ValidationException {

    DocumentItem item = this.documentService.findDocument(siteId, documentId);

    String s3key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
    String s3version =
        this.s3.getObjectMetadata(this.documentsS3Bucket, s3key, null).getVersionId();
    String contentType = !isEmpty(item.getContentType()) ? item.getContentType()
        : MimeType.MIME_OCTET_STREAM.getContentType();

    this.documentService.publishDocument(siteId, documentId, s3version, item.getPath(), contentType,
        action.userId());
  }
}
