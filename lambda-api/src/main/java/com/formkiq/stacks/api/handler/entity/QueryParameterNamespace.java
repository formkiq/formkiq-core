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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.validation.ValidationBuilder;

import java.util.List;
import java.util.function.Function;

/**
 * {@link Function} to convert {@link ApiGatewayRequestEvent} to {@link EntityTypeNamespace}.
 */
public class QueryParameterNamespace
    implements Function<ApiGatewayRequestEvent, EntityTypeNamespace> {

  @Override
  public EntityTypeNamespace apply(final ApiGatewayRequestEvent event) {
    EntityTypeNamespace ns = null;
    String namespace = event.getQueryStringParameter("namespace");

    if (namespace != null) {
      ValidationBuilder vb = new ValidationBuilder();
      vb.isEquals("namespace", namespace.toUpperCase(), List.of("PRESET", "CUSTOM"));
      vb.check();
      ns = EntityTypeNamespace.fromString(namespace);
    }

    return ns;
  }
}
