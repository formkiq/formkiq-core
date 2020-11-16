/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.lambda.apigateway;

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
