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
package com.formkiq.module.events;

import java.util.ArrayList;
import java.util.List;
import com.formkiq.module.events.document.DocumentEvent;
import com.formkiq.module.events.folder.FolderEvent;

/**
 * Mock implementation of {@link EventService}.
 */
public class EventServiceMock implements EventService {

  /** {@link List} {@link DocumentEvent}. */
  private List<DocumentEvent> documentEvents = new ArrayList<DocumentEvent>();

  /** {@link List} {@link FolderEvent}. */
  private List<FolderEvent> folderEvents = new ArrayList<FolderEvent>();

  /**
   * Clear Events.
   */
  public void clear() {
    this.folderEvents.clear();
    this.documentEvents.clear();
  }

  /**
   * Get {@link List} {@link DocumentEvent}.
   * 
   * @return {@link List} {@link DocumentEvent}
   */
  public List<DocumentEvent> getDocumentEvents() {
    return this.documentEvents;
  }

  /**
   * Get {@link List} {@link FolderEvent}.
   * 
   * @return {@link List} {@link FolderEvent}
   */
  public List<FolderEvent> getFolderEvents() {
    return this.folderEvents;
  }

  @Override
  public String publish(final DocumentEvent event) {
    this.documentEvents.add(event);
    return null;
  }

  @Override
  public String publish(final FolderEvent event) {
    this.folderEvents.add(event);
    return null;
  }

}
