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
package com.formkiq.module.actions.services;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;

import java.util.Map;
import java.util.function.Function;

/**
 * Convert {@link Map} to {@link Action}.
 */
public class MapToAction implements Function<Map<String, Object>, Action> {

  @Override
  public Action apply(final Map<String, Object> map) {

    String userId = ApiAuthorization.getAuthorization().getUsername();

    Object stype = map.get("type");
    String queueId = (String) map.get("queueId");

    ActionType type;
    try {
      type = stype != null ? ActionType.valueOf(stype.toString().toUpperCase()) : null;
    } catch (IllegalArgumentException e) {
      type = null;
    }

    Map<String, String> parameters = (Map<String, String>) map.get("parameters");
    return new Action().queueId(queueId).type(type).parameters(parameters).userId(userId);
  }
}
