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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 
 * {@link Function} to convert {@link DocumentTag} to {@link DynamicDocumentTag}.
 *
 */
public class DocumentTagToDynamicDocumentTag implements Function<DocumentTag, DynamicDocumentTag> {

  @Override
  public DynamicDocumentTag apply(final DocumentTag tag) {
    Map<String, Object> map = new HashMap<>();
    map.put("documentId", tag.getDocumentId());
    map.put("key", tag.getKey());
    map.put("type", tag.getType().name());
    map.put("userId", tag.getUserId());
    
    if (tag.getValue() != null) {
      map.put("value", tag.getValue());      
    }
    
    if (tag.getValues() != null) {      
      map.put("values", tag.getValues());
    }

    return new DynamicDocumentTag(map);
  }

}
