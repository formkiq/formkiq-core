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
package com.formkiq.module.eventsourcing.commands;

import com.formkiq.module.eventsourcing.stores.DomainEvent;
import com.formkiq.validation.ValidationException;

import java.util.Collection;

/**
 * Command Handler interface that processes a domain-level {@link Command} and produces one or more
 * {@link DomainEvent}s. Implementations encapsulate business logic to validate commands, enforce
 * invariants, and emit events that represent state changes within the domain.
 *
 * @param <C> the type of {@link Command} consumed by this handler
 * @param <E> the type of {@link DomainEvent} produced by this handler
 */
public interface CommandHandler<C extends Command, E extends DomainEvent> {

  /**
   * Handles the given domain-level {@link Command}, applies business rules, and returns a resulting
   * domain event of type {@code T}. Implementations should enforce validation, normalization, and
   * optimistic concurrency checks.
   *
   * @param command the {@link Command} to process
   * @return a domain event instance representing the outcome of handling the command
   * @throws ValidationException if the command fails business validation
   */
  Collection<E> handle(C command) throws ValidationException;
}
