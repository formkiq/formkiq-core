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
 * Event Bridge Service.
 *
 */
public interface EventBridgeService {

  /**
   * Create AWS Event Bridge.
   *
   * @param eventBusName The name or ARN of the event bus to receive the event. Only the rules that
   *        are associated with this event bus are used to match the event.
   * @return CreateEventBusResponse
   */
  CreateEventBusResponse createEventBridge(String eventBusName);

  /**
   * Delete Event Bridge.
   * 
   * @param eventBusName {@link String}
   * @return DeleteEventBusResponse
   */
  DeleteEventBusResponse deleteEventBus(String eventBusName);

  /**
   * Put AWS Event Bridge Event.
   *
   * @param eventBusName The name or ARN of the event bus to receive the event. Only the rules that
   *        are associated with this event bus are used to match the event. If you omit this, the
   *        default event bus is used.
   * @param detailType Free-form string, with a maximum of 128 characters, used to decide what
   *        fields to expect in the event detail.
   * @param detail A valid JSON object
   * @param source The source of the event
   * @return PutEventsResponse
   */
  PutEventsResponse putEvents(String eventBusName, String detailType, String detail, String source);

  /**
   * Put AWS Event Bridge Event.
   *
   * @param eventBusName The name or ARN of the event bus to receive the event. Only the rules that
   *        are associated with this event bus are used to match the event. If you omit this, the
   *        default event bus is used.
   * @param eventBridgeMessage {@link EventBridgeMessage}
   * @return PutEventsResponse
   */
  PutEventsResponse putEvents(String eventBusName, EventBridgeMessage eventBridgeMessage);

  /**
   * Create Event Bridge Rule.
   * 
   * @param eventBusName {@link String}
   * @param ruleName {@link String}
   * @param ruleArn {@link String}
   * @param eventPattern {@link String}
   * @param targetId {@link String}
   * @param targetArn {@link String}
   */
  void createRule(String eventBusName, String ruleName, String ruleArn, String eventPattern,
      String targetId, String targetArn);

  /**
   * Delete Event Bridge Rule.
   * 
   * @param eventBusName {@link String}
   * @param ruleName {@link String}
   * @param targetId {@link String}
   */
  void deleteRule(String eventBusName, String ruleName, String targetId);
}
