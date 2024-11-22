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
package com.formkiq.stacks.lambda.s3.actions;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.eventbridge.EventBridgeService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.validation.ValidationException;

import java.io.IOException;
import java.util.List;

/**
 * Event Bridge implementation of {@link DocumentAction}.
 */
public class EventBridgeAction implements DocumentAction {

  /** {@link EventBridgeService}. */
  private final EventBridgeService eventBridgeService;
  /** App Environment. */
  private final String appEnvironment;
  /** {@link DocumentExternalSystemExport}. */
  private final DocumentExternalSystemExport systemExport;
  /** Debug. */
  private final boolean debug;

  /**
   * constructor.
   *
   * @param serviceCache {@link AwsServiceCache}
   */
  public EventBridgeAction(final AwsServiceCache serviceCache) {
    this.eventBridgeService = serviceCache.getExtension(EventBridgeService.class);
    this.appEnvironment = serviceCache.environment("APP_ENVIRONMENT");
    this.systemExport = new DocumentExternalSystemExport(serviceCache);
    this.debug = serviceCache.debug();
  }

  @Override
  public void run(final LambdaLogger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException, ValidationException {

    String eventBusName = action.parameters().get("eventBusName");

    if (Strings.isEmpty(eventBusName)) {
      throw new IOException("'eventBusName' missing");
    }

    String detailType = "Document Action Event";
    String source = "formkiq." + this.appEnvironment;

    String detail = this.systemExport.apply(siteId, documentId, actions);

    if (this.debug) {
      String s = String.format(
          "{\"type\",\"%s\",\"eventBusName\":\"%s\","
              + "\"detailType\":\"%s\",\"source\":\"%s\",\"detail\":\"%s\"}",
          "eventBridge", eventBusName, detailType, source, detail);
      logger.log(s);
    }

    eventBridgeService.putEvents(eventBusName, detailType, detail, source);
  }
}
