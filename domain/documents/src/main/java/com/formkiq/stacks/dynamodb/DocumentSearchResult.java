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

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.graalvm.annotations.Reflectable;

import java.util.List;
import java.util.Map;

/**
 * A document returned by search together with search-specific metadata.
 *
 * @param documentRecord {@link DocumentRecord}
 * @param matchedAttribute attribute value that caused the document to match
 */
@Reflectable
public record DocumentSearchResult(DocumentRecord documentRecord,
    DocumentAttributeRecord matchedAttribute, Map<String, Object> attributeFields,
    DocumentTag matchedTag, List<DocumentTag> matchedTags, DocumentSearchFolderIndex folderIndex,
    String value) {

  /**
   * Canonical constructor.
   */
  public DocumentSearchResult {
  }

  public DocumentSearchResult(final String documentSearchValue) {
    this(null, null, null, null, null, null, documentSearchValue);
  }

  public DocumentSearchResult(final DocumentRecord item) {
    this(item, null, null, null, null, null, null);
  }

  public DocumentSearchResult(final DocumentRecord item,
      final DocumentAttributeRecord attributeRecord) {
    this(item, attributeRecord, null, null, null, null, null);
  }

  public DocumentSearchResult(final DocumentRecord item, final DocumentTag documentMatchedTag,
      final List<DocumentTag> documentMatchedTags) {
    this(item, null, null, documentMatchedTag, documentMatchedTags, null, null);
  }

  public DocumentSearchResult(final DocumentRecord item,
      final DocumentSearchFolderIndex documentSearchFolderIndex) {
    this(item, null, null, null, null, documentSearchFolderIndex, null);
  }

  public DocumentSearchResult(final DocumentSearchResult item,
      final Map<String, Object> documentAttributeFields) {
    this(item.documentRecord, item.matchedAttribute, documentAttributeFields, item.matchedTag,
        item.matchedTags, item.folderIndex, item.value);
  }
}
