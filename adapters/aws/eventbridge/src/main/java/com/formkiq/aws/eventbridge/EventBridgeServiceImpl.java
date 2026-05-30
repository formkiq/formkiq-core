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

import com.formkiq.module.lambdaservices.logger.Logger;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.PutTargetsRequest;
import software.amazon.awssdk.services.eventbridge.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.eventbridge.model.Target;

/**
 * Implementation of {@link EventBridgeService}.
 */
public class EventBridgeServiceImpl implements EventBridgeService {

  /** {@link EventBridgeClient}. */
  private final EventBridgeClient client;
  /** {@link Logger}. */
  private final Logger log;

  /**
   * constructor.
   * 
   * @param connection {@link EventBridgeConnectionBuilder}
   * @param logger {@link Logger}
   * 
   */
  public EventBridgeServiceImpl(final Logger logger,
      final EventBridgeConnectionBuilder connection) {
    this.client = connection.build();
    this.log = logger;
  }

  @Override
  public CreateEventBusResponse createEventBridge(final String eventBusName) {
    CreateEventBusRequest req = CreateEventBusRequest.builder().name(eventBusName).build();
    return client.createEventBus(req);
  }

  @Override
  public void createRule(final String eventBusName, final String ruleName, final String ruleArn,
      final String eventPattern, final String targetId, final String targetArn) {
    PutRuleRequest ruleReq = PutRuleRequest.builder().name(ruleName).eventBusName(eventBusName)
        .eventPattern(eventPattern).build();
    client.putRule(ruleReq);

    Target target = Target.builder().id(targetId).roleArn(ruleArn).arn(targetArn).build();
    PutTargetsRequest putTarget = PutTargetsRequest.builder().rule(ruleName)
        .eventBusName(eventBusName).targets(target).build();
    client.putTargets(putTarget);
  }

  public DeleteEventBusResponse deleteEventBus(final String eventBusName) {
    DeleteEventBusRequest req = DeleteEventBusRequest.builder().name(eventBusName).build();
    return client.deleteEventBus(req);
  }

  @Override
  public void deleteRule(final String eventBusName, final String ruleName, final String targetId) {

    RemoveTargetsRequest target = RemoveTargetsRequest.builder().rule(ruleName).ids(targetId)
        .eventBusName(eventBusName).build();
    client.removeTargets(target);

    DeleteRuleRequest deleteReq =
        DeleteRuleRequest.builder().name(ruleName).eventBusName(eventBusName).build();
    client.deleteRule(deleteReq);
  }

  @Override
  public PutEventsResponse putEvents(final String eventBusName, final EventBridgeMessage msg) {
    return putEvents(eventBusName, msg.getDetailType(), msg.getDetail(), msg.getSource());
  }

  @Override
  public PutEventsResponse putEvents(final String eventBusName, final String detailType,
      final String detail, final String source) {

    if (eventBusName == null) {
      throw new IllegalStateException("'eventBusName' cannot be null");
    }

    String s = String.format(
        "{\"type\",\"%s\",\"eventBusName\":\"%s\","
            + "\"detailType\":\"%s\",\"source\":\"%s\",\"detail\":\"%s\"}",
        "eventBridge", eventBusName, detailType, source, detail);
    this.log.debug(s);

    PutEventsRequestEntry requestEntry = PutEventsRequestEntry.builder().eventBusName(eventBusName)
        .detailType(detailType).source(source).detail(detail).build();

    PutEventsRequest request = PutEventsRequest.builder().entries(requestEntry).build();
    return client.putEvents(request);
  }
}
