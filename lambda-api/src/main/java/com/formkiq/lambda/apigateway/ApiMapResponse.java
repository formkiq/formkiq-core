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
package com.formkiq.lambda.apigateway;

import java.util.Map;
import com.formkiq.graalvm.annotations.Reflectable;

/** API Response Object. */
@Reflectable
public class ApiMapResponse implements ApiResponse {

  /** Document Id. */
  @Reflectable
  private Map<String, Object> map;

  /** constructor. */
  public ApiMapResponse() {}

  /**
   * constructor.
   * 
   * @param m {@link Map}
   */
  public ApiMapResponse(final Map<String, Object> m) {
    this.map = m;
  }

  @Override
  public String getNext() {
    return (String) this.map.get("next");
  }

  @Override
  public String getPrevious() {
    return (String) this.map.get("previous");
  }

  @Override
  public String toString() {
    return this.map.toString();
  }

  /**
   * Get {@link Map}.
   * 
   * @return {@link Map}
   */
  public Map<String, Object> getMap() {
    return this.map;
  }

  /**
   * Set {@link Map}.
   * 
   * @param m {@link Map}
   */
  public void setMap(final Map<String, Object> m) {
    this.map = m;
  }
}
