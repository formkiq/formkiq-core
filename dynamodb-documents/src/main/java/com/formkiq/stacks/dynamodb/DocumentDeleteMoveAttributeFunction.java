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

import static com.formkiq.stacks.dynamodb.DocumentService.SOFT_DELETE;
import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.MoveAttributeFunction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Delete Document {@link MoveAttributeFunction}.
 */
public class DocumentDeleteMoveAttributeFunction implements MoveAttributeFunction, DbKeys {

  /** Site Id. */
  private String siteId;
  /** Document Id. */
  private String documentId;

  /**
   * constructor.
   * 
   * @param siteIdParam {@link String}
   * @param id {@link String}
   */
  public DocumentDeleteMoveAttributeFunction(final String siteIdParam, final String id) {
    this.siteId = siteIdParam;
    this.documentId = id;
  }

  @Override
  public Map<String, AttributeValue> transform(final Map<String, AttributeValue> attr) {
    Map<String, AttributeValue> a = new HashMap<>(attr);

    String pk = a.get(PK).s();
    String sk = a.get(SK).s();

    if ("document".equals(sk)) {

      Map<String, AttributeValue> sdKeys =
          keysGeneric(this.siteId, SOFT_DELETE + PREFIX_DOCS, null);
      a.put(PK, sdKeys.get(PK));
      a.put(SK, AttributeValue.fromS(SOFT_DELETE + sk + "#" + this.documentId));

    } else {

      a.put(PK, AttributeValue.fromS(SOFT_DELETE + pk));
      a.put(SK, AttributeValue.fromS(SOFT_DELETE + sk));
    }

    return a;
  }

}
