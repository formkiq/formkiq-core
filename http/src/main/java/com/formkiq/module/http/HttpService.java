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

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * 
 * HttpService.
 *
 */
public interface HttpService {

  /**
   * Send DELETE request.
   * 
   * @param url {@link String}
   * @param headers {@link HttpHeaders}
   * @param parameters {@link Optional} {@link Map}
   * @return {@link HttpResponse} {@link String}
   * @throws IOException IOException
   */
  HttpResponse<String> delete(String url, Optional<HttpHeaders> headers,
      Optional<Map<String, String>> parameters) throws IOException;

  /**
   * GET HTTP Request and return a {@link HttpResponse}.
   * 
   * @param url {@link String}
   * @param headers {@link HttpHeaders}
   * @param parameters {@link Optional} {@link Map}
   * @return {@link HttpResponse} {@link String}
   * @throws IOException IOException
   */
  HttpResponse<String> get(String url, Optional<HttpHeaders> headers,
      Optional<Map<String, String>> parameters) throws IOException;

  /**
   * Patch HTTP Request and return a {@link HttpResponse}.
   * 
   * @param url {@link String}
   * @param headers {@link HttpHeaders}
   * @param parameters {@link Optional} {@link Map}
   * @param payload {@link String}
   * @return {@link HttpResponse} {@link String}
   * @throws IOException IOException
   */
  HttpResponse<String> patch(String url, Optional<HttpHeaders> headers,
      Optional<Map<String, String>> parameters, String payload) throws IOException;

  /**
   * Post HTTP Request and return a {@link HttpResponse}.
   * 
   * @param url {@link String}
   * @param headers {@link HttpHeaders}
   * @param parameters {@link Optional} {@link Map}
   * @param payload {@link String}
   * @return {@link HttpResponse} {@link String}
   * @throws IOException IOException
   */
  HttpResponse<String> post(String url, Optional<HttpHeaders> headers,
      Optional<Map<String, String>> parameters, String payload) throws IOException;

  /**
   * Put HTTP Request and return a {@link HttpResponse}.
   * 
   * @param url {@link String}
   * @param headers {@link HttpHeaders}
   * @param parameters {@link Optional} {@link Map}
   * @param payload {@link Path}
   * @return {@link HttpResponse} {@link Path}
   * @throws IOException IOException
   */
  HttpResponse<String> put(String url, Optional<HttpHeaders> headers,
      Optional<Map<String, String>> parameters, Path payload) throws IOException;

  /**
   * Put HTTP Request and return a {@link HttpResponse}.
   * 
   * @param url {@link String}
   * @param headers {@link HttpHeaders}
   * @param parameters {@link Optional} {@link Map}
   * @param payload {@link Path}
   * @return {@link HttpResponse} {@link String}
   * @throws IOException IOException
   */
  HttpResponse<String> put(String url, Optional<HttpHeaders> headers,
      Optional<Map<String, String>> parameters, String payload) throws IOException;
}
