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
package com.formkiq.stacks.dynamodb.s3;

import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.stacks.dynamodb.DocumentStorageService;

/**
 * Document Storage Service for S3.
 */
public class DocumentStorageServiceS3 implements DocumentStorageService {

  /** S3 Service. */
  private final S3Service s3;
  /** S3 Bucket. */
  private final String bucket;

  /**
   * constructor.
   *
   * @param s3Bucket {@link String}
   * @param s3Service {@link S3Service}
   */
  public DocumentStorageServiceS3(final String s3Bucket, final S3Service s3Service) {
    this.s3 = s3Service;
    this.bucket = s3Bucket;
  }

  @Override
  public void deleteObjectLock(final String siteId, final DocumentArtifact document) {
    var s3Key = SiteIdKeyGenerator.createS3Key(siteId, document);
    this.s3.removeObjectRetention(bucket, s3Key, null);
  }
}
