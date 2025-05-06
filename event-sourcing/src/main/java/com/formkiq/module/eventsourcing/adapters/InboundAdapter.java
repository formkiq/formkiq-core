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
package com.formkiq.module.eventsourcing.adapters;

import com.formkiq.module.eventsourcing.commands.Command;

import java.util.function.Function;

/**
 * Inbound Adapter interface that transforms an inbound transport-specific request into a domain
 * {@link Command}. This decouples the presentation layer (e.g., HTTP, messaging) from the core
 * application logic by mapping external inputs into commands.
 *
 * @param <T> the type of inbound request
 * @param <E> the type of {@link Command} object
 */
public interface InboundAdapter<T, E extends Command> extends Function<T, E> {

  /**
   * Converts the given inbound request into a domain-level {@link Command}. Implementations should
   * perform any necessary validation or normalization.
   *
   * @param request the inbound request payload to be transformed
   * @return a {@link Command} instance representing the user's intent
   */
  @Override
  E apply(T request);
}
