package com.formkiq.testutils.aws;

import java.util.Map;

/**
 * 
 * Wrapper for Http Request data.
 *
 */
public class ApiHttpRequest {

  /** Http Body. */
  private String body;
  /** Http Group. */
  private String group;
  /** Http Method. */
  private String method;
  /** Http path. */
  private String path;
  /** {@link Map}. */
  private Map<String, String> pathParameters;
  /** {@link Map}. */
  private Map<String, String> queryParameters;
  /** Http Resource. */
  private String resource;
  /** Http Request user. */
  private String user;

  /**
   * constructor.
   */
  public ApiHttpRequest() {}

  /**
   * Get Http Body.
   * 
   * @return {@link String}
   */
  public String body() {
    return this.body;
  }

  /**
   * Set Http Body.
   * 
   * @param httpBody {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest body(final String httpBody) {
    this.body = httpBody;
    return this;
  }

  /**
   * Get Http Group.
   * 
   * @return {@link String}
   */
  public String group() {
    return this.group;
  }

  /**
   * Set Http Group.
   * 
   * @param httpGroup {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest group(final String httpGroup) {
    this.group = httpGroup;
    return this;
  }

  /**
   * Get HTTP method.
   * 
   * @return {@link String}
   */
  public String method() {
    return this.method;
  }

  /**
   * Set HTTP method.
   * 
   * @param httpMethod {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest method(final String httpMethod) {
    this.method = httpMethod;
    return this;
  }

  /**
   * Get HTTP path.
   * 
   * @return {@link String}
   */
  public String path() {
    return this.path;
  }

  /**
   * Set HTTP path.
   * 
   * @param httpPath {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest path(final String httpPath) {
    this.path = httpPath;
    return this;
  }

  /**
   * Get Path Parameters.
   * 
   * @return {@link Map}
   */
  public Map<String, String> pathParameters() {
    return this.pathParameters;
  }

  /**
   * Set Path Parameters.
   * 
   * @param parameters {@link Map}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest pathParameters(final Map<String, String> parameters) {
    this.pathParameters = parameters;
    return this;
  }

  /**
   * Get Query Parameters.
   * 
   * @return {@link Map}
   */
  public Map<String, String> queryParameters() {
    return this.queryParameters;
  }

  /**
   * Set Query Parameters.
   * 
   * @param parameters {@link Map}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest queryParameters(final Map<String, String> parameters) {
    this.queryParameters = parameters;
    return this;
  }

  /**
   * Get HTTP resource.
   * 
   * @return {@link String}
   */
  public String resource() {
    return this.resource;
  }

  /**
   * Set HTTP resource.
   * 
   * @param httpResource {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest resource(final String httpResource) {
    this.resource = httpResource;
    return this;
  }

  /**
   * Get Http User.
   * 
   * @return {@link String}
   */
  public String user() {
    return this.user;
  }

  /**
   * Set Http User.
   * 
   * @param httpUser {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest user(final String httpUser) {
    this.user = httpUser;
    return this;
  }
}
