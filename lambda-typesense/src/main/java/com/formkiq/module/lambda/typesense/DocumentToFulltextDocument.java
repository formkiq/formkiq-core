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
package com.formkiq.module.lambda.typesense;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_METADATA;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link Function} for converting {@link Map} to {@link Map}.
 *
 */
public class DocumentToFulltextDocument
    implements Function<Map<String, Object>, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(final Map<String, Object> document) {

    String text = buildText(document);
    document.put("text", text);
    // document.add(new TextField("text", text, Field.Store.NO));

    return document;
  }

  /**
   * Build Full Text.
   * 
   * @param document {@link Map}
   * @return {@link String}
   */
  private String buildText(final Map<String, Object> document) {
    StringBuilder sb = new StringBuilder();

    for (String key : Arrays.asList("path", "userId")) {
      Object val = document.get(key);
      if (val != null && !StringUtils.isEmpty(val.toString())) {
        sb.append(val + " ");
      }
    }

    if (document.containsKey("path")) {
      String[] ss = document.get("path").toString().split("/");
      if (ss.length > 1) {
        String s = Arrays.asList(ss).stream().collect(Collectors.joining(" "));
        sb.append(s);
      }
    }

    List<String> metadata =
        document.entrySet().stream().filter(e -> e.getKey().startsWith(PREFIX_DOCUMENT_METADATA))
            .map(e -> e.getValue().toString()).collect(Collectors.toList());

    metadata.forEach(d -> sb.append(d));

    return sb.toString().trim();
  }
}
