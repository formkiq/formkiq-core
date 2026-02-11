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

import software.amazon.awssdk.services.eventbridge.model.CreateEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/**
 * Disabled {@link EventBridgeService} returns NoOp.
 */
public class EventBridgeServiceDisabled implements EventBridgeService {
  @Override
  public CreateEventBusResponse createEventBridge(final String eventBusName) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public void createRule(final String eventBusName, final String ruleName, final String ruleArn,
      final String eventPattern, final String targetId, final String targetArn) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public DeleteEventBusResponse deleteEventBus(final String eventBusName) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public void deleteRule(final String eventBusName, final String ruleName, final String targetId) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public PutEventsResponse putEvents(final String eventBusName,
      final EventBridgeMessage eventBridgeMessage) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }

  @Override
  public PutEventsResponse putEvents(final String eventBusName, final String detailType,
      final String detail, final String source) {
    throw new UnsupportedOperationException("Operational Mode 'Disabled'");
  }
}
