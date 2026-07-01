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
package com.formkiq.aws.dynamodb.documentattributes;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.entity.FindEntityById;
import com.formkiq.aws.dynamodb.entity.RetentionMode;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_MODE;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_POLICY;
import static com.formkiq.aws.dynamodb.entity.RetentionMode.GOVERNANCE;
import static com.formkiq.aws.dynamodb.entity.RetentionMode.RETENTION_ONLY;

/**
 * {@link Function} to determine {@link RetentionMode}.
 */
public class DocumentAttributeHasRetentionMode
    implements BiFunction<String, DocumentAttributeRecord, RetentionMode> {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   *
   * @param dbService {@link DynamoDbService}
   */
  public DocumentAttributeHasRetentionMode(final DynamoDbService dbService) {
    this.db = dbService;
  }

  @Override
  public RetentionMode apply(final String siteId, final DocumentAttributeRecord docAttr) {

    if (isRetentionKey(docAttr)) {

      var entityValue = docAttr.getStringValue();
      var documentAttributeEntityKeyValue = DocumentAttributeEntityKeyValue.fromString(entityValue);

      var retentionEntity =
          new FindEntityById().find(this.db, siteId, documentAttributeEntityKeyValue);

      if (retentionEntity != null) {
        var retentionMode =
            DynamoDbTypes.toString(retentionEntity.attributes().get(RETENTION_MODE.getKey()));
        return GOVERNANCE.name().equals(retentionMode) ? GOVERNANCE : RETENTION_ONLY;
      }

      return RETENTION_ONLY;
    }

    return null;
  }

  private boolean isRetentionKey(final DocumentAttributeRecord docAttr) {
    if (docAttr == null) {
      return false;
    }

    String key = docAttr.getKey();
    return RETENTION_POLICY.getKey().equals(key);
  }
}
