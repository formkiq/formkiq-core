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

import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.s3.ChecksumCalculator;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calculate and persist a document checksum.
 */
public class ChecksumAction implements DocumentAction {

  /** Documents S3 Bucket. */
  private final String documentsS3Bucket;
  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link S3Service}. */
  private final S3Service s3;
  /** {@link ChecksumCalculator}. */
  private final ChecksumCalculator checksumCalculator = new ChecksumCalculator();

  /**
   * constructor.
   *
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public ChecksumAction(final AwsServiceCache awsServiceCache) {
    this.documentService = awsServiceCache.getExtension(DocumentService.class);
    this.s3 = awsServiceCache.getExtension(S3Service.class);
    this.documentsS3Bucket = awsServiceCache.environment("DOCUMENTS_S3_BUCKET");
  }

  private String calculateChecksum(final String checksumType, final byte[] bytes)
      throws IOException {
    try {
      return switch (checksumType) {
        case "SHA1" ->
          this.checksumCalculator.bytesToHex(this.checksumCalculator.calculateSha1(bytes));
        case "SHA256" ->
          this.checksumCalculator.bytesToHex(this.checksumCalculator.calculateSha256(bytes));
        default -> throw new IOException("unsupported checksumType '" + checksumType + "'");
      };
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("unable to calculate checksum", e);
    }
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId,
      final DocumentArtifact document, final List<Action> actions, final Action action)
      throws IOException, ValidationException {

    String s3key = SiteIdKeyGenerator.createS3Key(siteId, document);
    byte[] bytes = this.s3.getContentAsBytes(this.documentsS3Bucket, s3key);

    String checksumType =
        action.parameters().get("checksumType").toString().toUpperCase(Locale.ROOT);
    String checksum = calculateChecksum(checksumType, bytes);

    this.documentService.updateDocument(siteId, document, Map.of("checksum",
        AttributeValue.fromS(checksum), "checksumType", AttributeValue.fromS(checksumType)));

    return new ProcessActionStatus(ActionStatus.COMPLETE);
  }
}
