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
package com.formkiq.stacks.lambda.s3.actions;

import com.formkiq.aws.eventbridge.EventBridgeMessage;

/**
 * Event Bridge Message Builder.
 */
public class EventBridgeMessageBuilder {

  /**
   * Build {@link EventBridgeMessage}.
   * 
   * @param appEnvironment {@link String}
   * @param detailType {@link String}
   * @param detail {@link String}
   * @return EventBridgeMessage
   */
  public EventBridgeMessage build(final String appEnvironment, final String detailType,
      final String detail) {
    return new EventBridgeMessage(detailType, detail, getSource(appEnvironment));
  }

  private String getSource(final String appEnvironment) {
    return "formkiq." + appEnvironment;
  }
}
