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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamodbVersionRecord;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * No versioning implementation of {@link DocumentVersionService}.
 *
 */
@Reflectable
public class DocumentVersionServiceNoVersioning implements DocumentVersionService {

  @Override
  public void deleteAllVersionIds(final String siteId, final String documentId) {
    // empty
  }

  @Override
  public String getDocumentVersionsTableName() {
    return null;
  }

  @Override
  public String getVersionId(final Map<String, AttributeValue> attrs) {
    return null;
  }

  @Override
  public Map<String, AttributeValue> get(final String siteId, final String documentId,
      final String versionKey) {
    return Collections.emptyMap();
  }

  @Override
  public void initialize(final Map<String, String> map,
      final DynamoDbConnectionBuilder connection) {
    // empty
  }

  @Override
  public List<Map<String, AttributeValue>> addRecords(final String siteId,
      final Collection<? extends DynamodbVersionRecord<?>> records) {
    // empty
    return null;
  }

  @Override
  public DocumentItem getDocumentItem(final DocumentService documentService, final String siteId,
      final String documentId, final String versionKey,
      final Map<String, AttributeValue> versionAttributes) {
    return documentService.findDocument(siteId, documentId);
  }

  @Override
  public DynamoDbService getDb() {
    return null;
  }
}
