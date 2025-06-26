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
 * Represents an activity performed by a user on a document. This includes metadata such as who
 * performed the action, when it occurred, and where it is stored (S3).
 */
public record UserActivity(
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
    String sourceIpAddress) {

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
    /** The unique identifier of the document. */
    private String documentId;
    /** The type of activity performed (e.g., "UPLOAD", "DELETE"). */
    private String type;
    /** The identifier of the user who performed the activity. */
    private String userId;
    /** The timestamp when the activity was recorded. */
    private Instant insertedDate;
    /** The S3 object key for the stored document or metadata. */
    private String s3Key;
    /** A message or description related to the activity. */
    private String message;
    /** The status of the activity (e.g., "SUCCESS", "FAILED"). */
    private UserActivityStatus status;
    /** The source IP address from which the activity originated. */
    private String sourceIpAddress;

    /**
     * Sets the document ID.
     *
     * @param userActivityDocumentId the document identifier
     * @return the builder instance
     */
    public Builder documentId(final String userActivityDocumentId) {
      this.documentId = userActivityDocumentId;
      return this;
    }

    /**
     * Sets the activity userActivityType.
     *
     * @param userActivityType the activity userActivityType
     * @return the builder instance
     */
    public Builder type(final String userActivityType) {
      this.type = userActivityType;
      return this;
    }

    /**
     * Sets the user ID.
     *
     * @param userActivityUserId the user identifier
     * @return the builder instance
     */
    public Builder userId(final String userActivityUserId) {
      this.userId = userActivityUserId;
      return this;
    }

    /**
     * Sets the inserted timestamp.
     *
     * @param userActivityInsertedDate the date and time of insertion
     * @return the builder instance
     */
    public Builder insertedDate(final Instant userActivityInsertedDate) {
      this.insertedDate = userActivityInsertedDate;
      return this;
    }

    /**
     * Sets the S3 key.
     *
     * @param userActivityS3Key the S3 object key
     * @return the builder instance
     */
    public Builder s3Key(final String userActivityS3Key) {
      this.s3Key = userActivityS3Key;
      return this;
    }

    /**
     * Sets the userActivityMessage.
     *
     * @param userActivityMessage the userActivityMessage or description
     * @return the builder instance
     */
    public Builder message(final String userActivityMessage) {
      this.message = userActivityMessage;
      return this;
    }

    /**
     * Sets the userActivityStatus of the activity.
     *
     * @param userActivityStatus the activity userActivityStatus
     * @return the builder instance
     */
    public Builder status(final UserActivityStatus userActivityStatus) {
      this.status = userActivityStatus;
      return this;
    }

    /**
     * Sets the userActivityStatus of the activity.
     *
     * @param userActivityStatus the activity userActivityStatus
     * @return the builder instance
     */
    public Builder status(final int userActivityStatus) {
      final int error = 500;
      status(userActivityStatus != error ? UserActivityStatus.SUCCESS : UserActivityStatus.FAILED);
      return this;
    }

    /**
     * Sets the source IP address.
     *
     * @param userActivitySourceIpAddress the originating IP address
     * @return the builder instance
     */
    public Builder sourceIpAddress(final String userActivitySourceIpAddress) {
      this.sourceIpAddress = userActivitySourceIpAddress;
      return this;
    }

    /**
     * Builds the {@link UserActivity} instance.
     *
     * @return a new {@link UserActivity} object
     */
    public UserActivity build() {
      return new UserActivity(documentId, type, userId, insertedDate, s3Key, message, status,
          sourceIpAddress);
    }
  }
}
