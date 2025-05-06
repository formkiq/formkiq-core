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
package com.formkiq.module.eventsourcing.transformer;

import com.formkiq.module.eventsourcing.stores.DomainEvent;
import com.formkiq.module.eventsourcing.commands.Command;
import com.formkiq.module.eventsourcing.commands.HttpCommand;

import java.util.function.Function;

/**
 * Specialized transformer that converts an {@link HttpCommand} into a {@link DomainEvent} by
 * delegating to the generic {@link CommandToEventTransformer}.
 */
public class HttpCommandToEventTransformer implements Function<HttpCommand, DomainEvent> {

  /**
   * Delegate transformer for general Command-to-Event conversion.
   */
  private final Function<Command, DomainEvent> delegate;

  /**
   * Creates a new HttpCommandToEventTransformer using the default delegate.
   */
  public HttpCommandToEventTransformer() {
    this.delegate = new CommandToEventTransformer();
  }

  /**
   * Transforms the given HttpCommand into a DomainEventRecord.
   *
   * @param httpCommand the HTTP-specific Command to transform
   * @return the resulting DomainEventRecord
   */
  @Override
  public DomainEvent apply(final HttpCommand httpCommand) {
    return delegate.apply(httpCommand);
  }
}
