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

import java.util.Map;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Api Gateway Request Context.
 *
 */
@Reflectable
public class ApiGatewayRequestContext {

  /** {@link Map} of Authorizer. */
  private Map<String, Object> authorizer;
  /** {@link Map} of Identity. */
  private Map<String, Object> identity;

  /**
   * constructor.
   */
  public ApiGatewayRequestContext() {}

  /**
   * Get Authorizer.
   * 
   * @return {@link Map}
   */
  public Map<String, Object> getAuthorizer() {
    return this.authorizer;
  }

  /**
   * Set Authorizer.
   * 
   * @param map {@link Map}
   */
  public void setAuthorizer(final Map<String, Object> map) {
    this.authorizer = map;
  }

  /**
   * Get Identity.
   * 
   * @return {@link Map}
   */
  public Map<String, Object> getIdentity() {
    return this.identity;
  }

  /**
   * Set Identity.
   * 
   * @param map {@link Map}
   */
  public void setIdentity(final Map<String, Object> map) {
    this.identity = map;
  }
}
