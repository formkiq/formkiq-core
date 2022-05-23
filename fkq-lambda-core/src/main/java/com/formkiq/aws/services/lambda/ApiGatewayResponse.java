package com.formkiq.aws.services.lambda;

import java.util.Map;

/**
 * 
 * API Gateway Response.
 *
 */
public class ApiGatewayResponse {
  /** Headers. */
  private Map<String, String> headers;
  /** Response Body. */
  private String body;
  /** Response Status Code. */
  private int statusCode;
  
  /**
   * constructor.
   */
  public ApiGatewayResponse() {
    
  }

  /**
   * Get Body.
   * @return {@link String}
   */
  public String getBody() {
    return this.body;
  }

  /**
   * Get Headers.
   * @return {@link Map}
   */
  public Map<String, String> getHeaders() {
    return this.headers;
  }

  /**
   * Get Status Code.
   * @return int
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * Set Body.
   * @param s {@link String}
   */
  public void setBody(final String s) {
    this.body = s;
  }

  /**
   * Set Headers.
   * @param map {@link Map}
   */
  public void setHeaders(final Map<String, String> map) {
    this.headers = map;
  }

  /**
   * Set Status Code.
   * @param status int
   */
  public void setStatusCode(final int status) {
    this.statusCode = status;
  }
}