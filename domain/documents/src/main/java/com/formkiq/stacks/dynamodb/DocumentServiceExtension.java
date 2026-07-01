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
package com.formkiq.stacks.dynamodb;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceExtension;
import com.formkiq.stacks.dynamodb.s3.DocumentStorageServiceS3;

import java.util.List;

import static com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorExtension.DEFAULT_PARENT_LAST_MODIFIED_UPDATE_INTERVAL_IN_MS;

/**
 * 
 * {@link AwsServiceExtension} for {@link DocumentService}.
 *
 */
public class DocumentServiceExtension implements AwsServiceExtension<DocumentService> {

  /** {@link DocumentService}. */
  private DocumentService service;

  /**
   * constructor.
   */
  public DocumentServiceExtension() {}

  @Override
  public DocumentService loadService(final AwsServiceCache awsServiceCache) {
    if (this.service == null) {
      DynamoDbConnectionBuilder connection =
          awsServiceCache.getExtension(DynamoDbConnectionBuilder.class);

      DocumentVersionService versionService =
          awsServiceCache.getExtension(DocumentVersionService.class);

      DocumentServiceInterceptor interceptor =
          awsServiceCache.getExtensionOrNull(DocumentServiceInterceptor.class);

      List<PresetEntity> presets = awsServiceCache.getExtensions(PresetEntity.class);

      S3ConnectionBuilder s3Connection = awsServiceCache.getExtension(S3ConnectionBuilder.class);
      S3Service s3 = new S3Service(s3Connection);

      var documentsBucket = awsServiceCache.environment("DOCUMENTS_S3_BUCKET");
      var documentStorageService = new DocumentStorageServiceS3(documentsBucket, s3);

      String lastModifiedInterval =
          awsServiceCache.environment("PARENT_LAST_MODIFIED_UPDATE_INTERVAL");
      long parentLastModifiedUpdateInterval =
          lastModifiedInterval != null ? Long.parseLong(lastModifiedInterval)
              : DEFAULT_PARENT_LAST_MODIFIED_UPDATE_INTERVAL_IN_MS;
      this.service = new DocumentServiceImpl(connection,
          awsServiceCache.environment("DOCUMENTS_TABLE"), presets, versionService, interceptor,
          documentStorageService, parentLastModifiedUpdateInterval);
    }

    return this.service;
  }
}
