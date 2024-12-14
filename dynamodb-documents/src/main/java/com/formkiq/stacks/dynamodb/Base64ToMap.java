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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Convert Base 64 {@link String} to {@link java.util.Map}.
 */
public class Base64ToMap implements Function<String, Map<String, Object>> {
  @Override
  public Map<String, Object> apply(final String base64) {
    Map<String, Object> map = null;

    if (!isEmpty(base64)) {
      map = new HashMap<>();

      String decodedString = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
      String[] entries = decodedString.split("\n");
      for (String entry : entries) {
        if (!entry.isEmpty()) {
          String[] keyValue = entry.split("=", 2);
          map.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
        }
      }
    }

    return map;
  }
}
