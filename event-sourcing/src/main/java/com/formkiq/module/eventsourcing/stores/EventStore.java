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
import java.util.List;

/**
 * EventStore provides an append-only persistence mechanism for {@link DomainEvent}s.
 * <p>
 * Implementations should ensure events are stored in order and support optimistic concurrency
 * control via version checks.
 *
 * @param <E> the specific type of {@link DomainEvent} to store
 */
public interface EventStore<E extends DomainEvent> {

  /**
   * Append one or more events for a given aggregate, enforcing optimistic concurrency.
   * <p>
   * The {@code expectedVersion} parameter is used to guarantee that no other events have been
   * appended since the caller last read the aggregate's version..
   *
   * @param events the list of events to append
   */
  void appendEvents(List<E> events);

  /**
   * Load all events for a given aggregate in ascending version order.
   *
   * @param entityType the type of aggregate (e.g., "Document")
   * @param entityId the identifier of the aggregate
   * @return a List of events, ordered by version ascending, or empty if none found
   */
  List<E> loadEvents(String entityType, String entityId);

  /**
   * Load events for a given aggregate, starting after a specific version.
   *
   * @param entityType the type of aggregate
   * @param entityId the identifier of the aggregate
   * @param from the exclusive lower bound date
   * @return a List of events after the given version
   */
  List<E> loadEventsFrom(String entityType, String entityId, Instant from);
}
