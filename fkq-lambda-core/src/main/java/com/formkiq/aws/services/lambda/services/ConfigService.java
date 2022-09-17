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
package com.formkiq.aws.services.lambda.services;

import com.formkiq.aws.dynamodb.DynamicObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Config Service. */
public interface ConfigService {

  /** Document Time To Live Key. */
  String DOCUMENT_TIME_TO_LIVE = "DocumentTimeToLive";
  /** Max Webhooks Key. */
  String MAX_WEBHOOKS = "MaxWebhooks";
  /** Max Documents Key. */
  String MAX_DOCUMENTS = "MaxDocuments";
  /** Max Document Size. */
  String MAX_DOCUMENT_SIZE_BYTES = "MaxContentLengthBytes";
  /** Webhook Time To Live Key. */
  String WEBHOOK_TIME_TO_LIVE = "WebhookTimeToLive";

  /**
   * Delete Config.
   * 
   * @param client {@link DynamoDbClient}
   * @param siteId {@link String}
   */
  void delete(DynamoDbClient client, String siteId);

  /**
   * Get Config.
   * 
   * @param client {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @return {@link DynamicObject}
   */
  DynamicObject get(DynamoDbClient client, String siteId);

  /**
   * Save Config.
   * 
   * @param client {@link DynamoDbClient}
   * @param siteId Optional Grouping siteId
   * @param obj {@link DynamicObject}
   */
  void save(DynamoDbClient client, String siteId, DynamicObject obj);
}
