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

import java.time.Instant;

/**
 * Represents an activity performed by a user on a document or entity. This includes metadata such
 * as who performed the action, when it occurred, where it is stored (S3), and the target entity
 * identifiers.
 */
public record UserActivity(
    /* Resource of User Activity. */
    String resource,
    /* The unique identifier of the document. */
    String documentId,

    /* The type of activity performed (e.g., "UPLOAD", "DELETE"). */
    String type,

    /* The identifier of the user who performed the activity. */
    String userId,

    /* The timestamp when the activity was recorded. */
    Instant insertedDate,

    /* The S3 object key for the stored document or metadata. */
    String s3Key,

    /* A message or description related to the activity. */
    String message,

    /* The status of the activity (e.g., "SUCCESS", "FAILED"). */
    UserActivityStatus status,

    /* The source IP address from which the activity originated. */
    String sourceIpAddress,

    /* The source system of the activity (e.g., HTTP, SQS). */
    String source,

    /* The type identifier of the target entity. */
    String entityTypeId,

    /* The identifier of the target entity. */
    String entityId,

    /* The namespace of the target entity. */
    String entityNamespace) {

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
    /** Document Id. */
    private String documentId;
    /** Activity Type. */
    private String type;
    /** User Id. */
    private String userId;
    /** Inserted Date. */
    private Instant insertedDate;
    /** S3 Key. */
    private String s3Key;
    /** Activity Message. */
    private String message;
    /** {@link UserActivityStatus}. */
    private UserActivityStatus status;
    /** Source IP Address. */
    private String sourceIpAddress;
    /** Source. */
    private String source;
    /** Entity Type Id. */
    private String entityTypeId;
    /** Entity Id. */
    private String entityId;
    /** Entity Namespace. */
    private String entityNamespace;
    /** Activity Resource. */
    private String resource;

    public Builder documentId(final String userActivityDocumentId) {
      this.documentId = userActivityDocumentId;
      return this;
    }

    public Builder type(final String userActivityType) {
      this.type = userActivityType;
      return this;
    }

    public Builder userId(final String userActivityUserId) {
      this.userId = userActivityUserId;
      return this;
    }

    public Builder insertedDate(final Instant userActivityInsertedDate) {
      this.insertedDate = userActivityInsertedDate;
      return this;
    }

    public Builder s3Key(final String userActivityS3Key) {
      this.s3Key = userActivityS3Key;
      return this;
    }

    public Builder message(final String userActivityMessage) {
      this.message = userActivityMessage;
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

    public Builder sourceIpAddress(final String userActivitySourceIpAddress) {
      this.sourceIpAddress = userActivitySourceIpAddress;
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

    /**
     * Sets the entity type identifier.
     *
     * @param activityEntityTypeId the entity type identifier
     * @return the builder instance
     */
    public Builder entityTypeId(final String activityEntityTypeId) {
      this.entityTypeId = activityEntityTypeId;
      return this;
    }

    /**
     * Sets the entity identifier.
     *
     * @param activityEntityId the entity identifier
     * @return the builder instance
     */
    public Builder entityId(final String activityEntityId) {
      this.entityId = activityEntityId;
      return this;
    }

    /**
     * Sets the entity namespace.
     *
     * @param activityEntityNamespace the entity namespace
     * @return the builder instance
     */
    public Builder entityNamespace(final String activityEntityNamespace) {
      this.entityNamespace = activityEntityNamespace;
      return this;
    }

    /**
     * Builds the {@link UserActivity} instance.
     *
     * @return a new {@link UserActivity} object
     */
    public UserActivity build() {
      return new UserActivity(resource, documentId, type, userId, insertedDate, s3Key, message,
          status, sourceIpAddress, source, entityTypeId, entityId, entityNamespace);
    }
  }
}
