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
package com.formkiq.stacks.dynamodb;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import com.formkiq.stacks.common.objects.DynamicObject;

/** Services for Querying, Updating Webhooks. */
public interface WebhooksService {
  
  /**
   * Add Tags to Webhook.
   * 
   * @param siteId Optional Grouping siteId
   * @param webhookId {@link String}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param ttl {@link Date}
   */
  void addTags(String siteId, String webhookId, Collection<DocumentTag> tags, Date ttl);
  
  /**
   * Delete Webhook.
   *
   * @param siteId Optional Grouping siteId
   * @param id {@link String}
   */
  void deleteWebhook(String siteId, String id);
  
  /**
   * Find Webhook Tag.
   *
   * @param siteId Optional Grouping siteId
   * @param webhookId {@link String}
   * @param tagKey {@link String}
   * 
   * @return {@link DynamicObject}
   */
  DynamicObject findTag(String siteId, String webhookId, String tagKey);
  
  /**
   * Find Webhook Tags.
   *
   * @param siteId Optional Grouping siteId
   * @param webhookId {@link String}
   * 
   * @return {@link DynamicObject} {@link PaginationResults}
   */
  PaginationResults<DynamicObject> findTags(String siteId, String webhookId);

  /**
   * Find Webhook.
   * @param siteId {@link String}
   * @param webhookId {@link String}
   * @return {@link DynamicObject}
   */
  DynamicObject findWebhook(String siteId, String webhookId);

  /**
   * Find Webhooks.
   *
   * @param siteId Optional Grouping siteId
   * 
   * @return {@link List} {@link DynamicObject}
   */
  List<DynamicObject> findWebhooks(String siteId);

  /**
   * Save Document and Tags.
   *
   * @param siteId Optional Grouping siteId
   * @param name {@link String}
   * @param userId {@link String}
   * @param ttl {@link Date}
   * @param enabled {@link String}
   * 
   * @return {@link String}
   */
  String saveWebhook(String siteId, String name, String userId, Date ttl, String enabled);
  
  /**
   * Update Webhook TimeToLive.
   * @param siteId {@link String}
   * @param webhookId {@link String}
   * @param ttl {@link Date}
   */
  void updateTimeToLive(String siteId, String webhookId, Date ttl);

  /**
   * Update Webhook.
   * @param siteId {@link String}
   * @param webhookId {@link String}
   * @param obj {@link DynamicObject}
   */
  void updateWebhook(String siteId, String webhookId, DynamicObject obj);  
}
