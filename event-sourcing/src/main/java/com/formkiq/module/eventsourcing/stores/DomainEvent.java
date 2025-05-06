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
package com.formkiq.module.eventsourcing.stores;

import java.time.Instant;
import java.util.Map;

/**
 * Event Sourcing Domain Event as a Java record, representing an immutable event in the system.
 */
public record DomainEvent(
    /* The type of entity this event pertains to (e.g., "Document"). */
    String entityType,
    /* The identifier of the aggregate/entity (e.g., documentId). */
    String entityId,
    /* The timestamp when the event was created, with nanosecond precision. */
    Instant timestamp,
    /* The domain-specific event type (e.g., "DocumentCreated"). */
    String type,
    /* Additional metadata (e.g., correlationId, causationId). */
    Map<String, Object> metadata,
    /* The payload containing event details (e.g., fields that changed). */
    Map<String, Object> payload,
    /* The user or system that triggered the event. */
    String user) {

  /**
   * Canonical constructor to enforce defensive copying of maps.
   *
   * @param entityType the type of entity this event pertains to
   * @param entityId the identifier of the aggregate/entity
   * @param timestamp the timestamp of event creation
   * @param type the domain-specific event type
   * @param metadata additional event metadata
   * @param payload the event payload
   * @param user the triggering user or system
   */
  public DomainEvent {
    metadata = Map.copyOf(metadata);
    payload = Map.copyOf(payload);
  }

  /**
   * Creates a new {@link Builder} for {@link DomainEvent}.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link DomainEvent} to facilitate fluent construction.
   */
  public static class Builder {
    /** Entity Type. */
    private String entityType;
    /** Entity Id. */
    private String entityId;
    /** Timestamp. */
    private Instant timestamp = Instant.now();
    /** Domain Type. */
    private String type;
    /** Metadata. */
    private Map<String, Object> metadata = Map.of();
    /** Payload. */
    private Map<String, Object> payload = Map.of();
    /** User. */
    private String user;

    /**
     * Sets the entity type.
     *
     * @param eventEntityType the type of entity (e.g., "Document")
     * @return this Builder instance
     */
    public Builder entityType(final String eventEntityType) {
      this.entityType = eventEntityType;
      return this;
    }

    /**
     * Sets the entity ID.
     *
     * @param eventEntityId the aggregate or entity identifier
     * @return this Builder instance
     */
    public Builder entityId(final String eventEntityId) {
      this.entityId = eventEntityId;
      return this;
    }

    /**
     * Sets the eventTimestamp for the event.
     *
     * @param eventTimestamp the Instant of event creation
     * @return this Builder instance
     */
    public Builder timestamp(final Instant eventTimestamp) {
      this.timestamp = eventTimestamp;
      return this;
    }

    /**
     * Sets the domain event eventType.
     *
     * @param eventType the event eventType (e.g., "DocumentCreated")
     * @return this Builder instance
     */
    public Builder type(final String eventType) {
      this.type = eventType;
      return this;
    }

    /**
     * Sets additional eventMetadata for the event.
     *
     * @param eventMetadata a map of eventMetadata entries
     * @return this Builder instance
     */
    public Builder metadata(final Map<String, Object> eventMetadata) {
      this.metadata = eventMetadata;
      return this;
    }

    /**
     * Sets the event eventPayload.
     *
     * @param eventPayload a map containing event details
     * @return this Builder instance
     */
    public Builder payload(final Map<String, Object> eventPayload) {
      this.payload = eventPayload;
      return this;
    }

    /**
     * Sets the eventUser or system that triggered the event.
     *
     * @param eventUser the eventUser or system identifier
     * @return this Builder instance
     */
    public Builder user(final String eventUser) {
      this.user = eventUser;
      return this;
    }

    /**
     * Builds the {@link DomainEvent} instance.
     *
     * @return a new DomainEventRecord
     */
    public DomainEvent build() {
      return new DomainEvent(entityType, entityId, timestamp, type, metadata, payload, user);
    }
  }
}

