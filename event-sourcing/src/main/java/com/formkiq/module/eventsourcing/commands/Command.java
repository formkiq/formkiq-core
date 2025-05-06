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

import java.time.Instant;

/**
 * Marker for an intent to change state in the system, enriched with common metadata needed by
 * handlers.
 */
public interface Command {

  /**
   * The type of event.
   * 
   * @return String
   */
  String getEventType();

  /**
   * The type of entity or aggregate this command applies to.
   * 
   * @return String
   */
  String getEntityType();

  /**
   * The identifier of that entity.
   * 
   * @return String
   */
  String getEntityId();

  /**
   * Who or what initiated the command (user-id, system-id, etc).
   * 
   * @return String
   */
  String getUser();

  /**
   * When the command was created.
   * 
   * @return Instant
   */
  Instant getTimestamp();
}
