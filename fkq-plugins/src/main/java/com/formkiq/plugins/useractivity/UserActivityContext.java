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
package com.formkiq.plugins.useractivity;

import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;

import java.util.Map;

/**
 * Thread-local container for holding user activity metadata as a DynamoDB map of AttributeValues.
 *
 * This class allows decoupling of service logic from where the user activity metadata is set,
 * enabling cross-cutting concerns like logging or auditing to access context safely.
 */
public class UserActivityContext {

  /** Thread Local Variable. */
  private static final ThreadLocal<UserActivityContextData> CONTEXT = new ThreadLocal<>();

  private UserActivityContext() {
    // Prevent instantiation
  }

  /**
   * Set the user activity context for the current thread.
   *
   * @param type {@link UserActivityType}
   * @param data the DynamoDB attribute map
   */
  public static void set(final UserActivityType type, final Map<String, ChangeRecord> data) {

    if (!UserActivityType.UPDATE.equals(type)) {
      CONTEXT.set(new UserActivityContextData(type, data));
    } else {
      data.remove("insertedDate");
      data.remove("lastModifiedDate");
      if (!data.isEmpty()) {
        CONTEXT.set(new UserActivityContextData(type, data));
      }
    }
  }

  /**
   * Get the user activity context for the current thread.
   *
   * @return UserActivityContextData, or null if not set
   */
  public static UserActivityContextData get() {
    return CONTEXT.get();
  }

  /**
   * Clear the user activity context for the current thread. Call this at the end of request
   * processing to prevent leaks.
   */
  public static void clear() {
    CONTEXT.remove();
  }
}
