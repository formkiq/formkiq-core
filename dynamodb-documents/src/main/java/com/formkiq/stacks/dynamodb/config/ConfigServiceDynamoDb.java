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
package com.formkiq.stacks.dynamodb.config;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.builder.MapToAttributeValue;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/** Implementation of the {@link ConfigService}. */
public final class ConfigServiceDynamoDb implements ConfigService, DbKeys {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public ConfigServiceDynamoDb(final DynamoDbConnectionBuilder connection,
      final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.db = new DynamoDbServiceImpl(connection, documentsTable);
  }

  @Override
  public void delete(final String siteId) {
    String s = siteId != null ? siteId : DEFAULT_SITE_ID;
    Map<String, AttributeValue> keys = keysGeneric(null, PREFIX_CONFIG, s);
    this.db.deleteItem(keys.get(PK), keys.get(SK));
  }

  @Override
  public SiteConfiguration get(final String siteId) {

    List<Map<String, AttributeValue>> keys = new ArrayList<>();

    String site = !isDefaultSiteId(siteId) ? siteId : DEFAULT_SITE_ID;
    keys.add(keysGeneric(null, PREFIX_CONFIG, site));

    BatchGetConfig batchConfig = new BatchGetConfig();
    List<Map<String, AttributeValue>> list = this.db.getBatch(batchConfig, keys);

    Map<String, Object> map =
        !list.isEmpty() ? new AttributeValueToMap().apply(list.get(0)) : Map.of();
    return transform(map);
  }

  @Override
  public long getIncrement(final String siteId, final String key) {
    Map<String, AttributeValue> keys = getIncrementKey(siteId, key);
    Map<String, AttributeValue> values = this.db.get(keys.get(PK), keys.get(SK));
    return !values.isEmpty() ? Long.parseLong(values.get("Number").n()) : -1;
  }

  private Map<String, AttributeValue> getIncrementKey(final String siteId, final String key) {
    String pk = "configvalues";
    String sk = "key#" + key;
    return keysGeneric(siteId, PK, pk, SK, sk);
  }

  @Override
  public Map<String, Long> getIncrements(final String siteId) {
    final int limit = 100;
    Map<String, AttributeValue> keys = getIncrementKey(siteId, "");
    QueryConfig config = new QueryConfig();
    QueryResponse response = this.db.queryBeginsWith(config, keys.get(PK), null, null, limit);

    Map<String, Long> map = new HashMap<>();
    response.items().forEach(i -> {
      String key = i.get(SK).s();
      key = key.substring(key.indexOf("#") + 1);
      long number = Long.parseLong(i.get("Number").n());
      map.put(key, number);
    });

    return map;
  }

  private String getString(final Object o) {
    return o != null ? o.toString() : "";
  }

  @Override
  public long increment(final String siteId, final String key) {
    Map<String, AttributeValue> keys = getIncrementKey(siteId, key);
    return this.db.getNextNumber(keys);
  }

  private void put(final Map<String, Object> map, final String mapKey, final String value) {
    if (value != null) {
      map.put(mapKey, value);
    }
  }

  @Override
  public boolean save(final String siteId, final SiteConfiguration config) {

    Map<String, Object> map = new HashMap<>();
    put(map, CHATGPT_API_KEY, config.getChatGptApiKey());
    put(map, MAX_DOCUMENT_SIZE_BYTES, config.getMaxContentLengthBytes());
    put(map, MAX_DOCUMENTS, config.getMaxDocuments());
    put(map, MAX_WEBHOOKS, config.getMaxWebhooks());
    put(map, NOTIFICATION_EMAIL, config.getNotificationEmail());
    put(map, DOCUMENT_TIME_TO_LIVE, config.getDocumentTimeToLive());
    put(map, WEBHOOK_TIME_TO_LIVE, config.getWebhookTimeToLive());

    updateGoogle(config, map);

    updateOcr(config, map);

    updateDocusign(config, map);

    Map<String, AttributeValue> item =
        keysGeneric(null, PREFIX_CONFIG, siteId != null ? siteId : DEFAULT_SITE_ID);

    Map<String, AttributeValue> values = new MapToAttributeValue().apply(map);
    item.putAll(values);

    if (!map.isEmpty()) {
      if (this.db.exists(item.get(PK), item.get(SK))) {
        this.db.updateValues(item.get(PK), item.get(SK), values);
      } else {
        this.db.putItem(item);
      }
    }

    return !map.isEmpty();
  }

