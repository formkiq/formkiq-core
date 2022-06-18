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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link Action}.
 *
 */
public class AttributeValueToAction implements Function<Map<String, AttributeValue>, Action> {

  @Override
  public Action apply(final Map<String, AttributeValue> map) {

    Action action = new Action();

    String userId = map.containsKey("userId") ? map.get("userId").s() : null;
    action.userId(userId);

    String type = map.containsKey("type") ? map.get("type").s() : null;
    action.type(type != null ? ActionType.valueOf(type.toUpperCase()) : null);

    String status = map.containsKey("status") ? map.get("status").s() : null;
    action
        .status(status != null ? ActionStatus.valueOf(status.toUpperCase()) : ActionStatus.PENDING);

    if (map.containsKey("parameters")) {
      Map<String, String> parameters = map.get("parameters").m().entrySet().stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().s()));
      action.parameters(parameters);
    }
    
    return action;
  }
}
