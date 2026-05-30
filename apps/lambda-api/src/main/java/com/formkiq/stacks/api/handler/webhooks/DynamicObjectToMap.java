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
package com.formkiq.stacks.api.handler.webhooks;

import com.formkiq.aws.dynamodb.DynamicObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;

/**
 * {@link Function} transform {@link DynamicObject} to {@link Map}.
 */
public class DynamicObjectToMap implements Function<DynamicObject, Map<String, Object>> {

  /** Url. */
  private final String url;
  /** SiteId. */
  private final String site;

  /**
   * constructor.
   * 
   * @param siteId {@link String}
   * @param webhookUrl {@link String}
   */
  public DynamicObjectToMap(final String siteId, final String webhookUrl) {
    this.url = webhookUrl;
    this.site = siteId;
  }

  @Override
  public Map<String, Object> apply(final DynamicObject m) {

    Map<String, Object> map = new HashMap<>();
    map.put("siteId", this.site != null ? this.site : DEFAULT_SITE_ID);
    map.put("webhookId", m.getString("documentId"));
    map.put("name", m.getString("path"));
    map.put("url", this.url);
    map.put("insertedDate", m.getString("inserteddate"));
    map.put("userId", m.getString("userId"));
    map.put("enabled", m.getString("enabled"));
    map.put("ttl", m.getString("ttl"));

    return map;
  }
}
