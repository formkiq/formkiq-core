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
package com.formkiq.aws.dynamodb.documents;

import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.MoveAttributeFunction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static com.formkiq.aws.dynamodb.documents.DocumentDeleteMoveAttributeFunction.SOFT_DELETE;

/**
 * Delete Document {@link MoveAttributeFunction}.
 */
public class DocumentRestoreMoveAttributeFunction implements MoveAttributeFunction, DbKeys {

  /** Site Id. */
  private final String siteId;
  /** Document. */
  private final DocumentArtifact document;

  /**
   * constructor.
   * 
   * @param siteIdParam {@link String}
   * @param documentArtifact {@link DocumentArtifact}
   */
  public DocumentRestoreMoveAttributeFunction(final String siteIdParam,
      final DocumentArtifact documentArtifact) {
    this.siteId = siteIdParam;
    this.document = documentArtifact;
  }

  @Override
  public Map<String, AttributeValue> transform(final Map<String, AttributeValue> attr) {
    Map<String, AttributeValue> a = new HashMap<>(attr);

    String pk = a.get(PK).s();
    String sk = a.get(SK).s();

    if (sk.startsWith(SOFT_DELETE + "document")) {

      var key = new DocumentRecordBuilder().document(this.document).buildKey(this.siteId).toMap();

      a.put(PK, key.get(PK));
      a.put(SK, key.get(SK));

      if (a.containsKey(GSI1_PK)) {
        a.put(GSI1_PK, AttributeValue.fromS(a.get(GSI1_PK).s().replaceAll(SOFT_DELETE, "")));
        a.put(GSI1_SK, AttributeValue.fromS(a.get(GSI1_SK).s().replaceAll(SOFT_DELETE, "")));
      }

    } else {

      a.put(PK, AttributeValue.fromS(pk.replaceAll(SOFT_DELETE, "")));
      a.put(SK, AttributeValue.fromS(sk.replaceAll(SOFT_DELETE, "")));

      if (a.containsKey(GSI1_PK)) {
        a.put(GSI1_PK, AttributeValue.fromS(a.get(GSI1_PK).s().replaceAll(SOFT_DELETE, "")));
        a.put(GSI1_SK, AttributeValue.fromS(a.get(GSI1_SK).s().replaceAll(SOFT_DELETE, "")));
      }

      if (a.containsKey(GSI2_PK)) {
        a.put(GSI2_PK, AttributeValue.fromS(a.get(GSI2_PK).s().replaceAll(SOFT_DELETE, "")));
        a.put(GSI2_SK, AttributeValue.fromS(a.get(GSI2_SK).s().replaceAll(SOFT_DELETE, "")));
      }
    }

    return a;
  }

}
