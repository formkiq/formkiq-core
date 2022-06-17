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

/** Http Status Codes. */
public enum ApiResponseStatus {

  /** {@code 200 OK} (HTTP/1.0 - RFC 1945). */
  SC_OK(200),
  /** {@code 201 CREATED} (HTTP/1.0 - RFC 1945). */
  SC_CREATED(201),
  /** {@code 202 ACCEPTED} (HTTP/1.0 - RFC 1945). */
  SC_ACCEPTED(202),
  /** Found (Previously "Moved Permanently"). */
  MOVED_PERMANENTLY(301),
  /** Found (Previously "Moved temporarily"). */
  SC_FOUND(302),
  /** {@code 303 SEE Other}. */
  SC_SEE_OTHER(303),
  /** Temporary Redirect. */
  SC_TEMPORARY_REDIRECT(307),
  /** {@code 400 bad request} (HTTP/1.0 - RFC 1945). */
  SC_BAD_REQUEST(400),
  /** {@code 401 bad request} (HTTP/1.0 - RFC 1945). */
  SC_UNAUTHORIZED(401),
  /** {@code 402 forbidden} (HTTP/1.0 - RFC 1945). */
  SC_PAYMENT(402),
  /** {@code 403 forbidden} (HTTP/1.0 - RFC 1945). */
  SC_FORBIDDEN(403),
  /** {@code 404 notfound} (HTTP/1.0 - RFC 1945). */
  SC_NOT_FOUND(404),
  /** {@code 429 Too Many Requests} (HTTP/1.0 - RFC 1945). */
  SC_TOO_MANY_REQUESTS(429),
  /** {@code 500 Server Error} (HTTP/1.0 - RFC 1945). */
  SC_ERROR(500),
  /** {@code 501 Server Error} (HTTP/1.0 - RFC 1945). */
  SC_NOT_IMPLEMENTED(501);

  /** Http Status Code. */
  private final int statusCode;

  /**
   * constructor.
   *
   * @param code int
   */
  ApiResponseStatus(final int code) {
    this.statusCode = code;
  }

  /**
   * Get Http Status.
   *
   * @return int
   */
  public int getStatusCode() {
    return this.statusCode;
  }
}
