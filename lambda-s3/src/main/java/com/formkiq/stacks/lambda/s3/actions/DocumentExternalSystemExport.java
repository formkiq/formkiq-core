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

import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordToMap;
import com.google.gson.Gson;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Export {@link DocumentItem} to External System.
 */
public class DocumentExternalSystemExport implements BiFunction<String, String, String> {

  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link Gson}. */
  private final Gson gson = GsonUtil.getInstance();
  /** Documents S3 Bucket. */
  private final String documentsBucket;
  /** {@link S3PresignerService}. */
  private final S3PresignerService s3Presigner;

  public DocumentExternalSystemExport(final AwsServiceCache serviceCache) {
    this.documentService = serviceCache.getExtension(DocumentService.class);
    this.documentsBucket = serviceCache.environment("DOCUMENTS_S3_BUCKET");
    this.s3Presigner = serviceCache.getExtension(S3PresignerService.class);
  }

  @Override
  public String apply(final String siteId, final String documentId) {
    return apply(siteId, documentId, null);
  }

  /**
   * Convert {@link DocumentItem} to JSON.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param activities {@link Collection}
   * @return {@link String}
   */
  public String apply(final String siteId, final String documentId,
      final Collection<Map<String, Object>> activities) {

    DocumentItem result = this.documentService.findDocument(siteId, documentId);
    if (result == null) {
      result = new DynamicDocumentItem(Map.of("documentId", documentId));
    }

    DynamicDocumentItem item = new DocumentItemToDynamicDocumentItem().apply(result);

    String site = siteId != null ? siteId : SiteIdKeyGenerator.DEFAULT_SITE_ID;
    item.put("siteId", site);

    URL s3Url = getS3Url(siteId, documentId, item);
    item.put("url", s3Url);

    Collection<Map<String, Object>> attributes = addDocumentAttributes(siteId, documentId);
    if (!attributes.isEmpty()) {
      item.put("attributes", attributes);
    }

    addChanged(activities, item);
    addChangedAttributes(activities, item);

    addDocumentTags(siteId, documentId, item);

    return this.gson.toJson(Map.of("document", item));
  }

  private void addChanged(final Collection<Map<String, Object>> activities,
      final DynamicDocumentItem item) {
    if (!notNull(activities).isEmpty()) {

      Collection<Map<String, Object>> documentActivities =
          activities.stream().filter(a -> "documents".equals(a.get("resource"))).toList();
      Map<String, Object> changed = new HashMap<>();
      documentActivities.forEach(a -> {
        Map<String, Object> valuesMap = (Map<String, Object>) a.get("changes");
        if (!notNull(valuesMap).isEmpty()) {
          valuesMap.forEach((k, v) -> {
            if (v instanceof Map mm && mm.containsKey("oldValue")) {
              Object oldValue = mm.get("oldValue");
              changed.put(k, oldValue);
            }
          });
        }
      });

      if (!changed.isEmpty()) {
        item.put("changed", changed);
      }
    }
  }

  private void addChangedAttributes(final Collection<Map<String, Object>> activities,
      final DynamicDocumentItem item) {

    if (!notNull(activities).isEmpty()) {

      Map<Object, List<Map<String, Object>>> attributesActivities =
          activities.stream().filter(a -> "documentAttributes".equals(a.get("resource")))
              .collect(Collectors.groupingBy(a -> a.get("attributeKey")));

      Map<String, Map<String, Object>> changedAttributes = new HashMap<>();
      attributesActivities.forEach((k, v) -> {
        Map<String, List<Object>> values = new HashMap<>();
        v.forEach(a -> {
          if (a.get("changes") instanceof Map m) {
            Map<String, Object> map = new HashMap<>(m);
            map.keySet().removeIf(key -> !key.contains("Value"));
            map.forEach((k1, v1) -> {
              if (v1 instanceof Map mm && mm.containsKey("oldValue")) {
                Object oldValue = mm.get("oldValue");
                values.computeIfAbsent(k1, s -> new ArrayList<>()).add(oldValue);
              }
            });
          }
        });

        Map<String, Object> m = new HashMap<>();
        values.forEach((k1, v1) -> {
          if (v1.size() == 1) {
            m.put(k1, v1.get(0));
          } else {
            m.put(k1 + "s", v1);
          }
        });

        if (!m.isEmpty()) {
          changedAttributes.put((String) k, m);
        }
      });

      if (!changedAttributes.isEmpty()) {
        item.put("changedAttributes", changedAttributes);
      }
    }
  }

  private Collection<Map<String, Object>> addDocumentAttributes(final String siteId,
      final String documentId) {

    final int limit = 100;

    PaginationResults<DocumentAttributeRecord> results =
        this.documentService.findDocumentAttributes(siteId, documentId, null, limit);

    Collection<Map<String, Object>> list =
        new DocumentAttributeRecordToMap(true).apply(results.getResults());

    list.forEach(l -> {
      l.remove("userId");
      l.remove("insertedDate");
    });

    return list;
  }

  private void addDocumentTags(final String siteId, final String documentId,
      final DynamicDocumentItem item) {

    Map<String, Collection<DocumentTag>> tagMap = this.documentService.findDocumentsTags(siteId,
        List.of(documentId), Arrays.asList("CLAMAV_SCAN_STATUS", "CLAMAV_SCAN_TIMESTAMP"));

    Map<String, String> values = new HashMap<>();
    Collection<DocumentTag> tags = tagMap.get(documentId);
    for (DocumentTag tag : tags) {
      values.put(tag.getKey(), tag.getValue());
    }

    String status = values.getOrDefault("CLAMAV_SCAN_STATUS", null);
    item.put("status", status);

    String timestamp = values.getOrDefault("CLAMAV_SCAN_TIMESTAMP", null);
    item.put("timestamp", timestamp);
  }

  /**
   * Get Document S3 Url.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link URL}
   */
  private URL getS3Url(final String siteId, final String documentId, final DocumentItem item) {

    URL url = null;

    if (item != null && !isEmpty(item.getPath())) {
      Duration duration = Duration.ofDays(1);
      PresignGetUrlConfig config =
          new PresignGetUrlConfig().contentDispositionByPath(item.getPath(), false);
      String s3key = createS3Key(siteId, documentId);
      url = s3Presigner.presignGetUrl(documentsBucket, s3key, duration, null, config);
    }

    return url;
  }
}
