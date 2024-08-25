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
package com.formkiq.stacks.api.transformers;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.stacks.api.handler.AddDocumentRequest;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;

/**
 * Convert {@link AddDocumentRequest} to {@link DocumentItem}.
 */
public class AddDocumentRequestToDocumentItem
    implements Function<AddDocumentRequest, DocumentItem> {

  /** BelongsToDocumentId. */
  private String belongsToDocumentId;
  /** User Id. */
  private String userId;
  /** {@link DocumentItem}. */
  private DocumentItem existing;

  /**
   * constructor.
   * 
   * @param existingDocument {@link DocumentItem}
   * @param username {@link String}
   * @param parentDocumentId {@link String}
   */
  public AddDocumentRequestToDocumentItem(final DocumentItem existingDocument,
      final String username, final String parentDocumentId) {
    this.belongsToDocumentId = parentDocumentId;
    this.userId = username;
    this.existing = existingDocument;
  }

  @Override
  public DocumentItem apply(final AddDocumentRequest r) {

    DocumentItem item = convert(r);

    List<DocumentItem> childs = new ArrayList<>();

    for (AddDocumentRequest childReq : notNull(r.getDocuments())) {
      DocumentItem childDoc = convert(childReq);
      childs.add(childDoc);
    }

    if (!childs.isEmpty()) {
      item.setDocuments(childs);
    }

    return item;
  }

  private DocumentItem convert(final AddDocumentRequest r) {

    DocumentItem item = this.existing != null ? this.existing : new DocumentItemDynamoDb();

    item.setBelongsToDocumentId(this.belongsToDocumentId);

    if (!isEmpty(r.getContentType())) {
      item.setContentType(r.getContentType());
    }

    if (!isEmpty(r.getDeepLinkPath())) {
      item.setDeepLinkPath(r.getDeepLinkPath());
    }

    if (this.existing == null) {
      item.setDocumentId(r.getDocumentId());
      item.setUserId(this.userId);
    } else {
      item.setLastModifiedDate(new Date());
    }

    if (!notNull(r.getMetadata()).isEmpty()) {
      item.setMetadata(r.getMetadata());
    }

    if (!isEmpty(r.getPath())) {
      item.setPath(r.getPath());
    }

    return item;
  }

}
