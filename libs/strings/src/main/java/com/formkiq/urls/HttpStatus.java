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
package com.formkiq.urls;

/**
 * HTTP Status Code Constants with JavaDoc explanations.
 * <p>
 * Usage: import static example.http.HttpStatus.*;
 */
public interface HttpStatus {

  // --- 1xx Informational ---

  /** Client may continue sending request body. */
  int CONTINUE = 100;

  /** Server is switching to a different protocol as requested. */
  int SWITCHING_PROTOCOLS = 101;

  /** Server has received request and is still processing (WebDAV). */
  int PROCESSING = 102;

  /** Used to send hints to the client before final response (e.g., preload). */
  int EARLY_HINTS = 103;


  // --- 2xx Success ---

  /** Standard success response. */
  int OK = 200;

  /** Request succeeded and resulted in a new resource being created. */
  int CREATED = 201;

  /** Request accepted for processing, but processing not complete. */
  int ACCEPTED = 202;

  /** Response received from a transforming proxy, not the origin server. */
  int NON_AUTHORITATIVE_INFORMATION = 203;

  /** Success but no response body. */
  int NO_CONTENT = 204;

  /** Client should reset document/view. */
  int RESET_CONTENT = 205;

  /** Partial content returned (typically for Range requests). */
  int PARTIAL_CONTENT = 206;

  /** WebDAV: Multiple status values returned. */
  int MULTI_STATUS = 207;

  /** WebDAV: Already reported in preceding response. */
  int ALREADY_REPORTED = 208;

  /** HTTP Delta encoding was applied. */
  int IM_USED = 226;


  // --- 3xx Redirection ---

  /** Multiple possible redirect choices. */
  int MULTIPLE_CHOICES = 300;

  /** Resource moved permanently (clients may cache). */
  int MOVED_PERMANENTLY = 301;

  /** Temporary redirect (commonly used with browsers). */
  int FOUND = 302;

  /** Redirect, but client should issue GET on the new resource. */
  int SEE_OTHER = 303;

  /** Resource has not changed since last request (cache validation). */
  int NOT_MODIFIED = 304;

  /** Resource must be accessed via proxy (deprecated). */
  int USE_PROXY = 305;

  /** Temporary redirect that preserves HTTP method. */
  int TEMPORARY_REDIRECT = 307;

  /** Permanent redirect that preserves HTTP method. */
  int PERMANENT_REDIRECT = 308;


  // --- 4xx Client Errors ---

  /** Invalid request syntax or missing required parameters. */
  int BAD_REQUEST = 400;

  /** Authentication required (not authorization failure). */
  int UNAUTHORIZED = 401;

  /** Reserved for future use. */
  int PAYMENT_REQUIRED = 402;

  /** Authenticated but not allowed to access resource. */
  int FORBIDDEN = 403;

  /** Resource not found. */
  int NOT_FOUND = 404;

  /** HTTP method not allowed for the target resource. */
  int METHOD_NOT_ALLOWED = 405;

  /** Cannot provide acceptable representation based on Accept headers. */
  int NOT_ACCEPTABLE = 406;

  /** Must authenticate with a proxy. */
  int PROXY_AUTHENTICATION_REQUIRED = 407;

  /** Server timed out waiting for request. */
  int REQUEST_TIMEOUT = 408;

  /** Conflict occurred (e.g., edit conflict). */
  int CONFLICT = 409;

  /** Resource is permanently gone. */
  int GONE = 410;

  /** Content-Length header is required. */
  int LENGTH_REQUIRED = 411;

  /** Preconditions specified are not met. */
  int PRECONDITION_FAILED = 412;

  /** Request payload is too large. */
  int PAYLOAD_TOO_LARGE = 413;

  /** URI is too long. */
  int URI_TOO_LONG = 414;

  /** Unsupported media/content type. */
  int UNSUPPORTED_MEDIA_TYPE = 415;

  /** Requested Range cannot be satisfied. */
  int RANGE_NOT_SATISFIABLE = 416;

  /** Expect header requirements not met. */
  int EXPECTATION_FAILED = 417;

  /** I'm a teapot ☕ (RFC 2324 — intentionally kept). */
  int IM_A_TEAPOT = 418;

  /** Request was directed at a server unable to produce a response. */
  int MISDIRECTED_REQUEST = 421;

  /** Request is semantically invalid. */
  int UNPROCESSABLE_ENTITY = 422;

  /** WebDAV: Resource is locked. */
  int LOCKED = 423;

  /** WebDAV: Request failed due to a failed dependency. */
  int FAILED_DEPENDENCY = 424;

  /** Request was made too early to be processed safely. */
  int TOO_EARLY = 425;

  /** Client must upgrade protocol (e.g., HTTP → HTTPS/TLS). */
  int UPGRADE_REQUIRED = 426;

  /** Conditional request required to prevent lost updates. */
  int PRECONDITION_REQUIRED = 428;

  /** Rate limit exceeded. */
  int TOO_MANY_REQUESTS = 429;

  /** Request header fields are too large. */
  int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

  /** Access to resource blocked for legal reasons (e.g., DMCA). */
  int UNAVAILABLE_FOR_LEGAL_REASONS = 451;


  // --- 5xx Server Errors ---

  /** Generic server error. */
  int INTERNAL_SERVER_ERROR = 500;

  /** Functionality not implemented. */
  int NOT_IMPLEMENTED = 501;

  /** Bad response received from upstream server. */
  int BAD_GATEWAY = 502;

  /** Server overloaded or down for maintenance. */
  int SERVICE_UNAVAILABLE = 503;

  /** Upstream server timeout. */
  int GATEWAY_TIMEOUT = 504;

  /** HTTP version not supported. */
  int HTTP_VERSION_NOT_SUPPORTED = 505;

  /** Negotiation failure. */
  int VARIANT_ALSO_NEGOTIATES = 506;

  /** Insufficient storage to complete request (WebDAV). */
  int INSUFFICIENT_STORAGE = 507;

  /** Infinite loop detected during processing (WebDAV). */
  int LOOP_DETECTED = 508;

  /** Further extensions required. */
  int NOT_EXTENDED = 510;

  /** Client must authenticate to access network. */
  int NETWORK_AUTHENTICATION_REQUIRED = 511;
}
