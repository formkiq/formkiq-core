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
package com.formkiq.module.eventsourcing.commands;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP request mapped to a domain {@link Command}, enriched with routing and audit
 * metadata required by the event-sourcing pipeline.
 */
public class HttpCommand implements Command {

  /** Type of entity or aggregate this command applies to (e.g., "Document"). */
  private final String entityType;

  /** Identifier of the target entity (e.g., documentId). */
  private final String entityId;

  /** User or system that initiated the command. */
  private final String user;

  /** Timestamp when the command was created. */
  private final Instant timestamp;

  /** HTTP method of the request (e.g., GET, POST, PUT, PATCH). */
  private final String method;

  /** Request URI or path (excluding query string). */
  private final String path;

  /** HTTP headers: header name to list of values (to handle repeating headers). */
  private final Map<String, List<String>> headers;

  /** HTTP query parameters: parameter name to list of values. */
  private final Map<String, List<String>> queryParams;

  /** The raw request body (JSON, XML, etc.), or null if none. */
  private final String body;

  private HttpCommand(final Builder builder) {
    this.entityType = builder.entityType;
    this.entityId = builder.entityId;
    this.user = builder.user;
    this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    this.method = builder.method;
    this.path = builder.path;
    this.headers = builder.headers;
    this.queryParams = builder.queryParams;
    this.body = builder.body;
  }

  @Override
  public String getEventType() {
    return "HTTP_" + method.toUpperCase();
  }

  @Override
  public String getEntityType() {
    return entityType;
  }

  @Override
  public String getEntityId() {
    return entityId;
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * HTTP method.
   * 
   * @return HTTP method (e.g., "GET")
   */
  public String getMethod() {
    return method;
  }

  /**
   * Request Path.
   * 
   * @return request path (e.g., "/documents/123")
   */
  public String getPath() {
    return path;
  }

  /**
   * Http Headers.
   * 
   * @return HTTP headers map
   */
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  /**
   * HTTP Query Parameters.
   * 
   * @return HTTP query parameters map
   */
  public Map<String, List<String>> getQueryParams() {
    return queryParams;
  }

  /**
   * Http Body.
   * 
   * @return raw request body, or null if none
   */
  public String getBody() {
    return body;
  }

  @Override
  public String toString() {
    return "HttpCommand{" + "entityType='" + entityType + '\'' + ", entityId='" + entityId + '\''
        + ", eventType='" + getEventType() + '\'' + ", user='" + user + '\'' + ", timestamp="
        + timestamp + ", method='" + method + '\'' + ", path='" + path + '\'' + ", headers="
        + headers + ", queryParams=" + queryParams + ", body="
        + (body != null ? "[REDACTED]" : null) + '}';
  }

  /**
   * Creates a new {@link Builder} for {@link HttpCommand}.
   *
   * @return a Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HttpCommand} to facilitate fluent construction.
   */
  public static class Builder {
    /** Entity Type. */
    private String entityType;
    /** Entity Id. */
    private String entityId;
    /** User. */
    private String user;
    /** Timestamp. */
    private Instant timestamp;
    /** Http Method. */
    private String method;
    /** Http Path. */
    private String path;
    /** Http Headers. */
    private Map<String, List<String>> headers = Map.of();
    /** Http Query Params. */
    private Map<String, List<String>> queryParams = Map.of();
    /** Http Body. */
    private String body;

    /**
     * Sets the entity type this command targets (e.g., "Document").
     *
     * @param commandEntityType the aggregate type
     * @return this Builder instance
     */
    public Builder entityType(final String commandEntityType) {
      this.entityType = commandEntityType;
      return this;
    }

    /**
     * Sets the identifier of the entity this command targets.
     *
     * @param commandEntityId the aggregate/entity ID
     * @return this Builder instance
     */
    public Builder entityId(final String commandEntityId) {
      this.entityId = commandEntityId;
      return this;
    }

    /**
     * Sets the commandUser or system that initiated the command.
     *
     * @param commandUser the commandUser ID or system name
     * @return this Builder instance
     */
    public Builder user(final String commandUser) {
      this.user = commandUser;
      return this;
    }

    /**
     * Sets the commandTimestamp when the command was created.
     *
     * @param commandTimestamp the command creation time
     * @return this Builder instance
     */
    public Builder timestamp(final Instant commandTimestamp) {
      this.timestamp = commandTimestamp;
      return this;
    }

    /**
     * Sets the HTTP commandMethod (e.g., "GET").
     *
     * @param commandMethod the HTTP commandMethod
     * @return this Builder instance
     */
    public Builder method(final String commandMethod) {
      this.method = commandMethod;
      return this;
    }

    /**
     * Sets the request commandPath (e.g., "/api/documents").
     *
     * @param commandPath the request URI or commandPath
     * @return this Builder instance
     */
    public Builder path(final String commandPath) {
      this.path = commandPath;
      return this;
    }

    /**
     * Sets the HTTP commandHeaders map.
     *
     * @param commandHeaders a map of header names to values
     * @return this Builder instance
     */
    public Builder headers(final Map<String, List<String>> commandHeaders) {
      this.headers = commandHeaders;
      return this;
    }

    /**
     * Sets the HTTP query parameters map.
     *
     * @param commandQueryParams a map of parameter names to values
     * @return this Builder instance
     */
    public Builder queryParams(final Map<String, List<String>> commandQueryParams) {
      this.queryParams = commandQueryParams;
      return this;
    }

    /**
     * Sets the raw request commandBody.
     *
     * @param commandBody the request commandBody payload
     * @return this Builder instance
     */
    public Builder body(final String commandBody) {
      this.body = commandBody;
      return this;
    }

    /**
     * Builds the {@link HttpCommand} instance.
     *
     * @return a newly constructed HttpCommand
     */
    public HttpCommand build() {
      return new HttpCommand(this);
    }
  }
}
