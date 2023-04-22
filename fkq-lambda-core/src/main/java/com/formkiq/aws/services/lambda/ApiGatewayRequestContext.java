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
  /** Domain Name. */
  private String domainName;
  /** {@link Map} of Identity. */
  private Map<String, Object> identity;
  /** Protocol. */
  private String protocol;
  /** Request Id. */
  private String requestId;
  /** Request Time. */
  private String requestTime;

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
   * Get Domain Name.
   * 
   * @return {@link String}
   */
  public String getDomainName() {
    return this.domainName;
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
   * Get Protocol.
   * 
   * @return {@link String}
   */
  public String getProtocol() {
    return this.protocol;
  }

  /**
   * Get Request Id.
   * 
   * @return {@link String}
   */
  public String getRequestId() {
    return this.requestId;
  }

  /**
   * Get Request Time.
   * 
   * @return {@link String}
   */
  public String getRequestTime() {
    return this.requestTime;
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
   * Set Domain Name.
   * 
   * @param domain {@link String}
   */
  public void setDomainName(final String domain) {
    this.domainName = domain;
  }

  /**
   * Set Identity.
   * 
   * @param map {@link Map}
   */
  public void setIdentity(final Map<String, Object> map) {
    this.identity = map;
  }

  /**
   * Set Procotol.
   * 
   * @param requestProtocol {@link String}
   */
  public void setProtocol(final String requestProtocol) {
    this.protocol = requestProtocol;
  }

  /**
   * Set Request Id.
   * 
   * @param id {@link String}
   */
  public void setRequestId(final String id) {
    this.requestId = id;
  }

  /**
   * Set Request Time.
   * 
   * @param time {@link String}
   */
  public void setRequestTime(final String time) {
    this.requestTime = time;
  }
}
