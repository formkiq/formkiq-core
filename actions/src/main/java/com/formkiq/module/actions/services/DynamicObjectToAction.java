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
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link Action}.
 *
 */
public class DynamicObjectToAction implements Function<DynamicObject, Action> {

  @Override
  public Action apply(final DynamicObject obj) {

    Action action = new Action();

    String userId = obj.containsKey("userId") ? obj.getString("userId") : "System";
    action.userId(userId);

    String type = obj.containsKey("type") ? obj.getString("type") : null;
    action.type(type != null ? ActionType.valueOf(type.toUpperCase()) : null);

    String status = obj.containsKey("status") ? obj.getString("status") : null;
    action
        .status(status != null ? ActionStatus.valueOf(status.toUpperCase()) : ActionStatus.PENDING);

    if (obj.containsKey("parameters")) {
      Map<String, String> parameters = obj.getMap("parameters").entrySet().stream()
          .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));
      action.parameters(parameters);
    }

    return action;
  }
}
