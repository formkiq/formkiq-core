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
package com.formkiq.module.events.notification;

import static com.formkiq.module.events.document.DocumentEventType.TEST_NOTIFICATION;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Event requesting delivery of a test notification using a site's saved configuration.
 *
 * @param type event type
 * @param siteId site identifier
 * @param to recipient email address
 * @param userId requesting user
 */
@Reflectable
public record NotificationTestEvent(String type, String siteId, String to, String userId) {

  /**
   * Creates a test-notification event.
   *
   * @param siteId site identifier
   * @param to recipient email address
   * @param userId requesting user
   */
  public NotificationTestEvent(final String siteIdParam, final String toParam,
      final String userIdParam) {
    this(TEST_NOTIFICATION, siteIdParam, toParam, userIdParam);
  }
}
