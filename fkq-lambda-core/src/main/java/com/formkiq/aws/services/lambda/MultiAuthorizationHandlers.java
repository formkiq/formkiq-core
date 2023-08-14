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
package com.formkiq.aws.services.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/**
 * 
 * Implementation that supports multiple {@link AuthorizationHandler}. If any
 * {@link AuthorizationHandler} is not authorized, they will all fail.
 *
 */
public class MultiAuthorizationHandlers implements AuthorizationHandler {

  /** {@link List} {@link AuthorizationHandler}. */
  private List<AuthorizationHandler> handlers;

  /**
   * constructor.
   * 
   * @param authorizationHandlers {@link List} {@link AuthorizationHandler}
   */
  public MultiAuthorizationHandlers(final List<AuthorizationHandler> authorizationHandlers) {
    this.handlers = authorizationHandlers;
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsServices,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {

    List<Optional<Boolean>> results = new ArrayList<>();

    for (AuthorizationHandler handler : this.handlers) {
      Optional<Boolean> authorized = handler.isAuthorized(awsServices, event, authorization);
      results.add(authorized);
    }

    Optional<Boolean> o = Optional.empty();
    List<Optional<Boolean>> list = results.stream()
        .filter(r -> r.isPresent() && !r.get().booleanValue()).collect(Collectors.toList());

    if (!list.isEmpty()) {
      o = Optional.of(Boolean.FALSE);
    } else {
      list = results.stream().filter(r -> r.isPresent() && r.get().booleanValue())
          .collect(Collectors.toList());
      o = !list.isEmpty() ? Optional.of(Boolean.TRUE) : Optional.empty();
    }

    return o;
  }
}
