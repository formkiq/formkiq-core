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

import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.plugins.version.DocumentVersionService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * {@link DocumentVersionService} implementation.
 *
 */
@Reflectable
public class DocumentVersionServiceDynamoDb implements DocumentVersionService, DbKeys {

  /** DynamoDB Document Versions Table Name. */
  private String tableName = null;

  @Override
  public void addDocumentVersionAttributes(final Map<String, AttributeValue> previous,
      final Map<String, AttributeValue> current) {

    if (!previous.isEmpty()) {

      String version = current.getOrDefault(VERSION_ATTRIBUTE, AttributeValue.fromS("0")).s();
      String nextVersion = String.valueOf(Integer.parseInt(version) + 1);

      previous.put(VERSION_ATTRIBUTE, AttributeValue.fromS(version));
      previous.put(SK, AttributeValue.fromS(previous.get(SK).s() + "#v" + nextVersion));

      current.put(VERSION_ATTRIBUTE, AttributeValue.fromS(nextVersion));
    }
  }

  @Override
  public String getDocumentVersionsTableName() {
    return this.tableName;
  }

  @Override
  public void initialize(final Map<String, String> map) {
    this.tableName = map.get("DOCUMENT_VERSIONS_TABLE");
  }
}