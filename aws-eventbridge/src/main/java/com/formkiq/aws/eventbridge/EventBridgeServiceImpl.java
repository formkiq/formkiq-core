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
package com.formkiq.aws.eventbridge;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/**
 * Implementation of {@link EventBridgeService}.
 */
public class EventBridgeServiceImpl implements EventBridgeService {

  /** {@link EventBridgeClient}. */
  private final EventBridgeClient client;

  /**
   * constructor.
   * 
   * @param connection {@link EventBridgeConnectionBuilder}
   * 
   */
  public EventBridgeServiceImpl(final EventBridgeConnectionBuilder connection) {
    this.client = connection.build();
  }


  @Override
  public PutEventsResponse putEvents(final String eventBusName, final String detailType,
      final String detail, final String source) {
    PutEventsRequestEntry requestEntry = PutEventsRequestEntry.builder().eventBusName(eventBusName)
        .detailType(detailType).source(source).detail(detail).build();

    PutEventsRequest request = PutEventsRequest.builder().entries(requestEntry).build();

    return client.putEvents(request);
  }
}
