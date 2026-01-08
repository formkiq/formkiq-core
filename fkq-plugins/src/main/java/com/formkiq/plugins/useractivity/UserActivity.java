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
import com.formkiq.aws.dynamodb.useractivities.UserActivityStatus;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Represents an activity performed by a user. This includes metadata such as who performed the
 * action, when it occurred.
 */
public record UserActivity(
    /* Site Identifier. */
    String siteId,
    /* Resource of User Activity. */
    String resource,
    /* The type of activity performed (e.g., "UPLOAD", "DELETE"). */
    UserActivityType type,

    /* The identifier of the user who performed the activity. */
    String userId,

    /* The timestamp when the activity was recorded. */
    Date insertedDate,

    /* A message or description related to the activity. */
    String message,

    /* The status of the activity (e.g., "SUCCESS", "FAILED"). */
    UserActivityStatus status,

    /* The source IP address from which the activity originated. */
    String sourceIpAddress,

    /* The source system of the activity (e.g., HTTP, SQS). */
    String source,

    /* Resource Ids. */
    Map<String, Object> resourceIds,
    /* Request Body. */
    String body,
    /* Change Set. */
    Map<String, ChangeRecord> changes,
    /* Properties. */
    Map<String, Object> properties) {

  /**
   * Creates a new {@link UserActivity.Builder} for {@link UserActivity}.
   *
   * @return a Builder instance
   */
  public static UserActivity.Builder builder() {
    return new UserActivity.Builder();
  }

  /**
   * Builder class for creating instances of {@link UserActivity}.
   */
  public static class Builder {
    /** Activity Type. */
    private UserActivityType type;
    /** User Id. */
    private String userId;
    /** Inserted Date. */
    private Instant insertedDate;
    /** Activity Message. */
    private String message;
    /** {@link UserActivityStatus}. */
    private UserActivityStatus status;
    /** Source IP Address. */
    private String sourceIpAddress;
    /** Source. */
    private String source;
    /** Activity Resource. */
    private String resource;
    /** Request Body. */
    private String body;
    /** Change Set. */
    private Map<String, ChangeRecord> changes;
    /** Resource Ids. */
    private Map<String, Object> resourceIds;
    /** Properties. */
    private Map<String, Object> properties;

    public Builder body(final String userActivityBody) {
      this.body = userActivityBody;
      return this;
    }

    /**
     * Builds the {@link UserActivity} instance.
     * 
     * @param siteId {@link String}
     *
     * @return a new {@link UserActivity} object
     */
    public UserActivity build(final String siteId) {
      return new UserActivity(siteId, resource, type, userId, Date.from(insertedDate), message,
          status, sourceIpAddress, source, resourceIds, body, changes, properties);
    }

    public Builder changes(final Map<String, ChangeRecord> userActivityChanges) {
      this.changes = userActivityChanges;
      return this;
    }

    public Builder insertedDate(final Instant userActivityInsertedDate) {
      this.insertedDate = userActivityInsertedDate;
      return this;
    }

    public Builder message(final String userActivityMessage) {
      this.message = userActivityMessage;
      return this;
    }

    public Builder properties(final Map<String, Object> userActivityProperties) {
      properties = userActivityProperties;
      return this;
    }

    /**
     * Sets the resourceof the activity.
     *
     * @param activityResource the object activityResource
     * @return the builder instance
     */
    public Builder resource(final String activityResource) {
      this.resource = activityResource;
      return this;
    }

    public Builder resourceIds(final Map<String, Object> userActivityResourceIds) {
      this.resourceIds = userActivityResourceIds;
      return this;
    }

    /**
     * Sets the activitySource system of the activity.
     *
     * @param activitySource the activitySource system (e.g., HTTP, SQS)
     * @return the builder instance
     */
    public Builder source(final String activitySource) {
      this.source = activitySource;
      return this;
    }

    public Builder sourceIpAddress(final String userActivitySourceIpAddress) {
      this.sourceIpAddress = userActivitySourceIpAddress;
      return this;
    }

    public Builder status(final UserActivityStatus userActivityStatus) {
      this.status = userActivityStatus;
      return this;
    }

    public Builder status(final int userActivityStatusCode) {
      final int error = 500;
      status(userActivityStatusCode != error ? UserActivityStatus.COMPLETE
          : UserActivityStatus.FAILED);
      return this;
    }

    public Builder type(final UserActivityType userActivityType) {
      this.type = userActivityType;
      return this;
    }

    public Builder userId(final String userActivityUserId) {
      this.userId = userActivityUserId;
      return this;
    }
  }
}
