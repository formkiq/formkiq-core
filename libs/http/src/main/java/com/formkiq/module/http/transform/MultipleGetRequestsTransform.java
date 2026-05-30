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
package com.formkiq.module.http.transform;

import com.formkiq.module.http.HttpResponseStatus;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link Function} to fetch the content for a {@link Collection} of Urls to {@link String}.
 */
public class MultipleGetRequestsTransform implements Function<Collection<String>, String> {

  /** {@link HttpService}. */
  private final HttpService http = new HttpServiceJdk11();

  @Override
  public String apply(final Collection<String> urls) {

    StringBuilder content = new StringBuilder();

    for (String url : urls) {
      try {
        HttpResponse<String> response = http.get(url, Optional.empty(), Optional.empty());
        if (HttpResponseStatus.is2XX(response)) {
          content.append(response.body());
        } else {
          throw new TransformException("Status code " + response.statusCode() + " for " + url);
        }

      } catch (IOException e) {
        throw new TransformException(e);
      }
    }

    return content.toString().trim();
  }
}