  private SiteConfiguration transform(final Map<String, Object> map) {

    SiteConfiguration config = new SiteConfiguration();
    config.setChatGptApiKey(getString(map.get(CHATGPT_API_KEY)));
    config.setGoogle(transformGoogle(map));
    config.setMaxWebhooks(getString(map.get(MAX_WEBHOOKS)));
    config.setMaxDocuments(getString(map.get(MAX_DOCUMENTS)));
    config.setDocumentTimeToLive(getString(map.get(DOCUMENT_TIME_TO_LIVE)));
    config.setDocusign(transformDocusign(map));
    config.setMaxContentLengthBytes(getString(map.get(MAX_DOCUMENT_SIZE_BYTES)));
    config.setNotificationEmail(getString(map.get(NOTIFICATION_EMAIL)));
    config.setOcr(transformOcr(map));
    config.setWebhookTimeToLive(getString(map.get(WEBHOOK_TIME_TO_LIVE)));
    return config;
  }

  private SiteConfigurationDocusign transformDocusign(final Map<String, Object> map) {
    SiteConfigurationDocusign docusign = new SiteConfigurationDocusign();
    docusign.setUserId(getString(map.get(KEY_DOCUSIGN_USER_ID)));
    docusign.setRsaPrivateKey(getString(map.get(KEY_DOCUSIGN_RSA_PRIVATE_KEY)));
    docusign.setHmacSignature(getString(map.get(KEY_DOCUSIGN_HMAC_SIGNATURE)));
    docusign.setIntegrationKey(getString(map.get(KEY_DOCUSIGN_INTEGRATION_KEY)));

    return docusign;
  }

  private SiteConfigurationGoogle transformGoogle(final Map<String, Object> map) {
    SiteConfigurationGoogle google = new SiteConfigurationGoogle();
    google.setWorkloadIdentityAudience(getString(map.get("googleWorkloadIdentityAudience")));
    google.setWorkloadIdentityServiceAccount(
        getString(map.get("googleWorkloadIdentityServiceAccount")));
    return google;
  }

  private SiteConfigurationOcr transformOcr(final Map<String, Object> map) {
    SiteConfigurationOcr ocr = new SiteConfigurationOcr();
    ocr.setMaxTransactions(
        map.containsKey("maxTransactions") ? ((Double) map.get("maxTransactions")).longValue()
            : -1);
    ocr.setMaxPagesPerTransaction(map.containsKey("maxPagesPerTransaction")
        ? ((Double) map.get("maxPagesPerTransaction")).longValue()
        : -1);
    return ocr;
  }

  private void updateDocusign(final SiteConfiguration config, final Map<String, Object> map) {

    SiteConfigurationDocusign docusign = config.getDocusign();
    if (docusign != null) {

      String docusignUserId = docusign.getUserId();
      if (!Strings.isEmpty(docusignUserId)) {
        map.put(KEY_DOCUSIGN_USER_ID, docusignUserId.trim());
      }

      String docusignIntegrationKey = docusign.getIntegrationKey();
      if (!Strings.isEmpty(docusignIntegrationKey)) {
        map.put(KEY_DOCUSIGN_INTEGRATION_KEY, docusignIntegrationKey.trim());
      }

      String docusignRsaPrivateKey = docusign.getRsaPrivateKey();
      if (!Strings.isEmpty(docusignRsaPrivateKey)) {
        map.put(KEY_DOCUSIGN_RSA_PRIVATE_KEY, docusignRsaPrivateKey.trim());
      }

      String docusignHmacSignature = docusign.getHmacSignature();
      if (!Strings.isEmpty(docusignHmacSignature)) {
        map.put(KEY_DOCUSIGN_HMAC_SIGNATURE, docusignHmacSignature.trim());
      }
    }
  }

  private void updateGoogle(final SiteConfiguration config, final Map<String, Object> map) {
    SiteConfigurationGoogle google = config.getGoogle();
    if (google != null) {

      String workloadIdentityAudience = google.getWorkloadIdentityAudience();
      String workloadIdentityServiceAccount = google.getWorkloadIdentityServiceAccount();

      if (!Strings.isEmpty(workloadIdentityAudience)) {
        map.put("googleWorkloadIdentityAudience", workloadIdentityAudience);
      }

      if (!Strings.isEmpty(workloadIdentityServiceAccount)) {
        map.put("googleWorkloadIdentityServiceAccount", workloadIdentityServiceAccount);
      }
    }
  }

  private void updateOcr(final SiteConfiguration config, final Map<String, Object> map) {
    SiteConfigurationOcr ocr = config.getOcr();

    if (ocr != null) {
      long maxTransactions = ocr.getMaxTransactions();
      long maxPagesPerTransaction = ocr.getMaxPagesPerTransaction();
      map.put("maxTransactions", maxTransactions != 0 ? maxTransactions : -1);
      map.put("maxPagesPerTransaction", maxPagesPerTransaction != 0 ? maxPagesPerTransaction : -1);
    }
  }
}
