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

import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;

import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;

/**
 * Thread-local container for holding user activity metadata as a DynamoDB map of AttributeValues.
 *
 * This class allows decoupling of service logic from where the user activity metadata is set,
 * enabling cross-cutting concerns like logging or auditing to access context safely.
 */
public class UserActivityContext {

  /** Thread Local Variable. */
  private static final ThreadLocal<Collection<UserActivityContextData>> CONTEXT =
      ThreadLocal.withInitial(ArrayList::new);

  private UserActivityContext() {
    // Prevent instantiation
  }

  /**
   * Set the user activity context for the current thread.
   *
   * @param resourceType {@link ActivityResourceType}
   * @param type {@link UserActivityType}
   * @param data the DynamoDB attribute map
   * @param properties Extra property information
   */
  public static void set(final ActivityResourceType resourceType, final UserActivityType type,
      final Map<String, ChangeRecord> data, final Map<String, Object> properties) {

    if (!UserActivityType.UPDATE.equals(type)) {
      CONTEXT.get().add(new UserActivityContextData(resourceType, type, data, properties));
    } else {
      data.remove("insertedDate");
      data.remove("lastModifiedDate");
      if (!data.isEmpty()) {
        CONTEXT.get().add(new UserActivityContextData(resourceType, type, data, properties));
      }
    }
  }

  /**
   * Get the user activity context for the current thread.
   *
   * @return UserActivityContextData, or null if not set
   */
  public static Collection<UserActivityContextData> get() {
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
