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
package com.formkiq.aws.eventbridge;

/**
 * Event Bridge Message.
 */
public class EventBridgeMessage {
  /** Event Bridge Detail Type. */
  private final String detailType;
  /** Event Bridge Detail. */
  private final String detail;
  /** Event Bridge Source. */
  private final String source;

  /**
   * constructor.
   *
   * @param eventBridgeDetailType {@link String}
   * @param eventBridgeDetail {@link String}
   * @param eventBridgeSource {@link String}
   */
  public EventBridgeMessage(final String eventBridgeDetailType, final String eventBridgeDetail,
      final String eventBridgeSource) {
    this.detailType = eventBridgeDetailType;
    this.detail = eventBridgeDetail;
    this.source = eventBridgeSource;
  }

  /**
   * Get Detail Type.
   * 
   * @return String
   */
  public String getDetailType() {
    return this.detailType;
  }

  /**
   * Get Detail.
   * 
   * @return String
   */
  public String getDetail() {
    return this.detail;
  }

  /**
   * Get Source.
   * 
   * @return String
   */
  public String getSource() {
    return this.source;
  }
}
