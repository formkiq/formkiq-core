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
package com.formkiq.module.http;

import java.net.http.HttpResponse;

/**
 * 
 * Http Response Status.
 *
 */
public class HttpResponseStatus {

  /** 200 Response Code. */
  private static final int STATUS_200 = 200;
  /** 300 Response Code. */
  private static final int STATUS_300 = 300;
  /** 403 Forbidden Response Code. */
  public static final int STATUS_FORBIDDEN = 403;
  /** 404 Response Code. */
  private static final int STATUS_404 = 404;
  /** 409 Response Code. */
  private static final int STATUS_409 = 409;
  /** 429 Response Code. */
  private static final int STATUS_429 = 404;

  /**
   * Is 2XX Status Code.
   * 
   * @param response {@link HttpResponse}
   * @return boolean
   */
  public static boolean is2XX(final HttpResponse<?> response) {
    int statusCode = response.statusCode();
    return statusCode >= STATUS_200 && statusCode < STATUS_300;
  }

  /**
   * Is 404 Status Code.
   * 
   * @param response {@link HttpResponse}
   * @return boolean
   */
  public static boolean is404(final HttpResponse<?> response) {
    int statusCode = response.statusCode();
    return statusCode == STATUS_404;
  }

  /**
   * Is 409 Status Code.
   * 
   * @param response {@link HttpResponse}
   * @return boolean
   */
  public static boolean is409(final HttpResponse<?> response) {
    int statusCode = response.statusCode();
    return statusCode == STATUS_409;
  }

  /**
   * Is 429 Status Code.
   * 
   * @param response {@link HttpResponse}
   * @return boolean
   */
  public static boolean is429(final HttpResponse<?> response) {
    int statusCode = response.statusCode();
    return statusCode == STATUS_429;
  }
}
